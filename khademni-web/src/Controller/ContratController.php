<?php

namespace App\Controller;

use App\Entity\Contrat;
use App\Entity\Job;
use App\Entity\Payment;
use App\Entity\User;
use App\Form\ContratType;
use App\Repository\JobRepository;
use App\Repository\UserRepository;
use Doctrine\ORM\EntityManagerInterface;
use Stripe\Stripe;
use Stripe\Checkout\Session;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

#[Route('/contrat')]
class ContratController extends AbstractController
{
    #[Route('/', name: 'app_contrat_index', methods: ['GET'])]
    public function index(Request $request, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        $search = $request->query->get('search');

        $qb = $em->createQueryBuilder()
            ->select('c')
            ->from(Contrat::class, 'c');

        // Ownership filter: regular users see only their own contracts
        // Admins see everything
        if ($user instanceof User && !$this->isGranted('ROLE_ADMIN')) {
            $qb->andWhere('c.clientId = :userId OR c.freelancerId = :userId')
                ->setParameter('userId', $user->getId());
        }

        // Only show unpaid contracts — paid ones appear in /payment/
        $qb->andWhere('c.statut = :statut')
           ->setParameter('statut', 'EN_ATTENTE');

        if ($search) {
            $qb->andWhere('c.titre LIKE :search OR c.description LIKE :search')
                ->setParameter('search', '%' . $search . '%');
        }

        $contrats = $qb->getQuery()->getResult();

        // Stats calculation
        $totalPendingAmount = 0;
        foreach ($contrats as $c) {
            $totalPendingAmount += $c->getPrix();
        }

        return $this->render('contrat/index.html.twig', [
            'contrats' => $contrats,
            'search' => $search,
            'totalPendingAmount' => $totalPendingAmount,
            'activeCount' => count($contrats)
        ]);
    }

    /**
     * Creates a contract automatically from a Job that has an accepted application.
     * Flow: Job owner accepted a freelancer → comes here → contract is auto-created.
     */
    #[Route('/create-from-job/{id}', name: 'app_contrat_create_from_job', methods: ['GET'])]
    public function createFromJob(Job $job, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        // Only the job owner (or admin) can create a contract from this job
        if (!$this->isGranted('ROLE_ADMIN')) {
            if ($job->getUser() === null || $job->getUser()->getId() !== $user->getId()) {
                throw $this->createAccessDeniedException('Vous devez être le propriétaire de cette offre.');
            }
        }

        // Find the accepted application for this job
        $acceptedApp = $job->getAcceptedApplication();
        if (!$acceptedApp) {
            $this->addFlash('error', 'Aucune candidature acceptée pour cette offre. Acceptez d\'abord un freelancer.');
            return $this->redirectToRoute('app_contrat_index');
        }

        $freelancer = $acceptedApp->getUser();
        if (!$freelancer instanceof User) {
            $this->addFlash('error', 'Le freelancer de la candidature acceptée est introuvable.');
            return $this->redirectToRoute('app_contrat_index');
        }

        // Prevent duplicate contracts for the same job (same client + freelancer + title)
        $existing = $em->getRepository(Contrat::class)->findOneBy([
            'clientId' => $job->getUser()->getId(),
            'freelancerId' => $freelancer->getId(),
            'titre' => $job->getTitle(),
        ]);

        if ($existing) {
            $this->addFlash('info', 'Un contrat existe déjà pour cette offre.');
            return $this->redirectToRoute('app_contrat_index');
        }

        // Parse salary from job's salaryRange (e.g. "1000-2000", "$1500")
        $prix = null;
        if ($job->getSalaryRange()) {
            if (preg_match('/[\d]+(?:\.[\d]+)?/', str_replace(',', '', $job->getSalaryRange()), $matches)) {
                $prix = (float) $matches[0];
            }
        }

        // Auto-create the contract from job data
        $contrat = new Contrat();
        $contrat->setClientId($job->getUser()->getId());
        $contrat->setFreelancerId($freelancer->getId());
        $contrat->setTitre($job->getTitle());
        $contrat->setDescription($job->getDescription());
        $contrat->setPrix($prix);
        $contrat->setDateContrat(new \DateTime());
        $contrat->setStatut('EN_ATTENTE');

        $em->persist($contrat);
        $em->flush();

        $this->addFlash('success', 'Contrat créé avec succès à partir de l\'offre "' . $job->getTitle() . '".');
        return $this->redirectToRoute('app_contrat_index');
    }

    #[Route('/new', name: 'app_contrat_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em, UserRepository $userRepository, JobRepository $jobRepository): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $isAdmin = $this->isGranted('ROLE_ADMIN');
        $selectedJobId = $request->query->get('jobId');
        
        // Build job choices for regular users:
        // Shows ALL platform jobs as suggestions
        $jobChoices = [];
        $allJobs = $jobRepository->findAll();
        
        foreach ($allJobs as $job) {
            $descSnippet = $job->getDescription() ? (mb_substr($job->getDescription(), 0, 50) . '...') : 'Pas de description';
            $label = $job->getTitle() . ' — ' . $descSnippet;
            $jobChoices[$label] = $job->getId();
        }

        $contrat = new Contrat();
        $contrat->setDateContrat(new \DateTime());
        if (!$isAdmin) {
            $contrat->setClientId($user->getId());
        }

        // PRE-FILL logic: if jobId is passed in URL, auto-suggest title/description
        if ($selectedJobId) {
            $preJob = $jobRepository->find($selectedJobId);
            if ($preJob && ($isAdmin || ($preJob->getUser() && $preJob->getUser()->getId() === $user->getId()))) {
                $contrat->setTitre($preJob->getTitle());
                $contrat->setDescription($preJob->getDescription());
                if ($preJob->getSalaryRange()) {
                    if (preg_match('/[\d]+(?:\.[\d]+)?/', str_replace(',', '', $preJob->getSalaryRange()), $matches)) {
                        $contrat->setPrix((float) $matches[0]);
                    }
                }
            }
        }

        $form = $this->createForm(ContratType::class, $contrat, [
            'is_admin' => $isAdmin,
            'job_choices' => $jobChoices,
        ]);
        $form->handleRequest($request);

        // POPULATE FROM JOB BEFORE VALIDATION
        if ($form->isSubmitted() && !$isAdmin) {
            $formJobId = $form->get('titre')->getData();
            $actualJobId = $formJobId ?: $selectedJobId;
            $job = $actualJobId ? $jobRepository->find($actualJobId) : null;

            if ($job) {
                $acceptedApp = $job->getAcceptedApplication();
                $contrat->setTitre($job->getTitle());
                if ($acceptedApp) {
                    $contrat->setFreelancerId($acceptedApp->getUser()->getId());
                }
                // Auto-fill description ONLY if it's currently empty
                if (!$contrat->getDescription()) {
                    $contrat->setDescription($job->getDescription());
                }
                if ($job->getSalaryRange() && !$contrat->getPrix()) {
                    if (preg_match('/[\d]+(?:\.[\d]+)?/', str_replace(',', '', $job->getSalaryRange()), $matches)) {
                        $contrat->setPrix((float) $matches[0]);
                    }
                }
            }
        }

        if ($form->isSubmitted() && $form->isValid()) {
            if ($isAdmin) {
                // Admin mode: manual validation
                $client = $userRepository->find($contrat->getClientId());
                $freelancer = $userRepository->find($contrat->getFreelancerId());

                if (!$client) {
                    $this->addFlash('error', 'Le Client (ID: ' . $contrat->getClientId() . ') n\'existe pas.');
                } elseif (!$freelancer) {
                    $this->addFlash('error', 'Le Freelancer (ID: ' . $contrat->getFreelancerId() . ') n\'existe pas.');
                } elseif ($client->getId() === $freelancer->getId()) {
                    $this->addFlash('error', 'Le client et le freelancer ne peuvent pas être la même personne.');
                } else {
                    $em->persist($contrat);
                    $em->flush();
                    $this->addFlash('success', 'Contrat créé avec succès.');
                    return $this->redirectToRoute('app_contrat_index', [], Response::HTTP_SEE_OTHER);
                }
            } else {
                // Regular user mode
                $jobTitle = $form->get('titre')->getData();
                $job = $em->getRepository(\App\Entity\Job::class)->findOneBy(['title' => $jobTitle]);
                if ($job) {
                    $description = $contrat->getDescription();
                    $contrat->setDescription($description . " (JobID: " . $job->getId() . ")");
                }

                // FALLBACK: If no freelancer was auto-suggested or selected, 
                // use the current user's ID to satisfy the NOT NULL database check.
                if (!$contrat->getFreelancerId()) {
                    $contrat->setFreelancerId($user->getId());
                }
                
                $contrat->setStatut('EN_ATTENTE');
                $em->persist($contrat);
                $em->flush();
                $this->addFlash('success', 'Contrat créé et ajouté à votre liste.');
                return $this->redirectToRoute('app_contrat_index', [], Response::HTTP_SEE_OTHER);
            }
        }

        return $this->render('contrat/new.html.twig', [
            'contrat' => $contrat,
            'form' => $form->createView(),
        ]);
    }


    #[Route('/{id}/edit', name: 'app_contrat_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Contrat $contrat, EntityManagerInterface $em, UserRepository $userRepository, JobRepository $jobRepository): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        // Loosened ownership check: any logged-in user can edit
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        // Lock paid contracts: once paid, a contract cannot be modified
        if ($contrat->getStatut() === 'PAYE') {
            $this->addFlash('error', 'Ce contrat a déjà été payé et ne peut plus être modifié.');
            return $this->redirectToRoute('app_contrat_index');
        }

        $isAdmin = $this->isGranted('ROLE_ADMIN');

        // Build job choices for edit (same as new)
        $jobChoices = [];
        $selectedJobId = null;
        $allJobs = $jobRepository->findAll();
        foreach ($allJobs as $job) {
            $descSnippet = $job->getDescription() ? (mb_substr($job->getDescription(), 0, 50) . '...') : 'Pas de description';
            $label = $job->getTitle() . ' — ' . $descSnippet;
            $jobChoices[$label] = $job->getId();
            
            // If the current contract title matches this job's title, mark it as selected
            if ($job->getTitle() === $contrat->getTitre()) {
                $selectedJobId = $job->getId();
            }
        }

        $form = $this->createForm(ContratType::class, $contrat, [
            'is_admin' => $isAdmin,
            'job_choices' => $jobChoices,
        ]);

        // Pre-select the job in the non-mapped 'titre' ChoiceType field
        if (!$isAdmin && $selectedJobId) {
            $form->get('titre')->setData($selectedJobId);
        }

        $form->handleRequest($request);

        // POPULATE FROM JOB BEFORE VALIDATION
        if ($form->isSubmitted() && !$isAdmin) {
            $formJobId = $form->get('titre')->getData();
            if ($formJobId) {
                $job = $jobRepository->find($formJobId);
                if ($job) {
                    $acceptedApp = $job->getAcceptedApplication();
                    $contrat->setTitre($job->getTitle());
                    if ($acceptedApp) {
                        $contrat->setFreelancerId($acceptedApp->getUser()->getId());
                    }
                    // Do NOT overwrite description on edit
                }
            }
        }

        if ($form->isSubmitted() && $form->isValid()) {
            if ($isAdmin) {
                // Admin: validate user IDs
                $client = $userRepository->find($contrat->getClientId());
                $freelancer = $userRepository->find($contrat->getFreelancerId());

                if (!$client) {
                    $this->addFlash('error', 'Le Client (ID: ' . $contrat->getClientId() . ') n\'existe pas.');
                } elseif (!$freelancer) {
                    $this->addFlash('error', 'Le Freelancer (ID: ' . $contrat->getFreelancerId() . ') n\'existe pas.');
                } else {
                    $em->flush();
                    return $this->redirectToRoute('app_contrat_index', [], Response::HTTP_SEE_OTHER);
                }
            } else {
                // Regular user
                if (!$contrat->getFreelancerId()) {
                    $contrat->setFreelancerId($user->getId());
                }
                $em->flush();
                $this->addFlash('success', 'Contrat mis à jour.');
                return $this->redirectToRoute('app_contrat_index', [], Response::HTTP_SEE_OTHER);
            }
        }

        return $this->render('contrat/edit.html.twig', [
            'contrat' => $contrat,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}', name: 'app_contrat_delete', methods: ['POST'])]
    public function delete(Request $request, Contrat $contrat, EntityManagerInterface $em): Response
    {
        // Loosened ownership check: any logged-in user can delete
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        // Lock paid contracts: once paid, a contract cannot be deleted
        if ($contrat->getStatut() === 'PAYE') {
            $this->addFlash('error', 'Ce contrat a déjà été payé et ne peut pas être supprimé.');
            return $this->redirectToRoute('app_contrat_index');
        }

        if ($this->isCsrfTokenValid('delete' . $contrat->getId(), $request->request->get('_token'))) {
            $em->remove($contrat);
            $em->flush();
        }

        return $this->redirectToRoute('app_contrat_index', [], Response::HTTP_SEE_OTHER);
    }

    #[Route('/{id}/export', name: 'app_contrat_export_pdf', methods: ['GET'])]
    public function exportPdf(Contrat $contrat): Response
    {
        // Ownership check
        $this->denyAccessUnlessOwner($contrat);
        $pdfOptions = new Options();
        $pdfOptions->set('defaultFont', 'Arial');
        $pdfOptions->setIsRemoteEnabled(true);

        $dompdf = new Dompdf($pdfOptions);

        $html = $this->renderView('contrat/pdf.html.twig', [
            'contrat' => $contrat
        ]);

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return new Response(
            $dompdf->output(),
            Response::HTTP_OK,
            ['Content-Type' => 'application/pdf', 'Content-Disposition' => 'inline; filename="contrat_' . $contrat->getId() . '.pdf"']
        );
    }

    #[Route('/{id}/pay', name: 'app_contrat_pay', methods: ['GET'])]
    public function pay(Contrat $contrat, EntityManagerInterface $em): Response
    {
        // Ownership check: only the client (who pays) can trigger this
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        // Prevent double payment
        if ($contrat->getStatut() === 'PAYE') {
            $this->addFlash('info', 'Ce contrat a déjà été payé.');
            return $this->redirectToRoute('app_contrat_index');
        }

        Stripe::setApiKey($this->getParameter('stripe_secret_key'));

        $totalPrice = $contrat->getPrix();
        $payAmount = $totalPrice;
        $description = $contrat->getDescription();
        $isMilestone = false;

        // PARSE MILESTONES: Supports (Milestones: 50/50), (Milestones: 30 / 30 / 40)
        if (preg_match('/\(Milestones:\s*([\d\/%\s]+)\)/i', $description, $matches)) {
            $ratios = explode('/', str_replace(['%', ' '], '', $matches[1]));
            
            // Get already paid installments
            $existingPaymentsCount = $em->getRepository(Payment::class)->count(['contrat' => $contrat, 'status' => 'PAID']);
            
            if (isset($ratios[$existingPaymentsCount])) {
                $percentage = (float) $ratios[$existingPaymentsCount];
                $payAmount = ($totalPrice * $percentage) / 100;
                $isMilestone = true;
            } else {
                // All milestones already paid? 
                $this->addFlash('info', 'Toutes les étapes de ce contrat ont déjà été payées.');
                return $this->redirectToRoute('app_contrat_index');
            }
        }

        $session = Session::create([
            'payment_method_types' => ['card'],
            'line_items' => [
                [
                    'price_data' => [
                        'currency' => 'usd',
                        'product_data' => [
                            'name' => 'Paiement Contrat: ' . $contrat->getTitre() . ($isMilestone ? ' (Étape ' . ($existingPaymentsCount + 1) . ')' : ''),
                            'description' => 'Facture #' . $contrat->getId(),
                        ],
                        'unit_amount' => (int) ($payAmount * 100),
                    ],
                    'quantity' => 1,
                ]
            ],
            'mode' => 'payment',
            'success_url' => $this->generateUrl('app_payment_success', ['id' => $contrat->getId()], UrlGeneratorInterface::ABSOLUTE_URL) . '?session_id={CHECKOUT_SESSION_ID}',
            'cancel_url' => $this->generateUrl('app_payment_cancel', ['id' => $contrat->getId()], UrlGeneratorInterface::ABSOLUTE_URL),
            'metadata' => [
                'contrat_id' => $contrat->getId(),
                'pay_amount' => $payAmount, // Save the actual amount being paid
            ],
        ]);

        return $this->redirect($session->url, 303);
    }

    #[Route('/payment/{id}/success', name: 'app_payment_success', methods: ['GET'])]
    public function paymentSuccess(Request $request, Contrat $contrat, EntityManagerInterface $em, UserRepository $userRepository): Response
    {
        $sessionId = $request->query->get('session_id');

        Stripe::setApiKey($this->getParameter('stripe_secret_key'));
        if ($sessionId) {
            $stripeSession = Session::retrieve($sessionId);
            if ($stripeSession && $stripeSession->payment_status === 'paid') {
                
                $actualPaidAmount = (float) ($stripeSession->metadata->pay_amount ?? $contrat->getPrix());

                $payment = new Payment();
                $payment->setContrat($contrat);
                $payment->setAmount($actualPaidAmount);
                $payment->setStripeSessionId($sessionId);
                $payment->setStatus('PAID');
                $em->persist($payment);

                // Credit the freelancer's wallet balance
                $freelancer = $userRepository->find($contrat->getFreelancerId());
                if ($freelancer instanceof User) {
                    $freelancer->setBalance($freelancer->getBalance() + $actualPaidAmount);
                }

                $em->flush();

                // Check if FULLY PAID
                $allPayments = $em->getRepository(Payment::class)->findBy(['contrat' => $contrat, 'status' => 'PAID']);
                $sumPaid = 0;
                foreach ($allPayments as $p) { $sumPaid += $p->getAmount(); }

                if ($sumPaid >= ($contrat->getPrix() - 0.01)) {
                    $contrat->setStatut('PAYE');
                    $em->flush();
                }
            }
        }

        return $this->render('contrat/payment_success.html.twig', [
            'contrat' => $contrat
        ]);
    }

    #[Route('/payment/{id}/cancel', name: 'app_payment_cancel', methods: ['GET'])]
    public function paymentCancel(Contrat $contrat): Response
    {
        return $this->render('contrat/payment_cancel.html.twig', [
            'contrat' => $contrat
        ]);
    }

    /**
     * Verifies the current user is the client or freelancer of the contract.
     * Admins bypass this check. Throws 403 if unauthorized.
     * This is a PHP-only security guard — NO database changes.
     */
    private function denyAccessUnlessOwner(Contrat $contrat): void
    {
        $user = $this->getUser();

        if (!$user instanceof User) {
            throw $this->createAccessDeniedException('Vous devez être connecté.');
        }

        // Admins can access everything
        if ($this->isGranted('ROLE_ADMIN')) {
            return;
        }

        // Regular users can only access their own contracts
        if ($contrat->getClientId() !== $user->getId() && $contrat->getFreelancerId() !== $user->getId()) {
            throw $this->createAccessDeniedException('Vous n\'avez pas accès à ce contrat.');
        }
    }
}
