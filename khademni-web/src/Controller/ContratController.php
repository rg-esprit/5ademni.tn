<?php

namespace App\Controller;

use App\Entity\Contrat;
use App\Entity\Payment;
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
        $search = $request->query->get('search');
        
        $qb = $em->createQueryBuilder()
            ->select('c')
            ->from(Contrat::class, 'c');
            
        if ($search) {
            $qb->where('c.titre LIKE :search OR c.description LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }
        
        $contrats = $qb->getQuery()->getResult();

        return $this->render('contrat/index.html.twig', [
            'contrats' => $contrats,
            'search' => $search
        ]);
    }

    #[Route('/new', name: 'app_contrat_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em, UserRepository $userRepository): Response
    {
        $contrat = new Contrat();
        // default dates
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
        if ($this->isCsrfTokenValid('delete'.$contrat->getId(), $request->request->get('_token'))) {
            $em->remove($contrat);
            $em->flush();
        }

        return $this->redirectToRoute('app_contrat_index', [], Response::HTTP_SEE_OTHER);
    }
    
    #[Route('/{id}/export', name: 'app_contrat_export_pdf', methods: ['GET'])]
    public function exportPdf(Contrat $contrat): Response
    {
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
            ['Content-Type' => 'application/pdf', 'Content-Disposition' => 'inline; filename="contrat_'.$contrat->getId().'.pdf"']
        );
    }

    #[Route('/{id}/pay', name: 'app_contrat_pay', methods: ['GET'])]
    public function pay(Contrat $contrat, EntityManagerInterface $em): Response
    {
        Stripe::setApiKey($_ENV['STRIPE_SECRET_KEY'] ?? ''); 

        $session = Session::create([
            'payment_method_types' => ['card'],
            'line_items' => [[
                'price_data' => [
                    'currency' => 'usd',
                    'product_data' => [
                        'name' => $contrat->getTitre() ?: 'Contrat #'.$contrat->getId(),
                    ],
                    'unit_amount' => (int)($contrat->getPrix() * 100),
                ],
                'quantity' => 1,
            ]],
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
}
