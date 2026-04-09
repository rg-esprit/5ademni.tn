<?php

namespace App\Service;

use App\Entity\Contrat;
use App\Entity\Payment;
use App\Entity\User;
use App\Repository\PaymentRepository;
use App\Repository\UserRepository;
use Doctrine\ORM\EntityManagerInterface;

class ContractPaymentService
{
    public function __construct(
        private readonly EntityManagerInterface $entityManager,
        private readonly PaymentRepository $paymentRepository,
        private readonly UserRepository $userRepository,
    ) {
    }

    /**
     * @return array{name: string, amount: float}
     */
    public function getCheckoutDetails(Contrat $contrat): array
    {
        $price = $contrat->getPrix();
        if (null === $price || $price <= 0) {
            throw new \RuntimeException('Le contrat doit avoir un prix valide avant le paiement.');
        }

        $ratios = $this->parseMilestoneRatios($contrat->getDescription());
        if (null === $ratios) {
            return [
                'name' => $contrat->getTitre() ?: 'Contrat #' . $contrat->getId(),
                'amount' => $price,
            ];
        }

        $paidPayments = $this->paymentRepository->findBy([
            'contrat' => $contrat,
            'status' => 'PAID',
        ], [
            'createdAt' => 'ASC',
            'id' => 'ASC',
        ]);

        $nextMilestoneIndex = count($paidPayments);
        if ($nextMilestoneIndex >= count($ratios)) {
            throw new \RuntimeException('Toutes les etapes de ce contrat ont deja ete payees.');
        }

        $amount = round(($price * $ratios[$nextMilestoneIndex]) / 100, 2);
        if ($amount <= 0) {
            throw new \RuntimeException('Le montant de la prochaine etape est invalide.');
        }

        return [
            'name' => sprintf(
                '%s - Etape %d/%d',
                $contrat->getTitre() ?: 'Contrat #' . $contrat->getId(),
                $nextMilestoneIndex + 1,
                count($ratios)
            ),
            'amount' => $amount,
        ];
    }

    public function recordStripePayment(Contrat $contrat, string $sessionId, float $amount): Payment
    {
        if ('' === trim($sessionId)) {
            throw new \RuntimeException('Stripe session id is required.');
        }

        if ($amount <= 0) {
            throw new \RuntimeException('The paid amount is invalid.');
        }

        $existingPayment = $this->paymentRepository->findOneBy(['stripeSessionId' => $sessionId]);
        if ($existingPayment instanceof Payment) {
            return $existingPayment;
        }

        $payment = new Payment();
        $payment->setContrat($contrat);
        $payment->setAmount($amount);
        $payment->setStripeSessionId($sessionId);
        $payment->setStatus('PAID');

        $this->entityManager->persist($payment);

        $freelancer = $this->userRepository->find($contrat->getFreelancerId());
        if ($freelancer instanceof User) {
            $freelancer->setBalance($freelancer->getBalance() + $amount);
        }

        $totalPaid = $amount;
        foreach ($this->paymentRepository->findBy(['contrat' => $contrat, 'status' => 'PAID']) as $existingPaidPayment) {
            $totalPaid += (float) $existingPaidPayment->getAmount();
        }

        $contractPrice = $contrat->getPrix();
        if (null !== $contractPrice && $totalPaid >= ($contractPrice - 0.01)) {
            $contrat->setStatut('PAYE');
        } else {
            $contrat->setStatut('EN_ATTENTE');
        }

        $this->entityManager->flush();

        return $payment;
    }

    /**
     * @return float[]|null
     */
    private function parseMilestoneRatios(?string $description): ?array
    {
        if (null === $description || '' === trim($description)) {
            return null;
        }

        if (!preg_match('/\(Milestones:\s*([^)]+)\)/i', $description, $matches)) {
            return null;
        }

        $parts = array_values(array_filter(array_map(
            static fn (string $part): string => trim(str_replace('%', '', $part)),
            explode('/', $matches[1])
        ), static fn (string $part): bool => '' !== $part));

        if ([] === $parts) {
            throw new \RuntimeException('Le format des milestones est invalide.');
        }

        $ratios = [];
        $total = 0.0;

        foreach ($parts as $part) {
            if (!is_numeric($part)) {
                throw new \RuntimeException('Le format des milestones est invalide.');
            }

            $ratio = (float) $part;
            if ($ratio <= 0) {
                throw new \RuntimeException('Chaque milestone doit etre strictement positive.');
            }

            $ratios[] = $ratio;
            $total += $ratio;
        }

        if (abs($total - 100.0) > 0.01) {
            throw new \RuntimeException('La somme des milestones doit etre egale a 100.');
        }

        return $ratios;
    }
}
