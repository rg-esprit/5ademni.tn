<?php

namespace App\Controller;

use App\Entity\Contrat;
use App\Entity\Payment;
use App\Entity\User;
use App\Form\ContratType;
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

    #[Route('/new', name: 'app_contrat_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em, UserRepository $userRepository): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $contrat = new Contrat();
        // Auto-set: the logged-in user is the client (owner of the contract)
        $contrat->setClientId($user->getId());
        $contrat->setDateContrat(new \DateTime());
        $form = $this->createForm(ContratType::class, $contrat);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // MANUAL VALIDATION for DB Integrity (JavaFX compatibility)
            $client = $userRepository->find($contrat->getClientId());
            $freelancer = $userRepository->find($contrat->getFreelancerId());

            if (!$client) {
                $this->addFlash('error', 'Le Client (ID: ' . $contrat->getClientId() . ') n\'existe pas.');
            } elseif (!$freelancer) {
                $this->addFlash('error', 'Le Freelancer (ID: ' . $contrat->getFreelancerId() . ') n\'existe pas.');
            } else {
                $em->persist($contrat);
                $em->flush();
                return $this->redirectToRoute('app_contrat_index', [], Response::HTTP_SEE_OTHER);
            }
        }

        return $this->render('contrat/new.html.twig', [
            'contrat' => $contrat,
            'form' => $form->createView(),
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

        $form = $this->createForm(ContratType::class, $contrat);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // MANUAL VALIDATION for DB Integrity (JavaFX compatibility)
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
    public function pay(Contrat $contrat, EntityManagerInterface $em): Response
    {
        // Ownership check: only the client or an admin can pay
        $this->denyAccessUnlessOwner($contrat);
        Stripe::setApiKey($_ENV['STRIPE_SECRET_KEY'] ?? '');

        $session = Session::create([
            'payment_method_types' => ['card'],
            'line_items' => [
                [
                    'price_data' => [
                        'currency' => 'usd',
                        'product_data' => [
                            'name' => $contrat->getTitre() ?: 'Contrat #' . $contrat->getId(),
                        ],
                        'unit_amount' => (int) ($contrat->getPrix() * 100),
                    ],
                    'quantity' => 1,
                ]
            ],
            'mode' => 'payment',
            'success_url' => $this->generateUrl('app_payment_success', ['id' => $contrat->getId()], UrlGeneratorInterface::ABSOLUTE_URL) . '?session_id={CHECKOUT_SESSION_ID}',
            'cancel_url' => $this->generateUrl('app_payment_cancel', ['id' => $contrat->getId()], UrlGeneratorInterface::ABSOLUTE_URL),
        ]);

        return $this->redirect($session->url, 303);
    }

    #[Route('/payment/{id}/success', name: 'app_payment_success', methods: ['GET'])]
    public function paymentSuccess(Request $request, Contrat $contrat, EntityManagerInterface $em): Response
    {
        $sessionId = $request->query->get('session_id');

        Stripe::setApiKey($_ENV['STRIPE_SECRET_KEY'] ?? '');
        if ($sessionId) {
            $stripeSession = Session::retrieve($sessionId);
            if ($stripeSession && $stripeSession->payment_status === 'paid') {
                $contrat->setStatut('PAYE');

                $payment = new Payment();
                $payment->setContrat($contrat);
                $payment->setAmount($contrat->getPrix());
                $payment->setStripeSessionId($sessionId);
                $payment->setStatus('PAID');

                $em->persist($payment);
                $em->flush();
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
