<?php

namespace App\Controller;

use App\Entity\Contrat;
use App\Entity\Job;
use App\Entity\User;
use App\Form\ContratType;
use App\Repository\JobRepository;
use App\Repository\UserRepository;
use App\Service\ContractPaymentService;
use Doctrine\ORM\EntityManagerInterface;
use Dompdf\Dompdf;
use Dompdf\Options;
use Stripe\Checkout\Session;
use Stripe\Stripe;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\FormError;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

#[Route('/contrat')]
class ContratController extends AbstractController
{
    #[Route('/', name: 'app_contrat_index', methods: ['GET'])]
    public function index(Request $request, EntityManagerInterface $em, UserRepository $userRepository): Response
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

        $participantNames = [];
        $userIds = [];

        foreach ($contrats as $contrat) {
            if (null !== $contrat->getClientId()) {
                $userIds[] = $contrat->getClientId();
            }

            if (null !== $contrat->getFreelancerId()) {
                $userIds[] = $contrat->getFreelancerId();
            }
        }

        if ([] !== $userIds) {
            foreach ($userRepository->findBy(['id' => array_values(array_unique($userIds))]) as $participant) {
                $participantNames[$participant->getId()] = $participant->getDisplayName();
            }
        }

        // Stats calculation
        $totalPendingAmount = 0;
        foreach ($contrats as $c) {
            $totalPendingAmount += $c->getPrix();
        }

        return $this->render('contrat/index.html.twig', [
            'contrats' => $contrats,
            'participantNames' => $participantNames,
            'search' => $search,
            'totalPendingAmount' => $totalPendingAmount,
            'activeCount' => count($contrats)
        ]);
    }

    #[Route('/create-from-job/{id<\d+>}', name: 'app_contrat_create_from_job', methods: ['GET'])]
    public function createFromJob(Job $job): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if (!$this->isGranted('ROLE_ADMIN') && $job->getUser()?->getId() !== $user->getId()) {
            throw $this->createAccessDeniedException('Vous devez etre le proprietaire de cette offre.');
        }

        if (null === $job->getAcceptedApplication()?->getUser()) {
            $this->addFlash('error', 'Cette offre n\'a pas encore de freelancer accepte.');

            return $this->redirectToRoute('app_contrat_new');
        }

        return $this->redirectToRoute('app_contrat_new', ['jobId' => $job->getId()]);
    }

    #[Route('/new', name: 'app_contrat_new', methods: ['GET', 'POST'])]
    public function new(
        Request $request,
        EntityManagerInterface $em,
        UserRepository $userRepository,
        JobRepository $jobRepository
    ): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $isAdmin = $this->isGranted('ROLE_ADMIN');
        $availableJobs = $isAdmin ? [] : $this->getAccessibleContractJobs($user, $jobRepository);
        $selectedJobId = trim((string) $request->query->get('jobId', ''));
        $selectedJob = null;

        if ('' !== $selectedJobId) {
            if ($isAdmin) {
                $selectedJob = $jobRepository->find((int) $selectedJobId);
                if (!$selectedJob instanceof Job || null === $selectedJob->getAcceptedApplication()?->getUser()) {
                    $this->addFlash('error', 'Impossible de precharger ce contrat depuis l\'offre selectionnee.');

                    return $this->redirectToRoute('app_contrat_new');
                }
            } else {
                $selectedJob = $this->findAccessibleJobById($selectedJobId, $availableJobs);
                if (!$selectedJob instanceof Job) {
                    $this->addFlash('error', 'Cette offre n\'est pas disponible pour la creation d\'un contrat.');

                    return $this->redirectToRoute('app_contrat_new');
                }
            }
        }

        $contrat = new Contrat();
        $contrat->setDateContrat(new \DateTime());

        if (!$isAdmin) {
            $contrat->setClientId($user->getId());
        }

        if ($selectedJob instanceof Job) {
            $this->syncContractFromJob($contrat, $selectedJob, false);
        }

        $form = $this->createForm(ContratType::class, $contrat, [
            'is_admin' => $isAdmin,
            'job_choices' => $this->buildJobChoices($availableJobs),
            'use_job_selector' => !$isAdmin,
        ]);

        if (!$isAdmin && $selectedJob instanceof Job) {
            $form->get('titre')->setData((string) $selectedJob->getId());
        }

        $form->handleRequest($request);

        if ($form->isSubmitted() && !$isAdmin) {
            $chosenJob = $this->findAccessibleJobById((string) $form->get('titre')->getData(), $availableJobs);

            if (!$chosenJob instanceof Job) {
                $form->get('titre')->addError(new FormError('Veuillez selectionner une offre avec un freelancer accepte.'));
            } else {
                $this->syncContractFromJob($contrat, $chosenJob, true);
                $contrat->setClientId($user->getId());
                $contrat->setStatut('EN_ATTENTE');
            }
        }

        if ($form->isSubmitted() && $form->isValid()) {
            $client = $userRepository->find($contrat->getClientId());
            $freelancer = $userRepository->find($contrat->getFreelancerId());

            if (!$client) {
                $this->addFlash('error', 'Le Client (ID: ' . $contrat->getClientId() . ') n\'existe pas.');
            } elseif (!$freelancer) {
                $this->addFlash('error', 'Le Freelancer (ID: ' . $contrat->getFreelancerId() . ') n\'existe pas.');
            } else {
                $em->persist($contrat);
                $em->flush();

                $this->addFlash('success', 'Contrat cree avec succes.');

                return $this->redirectToRoute('app_contrat_index', [], Response::HTTP_SEE_OTHER);
            }
        }

        return $this->render('contrat/new.html.twig', [
            'contrat' => $contrat,
            'form' => $form->createView(),
            'is_admin' => $isAdmin,
            'available_job_count' => count($availableJobs),
        ]);
    }

    #[Route('/{id}/edit', name: 'app_contrat_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Contrat $contrat, EntityManagerInterface $em, UserRepository $userRepository): Response
    {
        // Ownership check: only the client, freelancer, or an admin can edit
        $this->denyAccessUnlessOwner($contrat);

        // Lock paid contracts: once paid, a contract cannot be modified
        if ($contrat->getStatut() === 'PAYE') {
            $this->addFlash('error', 'Ce contrat a déjà été payé et ne peut plus être modifié.');
            return $this->redirectToRoute('app_contrat_index');
        }

        $isAdmin = $this->isGranted('ROLE_ADMIN');
        $originalClientId = $contrat->getClientId();
        $originalFreelancerId = $contrat->getFreelancerId();

        $form = $this->createForm(ContratType::class, $contrat, [
            'is_admin' => $isAdmin,
            'use_job_selector' => false,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && !$isAdmin) {
            $contrat->setClientId($originalClientId);
            $contrat->setFreelancerId($originalFreelancerId);
        }

        if ($form->isSubmitted() && $form->isValid()) {
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
        }

        return $this->render('contrat/edit.html.twig', [
            'contrat' => $contrat,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}', name: 'app_contrat_delete', methods: ['POST'])]
    public function delete(Request $request, Contrat $contrat, EntityManagerInterface $em): Response
    {
        // Ownership check: only the client or an admin can delete
        $this->denyAccessUnlessOwner($contrat);

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
    public function pay(Contrat $contrat, ContractPaymentService $contractPaymentService): Response
    {

        $this->denyAccessUnlessClientCanPay($contrat);

        if ($contrat->getStatut() === 'PAYE') {
            $this->addFlash('info', 'Ce contrat a deja ete paye.');

            return $this->redirectToRoute('app_contrat_index');
        }


        // Ownership check: only the client or an admin can pay
        $this->denyAccessUnlessOwner($contrat);

        $stripeSecretKey = $this->getStripeSecretKey();
        if ('' === $stripeSecretKey) {
            $this->addFlash('error', 'Stripe is not configured yet. Online payment is currently unavailable.');

            return $this->redirectToRoute('app_contrat_index');
        }


        
        try {
            $checkout = $contractPaymentService->getCheckoutDetails($contrat);
        } catch (\RuntimeException $e) {
            $this->addFlash('error', $e->getMessage());

            return $this->redirectToRoute('app_contrat_index');
        }


        Stripe::setApiKey($stripeSecretKey);

        try {
            $session = Session::create([
                'payment_method_types' => ['card'],
                'line_items' => [
                    [
                        'price_data' => [
                            'currency' => 'usd',
                            'product_data' => [
                                'name' => $checkout['name'],
                            ],
                            'unit_amount' => (int) round($checkout['amount'] * 100),
                        ],
                        'quantity' => 1,
                    ],
                ],
                'mode' => 'payment',
                'success_url' => $this->generateUrl('app_payment_success', ['id' => $contrat->getId()], UrlGeneratorInterface::ABSOLUTE_URL) . '?session_id={CHECKOUT_SESSION_ID}',
                'cancel_url' => $this->generateUrl('app_payment_cancel', ['id' => $contrat->getId()], UrlGeneratorInterface::ABSOLUTE_URL),

                'metadata' => [
                    'contrat_id' => (string) $contrat->getId(),
                    'pay_amount' => number_format($checkout['amount'], 2, '.', ''),
                ],

            ]);
        } catch (\Throwable) {
            $this->addFlash('error', 'The payment gateway is unavailable right now. Please try again later.');

            return $this->redirectToRoute('app_contrat_index');
        }

        return $this->redirect($session->url, 303);
    }

    #[Route('/payment/{id}/success', name: 'app_payment_success', methods: ['GET'])]
    public function paymentSuccess(Request $request, Contrat $contrat, ContractPaymentService $contractPaymentService): Response
    {
        $this->denyAccessUnlessOwner($contrat);

        $sessionId = $request->query->get('session_id');
        $payment = null;

        $stripeSecretKey = $this->getStripeSecretKey();
        if ('' !== $stripeSecretKey && $sessionId) {
            Stripe::setApiKey($stripeSecretKey);

            try {
                $stripeSession = Session::retrieve($sessionId);
                if ($stripeSession && $stripeSession->payment_status === 'paid') {

                
                    $metadataContratId = (int) ($stripeSession->metadata->contrat_id ?? 0);

                    if ($metadataContratId !== $contrat->getId()) {
                        $this->addFlash('error', 'This payment session does not match the requested contract.');
                    } else {
                        $payment = $contractPaymentService->recordStripePayment(
                            $contrat,
                            (string) $sessionId,
                            (float) ($stripeSession->metadata->pay_amount ?? $contrat->getPrix())
                        );
                    }
                }
            } catch (\RuntimeException $e) {
                $this->addFlash('error', $e->getMessage());
            } catch (\Throwable) {
                $this->addFlash('error', 'The payment could not be verified automatically.');
            }
        }

        return $this->render('contrat/payment_success.html.twig', [
            'contrat' => $contrat,
            'payment' => $payment,
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


    
    private function denyAccessUnlessClientCanPay(Contrat $contrat): void
    {
        $user = $this->getUser();

        if (!$user instanceof User) {
            throw $this->createAccessDeniedException('Vous devez etre connecte.');
        }

        if ($this->isGranted('ROLE_ADMIN')) {
            return;
        }

        if ($contrat->getClientId() !== $user->getId()) {
            throw $this->createAccessDeniedException('Seul le client peut lancer ce paiement.');
        }
    }

    private function getStripeSecretKey(): string
    {
        return trim((string) $this->getParameter('stripe_secret_key'));
    }

    /**
     * @return Job[]
     */
    private function getAccessibleContractJobs(User $user, JobRepository $jobRepository): array
    {
        $jobs = $jobRepository->findBy(['user' => $user], ['postedDate' => 'DESC']);

        return array_values(array_filter($jobs, static function (Job $job): bool {
            return null !== $job->getAcceptedApplication()?->getUser();
        }));
    }

    /**
     * @param Job[] $jobs
     *
     * @return array<string, string>
     */
    private function buildJobChoices(array $jobs): array
    {
        $choices = [];

        foreach ($jobs as $job) {
            $freelancerName = $job->getAcceptedApplication()?->getUser()?->getDisplayName();
            $label = $job->getTitle() . ' - ' . $job->getCompany();

            if (null !== $freelancerName && '' !== $freelancerName) {
                $label .= ' - Freelancer: ' . $freelancerName;
            }

            $choices[$label] = (string) $job->getId();
        }

        return $choices;
    }

    /**
     * @param Job[] $jobs
     */
    private function findAccessibleJobById(string $jobId, array $jobs): ?Job
    {
        foreach ($jobs as $job) {
            if ((string) $job->getId() === $jobId) {
                return $job;
            }
        }

        return null;
    }

    private function syncContractFromJob(Contrat $contrat, Job $job, bool $preserveUserInput): void
    {
        $jobOwner = $job->getUser();
        $acceptedApplication = $job->getAcceptedApplication();
        $freelancer = $acceptedApplication?->getUser();

        if (!$jobOwner instanceof User || !$freelancer instanceof User) {
            throw new \RuntimeException('Cette offre ne peut pas encore etre transformee en contrat.');
        }

        $contrat->setClientId($jobOwner->getId());
        $contrat->setFreelancerId($freelancer->getId());
        $contrat->setTitre($job->getTitle());

        if (!$preserveUserInput || '' === trim((string) $contrat->getDescription())) {
            $contrat->setDescription($job->getDescription());
        }

        if (!$preserveUserInput || null === $contrat->getPrix()) {
            $price = $this->extractJobPrice($job);
            if (null !== $price) {
                $contrat->setPrix($price);
            }
        }
    }

    private function extractJobPrice(Job $job): ?float
    {
        $salaryRange = $job->getSalaryRange();
        if (null === $salaryRange || '' === trim($salaryRange)) {
            return null;
        }

        if (preg_match('/[\d]+(?:\.[\d]+)?/', str_replace(',', '', $salaryRange), $matches)) {
            return (float) $matches[0];
        }

        return null;
    }
}
