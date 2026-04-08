<?php

namespace App\Controller\Admin;

use App\Entity\Payment;
use App\Form\PaymentType;
use App\Repository\PaymentRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * Back-office: Full CRUD for Payments (Admin only).
 * NO database schema changes — uses existing tables and columns only.
 */
#[Route('/admin/payment')]
#[IsGranted('ROLE_ADMIN')]
class AdminPaymentController extends AbstractController
{
    #[Route('/', name: 'admin_payment_index', methods: ['GET'])]
    public function index(Request $request, PaymentRepository $paymentRepository): Response
    {
        $search = $request->query->get('search');

        $payments = $search
            ? $paymentRepository->searchPayments($search)
            : $paymentRepository->findAll();

        return $this->render('admin/payment/index.html.twig', [
            'payments' => $payments,
            'search' => $search,
        ]);
    }

    #[Route('/new', name: 'admin_payment_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $payment = new Payment();
        $form = $this->createForm(PaymentType::class, $payment);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // Auto-update contract status to PAYE when a payment is recorded
            $contrat = $payment->getContrat();
            if ($contrat) {
                $contrat->setStatut('PAYE');
            }

            $entityManager->persist($payment);
            $entityManager->flush();

            $this->addFlash('success', 'Paiement enregistré avec succès.');
            return $this->redirectToRoute('admin_payment_index', [], Response::HTTP_SEE_OTHER);
        }

        return $this->render('admin/payment/new.html.twig', [
            'payment' => $payment,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/edit', name: 'admin_payment_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Payment $payment, EntityManagerInterface $entityManager): Response
    {
        $form = $this->createForm(PaymentType::class, $payment);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();

            $this->addFlash('success', 'Paiement mis à jour avec succès.');
            return $this->redirectToRoute('admin_payment_index', [], Response::HTTP_SEE_OTHER);
        }

        return $this->render('admin/payment/edit.html.twig', [
            'payment' => $payment,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}', name: 'admin_payment_delete', methods: ['POST'])]
    public function delete(Request $request, Payment $payment, EntityManagerInterface $entityManager): Response
    {
        if ($this->isCsrfTokenValid('delete'.$payment->getId(), $request->request->get('_token'))) {
            $entityManager->remove($payment);
            $entityManager->flush();
            $this->addFlash('success', 'Paiement supprimé.');
        }

        return $this->redirectToRoute('admin_payment_index', [], Response::HTTP_SEE_OTHER);
    }
}
