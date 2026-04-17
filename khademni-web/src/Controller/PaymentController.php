<?php

namespace App\Controller;

use App\Entity\Payment;
use App\Entity\User;
use App\Repository\PaymentRepository;
use Dompdf\Dompdf;
use Dompdf\Options;
use Knp\Component\Pager\PaginatorInterface;
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
    public function index(Request $request, PaymentRepository $paymentRepository, PaginatorInterface $paginator): Response
    {
        $user = $this->getUser();

        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $search = $request->query->get('search');

        // Build query (QueryBuilder, not array) for paginator
        if ($this->isGranted('ROLE_ADMIN')) {
            $qb = $paymentRepository->findAllQueryBuilder($search);
        } else {
            $qb = $paymentRepository->findByUserQueryBuilder($user, $search);
        }

        // Stats: lightweight aggregate query (accurate across all pages)
        $statsQb = clone $qb;
        $statsQb->select('COUNT(p.id) AS total_count, SUM(p.amount) AS total_amount');
        $stats = $statsQb->getQuery()->getSingleResult();
        $totalPaidAmount = round((float) ($stats['total_amount'] ?? 0), 2);
        $paymentCount = (int) ($stats['total_count'] ?? 0);

        // Paginate at 8 per page
        $page = max(1, (int) $request->query->get('page', 1));
        $pagination = $paginator->paginate($qb, $page, 8);

        return $this->render('payment/index.html.twig', [
            'payments' => $pagination,
            'search' => $search,
            'totalPaidAmount' => $totalPaidAmount,
            'paymentCount' => $paymentCount,
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
