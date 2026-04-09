<?php

namespace App\Controller;

use App\Repository\ContratRepository;
use App\Service\ContractPaymentService;
use Stripe\Webhook;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class WebhookController extends AbstractController
{
    #[Route('/webhook/stripe', name: 'app_stripe_webhook', methods: ['POST'])]
    public function stripeWebhook(
        Request $request,
        ContratRepository $contratRepository,
        ContractPaymentService $contractPaymentService,
    ): Response {
        $endpointSecret = trim((string) ($_ENV['STRIPE_WEBHOOK_SECRET'] ?? $_SERVER['STRIPE_WEBHOOK_SECRET'] ?? ''));
        if ('' === $endpointSecret) {
            return new Response('Stripe webhook is not configured.', Response::HTTP_SERVICE_UNAVAILABLE);
        }

        $signature = $request->headers->get('Stripe-Signature');
        if (null === $signature) {
            return new Response('Missing Stripe signature.', Response::HTTP_BAD_REQUEST);
        }

        try {
            $event = Webhook::constructEvent($request->getContent(), $signature, $endpointSecret);
        } catch (\UnexpectedValueException) {
            return new Response('Invalid payload.', Response::HTTP_BAD_REQUEST);
        } catch (\Stripe\Exception\SignatureVerificationException) {
            return new Response('Invalid Stripe signature.', Response::HTTP_BAD_REQUEST);
        }

        if ('checkout.session.completed' !== $event->type) {
            return new Response('Event ignored.', Response::HTTP_OK);
        }

        $session = $event->data->object;
        $contratId = (int) ($session->metadata->contrat_id ?? 0);
        if ($contratId <= 0) {
            return new Response('Missing contract metadata.', Response::HTTP_BAD_REQUEST);
        }

        $contrat = $contratRepository->find($contratId);
        if (null === $contrat) {
            return new Response('Contract not found.', Response::HTTP_NOT_FOUND);
        }

        if ('paid' !== (string) ($session->payment_status ?? '')) {
            return new Response('Payment not completed.', Response::HTTP_OK);
        }

        try {
            $contractPaymentService->recordStripePayment(
                $contrat,
                (string) ($session->id ?? ''),
                (float) ($session->metadata->pay_amount ?? $contrat->getPrix())
            );
        } catch (\RuntimeException $e) {
            return new Response($e->getMessage(), Response::HTTP_BAD_REQUEST);
        }

        return new Response('Webhook handled.', Response::HTTP_OK);
    }
}
