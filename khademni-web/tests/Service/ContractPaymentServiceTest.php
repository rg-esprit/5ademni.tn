<?php

namespace App\Tests\Service;

use App\Entity\Contrat;
use App\Repository\PaymentRepository;
use App\Repository\UserRepository;
use App\Service\ContractPaymentService;
use Doctrine\ORM\EntityManagerInterface;
use PHPUnit\Framework\TestCase;

class ContractPaymentServiceTest extends TestCase
{
    public function testGetCheckoutDetailsReturnsFullAmountWithoutMilestones(): void
    {
        $service = $this->createService();

        $contrat = (new Contrat())
            ->setTitre('Landing Page Build')
            ->setPrix(1200.0)
            ->setDescription('Responsive landing page and revisions.');

        $details = $service->getCheckoutDetails($contrat);

        self::assertSame('Landing Page Build', $details['name']);
        self::assertSame(1200.0, $details['amount']);
    }

    public function testGetCheckoutDetailsUsesNextMilestoneAmount(): void
    {
        $paymentRepository = $this->createMock(PaymentRepository::class);
        $paymentRepository->expects(self::once())
            ->method('findBy')
            ->with(
                self::callback(static fn (array $criteria): bool => isset($criteria['contrat'], $criteria['status'])
                    && $criteria['contrat'] instanceof Contrat
                    && 'PAID' === $criteria['status']),
                ['createdAt' => 'ASC', 'id' => 'ASC']
            )
            ->willReturn([]);

        $service = $this->createService($paymentRepository);

        $contrat = (new Contrat())
            ->setTitre('Mobile App MVP')
            ->setPrix(1000.0)
            ->setDescription('Scope locked. (Milestones: 50/30/20)');

        $details = $service->getCheckoutDetails($contrat);

        self::assertSame('Mobile App MVP - Etape 1/3', $details['name']);
        self::assertSame(500.0, $details['amount']);
    }

    public function testInvalidMilestoneSplitThrowsException(): void
    {
        $service = $this->createService();

        $contrat = (new Contrat())
            ->setTitre('API Build')
            ->setPrix(900.0)
            ->setDescription('Backend work. (Milestones: 40/40)');

        $this->expectException(\RuntimeException::class);
        $this->expectExceptionMessage('La somme des milestones doit etre egale a 100.');

        $service->getCheckoutDetails($contrat);
    }

    private function createService(?PaymentRepository $paymentRepository = null): ContractPaymentService
    {
        return new ContractPaymentService(
            $this->createStub(EntityManagerInterface::class),
            $paymentRepository ?? $this->createStub(PaymentRepository::class),
            $this->createStub(UserRepository::class),
        );
    }
}
