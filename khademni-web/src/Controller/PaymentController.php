<?php

namespace App\Controller;

use App\Entity\Payment;
use App\Entity\User;
use App\Repository\PaymentRepository;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/payment')]
class PaymentController extends AbstractController
{
    /**
     * Front-office: READ-ONLY view of the current user's payments.
     * No create, edit, or delete — those are in the Admin area.
     */
    #[Route('/', name: 'app_payment_index', methods: ['GET'])]
    public function index(Request $request, PaymentRepository $paymentRepository): Response
    {
        $user = $this->getUser();

        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $search = $request->query->get('search');

        // Ownership filter: regular users see only payments linked to their contracts
        // Admins see everything
        if ($this->isGranted('ROLE_ADMIN')) {
            $payments = $search
                ? $paymentRepository->searchPayments($search)
                : $paymentRepository->findAll();
        } else {
            $payments = $paymentRepository->findByUser($user, $search);
        }

        // Stats calculation
        $totalPaidAmount = 0;
        foreach ($payments as $p) {
            $totalPaidAmount += $p->getAmount();
        }

        return $this->render('payment/index.html.twig', [
            'payments' => $payments,
            'search' => $search,
            'totalPaidAmount' => $totalPaidAmount,
            'paymentCount' => count($payments)
        ]);
    }

    #[Route('/{id}/export', name: 'app_payment_export_pdf', methods: ['GET'])]
    public function exportPdf(Payment $payment): Response
    {
        $this->denyAccessUnlessOwner($payment);

        $pdfOptions = new Options();
        $pdfOptions->set('defaultFont', 'Arial');
        $pdfOptions->setIsRemoteEnabled(true);

        $dompdf = new Dompdf($pdfOptions);
        $dompdf->loadHtml($this->renderView('payment/pdf.html.twig', [
            'payment' => $payment,
        ]));
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return new Response(
            $dompdf->output(),
            Response::HTTP_OK,
            [
                'Content-Type' => 'application/pdf',
                'Content-Disposition' => 'inline; filename="paiement_' . $payment->getId() . '.pdf"',
            ]
        );
    }

    private function denyAccessUnlessOwner(Payment $payment): void
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            throw $this->createAccessDeniedException('Login required.');
        }

        if ($this->isGranted('ROLE_ADMIN')) {
            return;
        }

        $contrat = $payment->getContrat();
        if (!$contrat || ($contrat->getClientId() !== $user->getId() && $contrat->getFreelancerId() !== $user->getId())) {
            throw $this->createAccessDeniedException('Access denied to this payment record.');
        }
    }
}
