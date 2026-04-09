<?php

namespace App\Controller;

use App\Entity\Contrat;
use App\Entity\Payment;
use App\Repository\ContratRepository;
use App\Repository\PaymentRepository;
use App\Repository\UserRepository;
use Doctrine\ORM\EntityManagerInterface;
use Stripe\Stripe;
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
        EntityManagerInterface $em, 
        ContratRepository $contratRepository, 
        UserRepository $userRepository,
        PaymentRepository $paymentRepository
    ): Response {
        $stripeSecretKey = $this->getParameter('stripe_secret_key');
        $endpointSecret = $_ENV['STRIPE_WEBHOOK_SECRET'] ?? ''; // Set this in .env for production

        Stripe::setApiKey($stripeSecretKey);

        $payload = $request->getContent();
        $sigHeader = $request->headers->get('Stripe-Signature');
        $event = null;

        try {
            if ($endpointSecret) {
                // Production mode: Verify signature
                $event = Webhook::constructEvent($payload, $sigHeader, $endpointSecret);
            } else {
                // Dev mode: Proceed with payload directly (unsafe for prod)
                $event = json_decode($payload, false);
            }
        } catch (\UnexpectedValueException $e) {
            return new Response('Invalid payload', Response::HTTP_BAD_REQUEST);
        } catch (\Stripe\Exception\SignatureVerificationException $e) {
            return new Response('Invalid signature', Response::HTTP_BAD_REQUEST);
        }

        // Handle the event
        if ($event->type === 'checkout.session.completed') {
            $session = $event->data->object;
            $metadata = $session->metadata;
            $contratId = $metadata->contrat_id ?? null;

            if ($contratId) {
                $contrat = $contratRepository->find($contratId);
                if ($contrat && $contrat->getStatut() !== 'PAYE') {
                    // 1. Update Contract Status
                    $contrat->setStatut('PAYE');

                    // 2. Create/Update Payment record
                    $payment = $paymentRepository->findOneBy(['stripeSessionId' => $session->id]);
                    if (!$payment) {
                        $payment = new Payment();
                        $payment->setContrat($contrat);
                        $payment->setAmount($contrat->getPrix());
                        $payment->setStripeSessionId($session->id);
                        $em->persist($payment);
                    }
                    $payment->setStatus('PAID');

                    // 3. Credit Freelancer Balance
                    $freelancerId = $contrat->getFreelancerId();
                    if ($freelancerId) {
                        $freelancer = $userRepository->find($freelancerId);
                        if ($freelancer) {
                            $freelancer->setBalance($freelancer->getBalance() + $contrat->getPrix());
                        }
                    }

                    $em->flush();
                }
            }
        }

        return new Response('Webhook handled', Response::HTTP_OK);
    }
}
