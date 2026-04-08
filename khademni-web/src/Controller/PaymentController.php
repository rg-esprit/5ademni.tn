<?php

namespace App\Controller;

use App\Entity\User;
use App\Repository\PaymentRepository;
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
}
