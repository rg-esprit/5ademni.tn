<?php

namespace App\Controller;

use App\Entity\User;
use App\Repository\NotificationRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/api/notifications')]
class NotificationController extends AbstractController
{
    #[Route('', name: 'api_notifications_list', methods: ['GET'])]
    public function list(NotificationRepository $repo): JsonResponse
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return new JsonResponse([], 401);
        }

        $notifications = $repo->findUnreadForUser($user);

        return new JsonResponse(array_map(fn($n) => [
            'id'      => $n->getId(),
            'message' => $n->getMessage(),
            'type'    => $n->getType(),
            'link'    => $n->getLink(),
            'time'    => $n->getTimeAgo(),
        ], $notifications));
    }

    #[Route('/count', name: 'api_notifications_count', methods: ['GET'])]
    public function count(NotificationRepository $repo): JsonResponse
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return new JsonResponse(['count' => 0]);
        }

        return new JsonResponse(['count' => $repo->countUnreadForUser($user)]);
    }

    #[Route('/{id}/read', name: 'api_notifications_read', methods: ['POST'])]
    public function markRead(int $id, NotificationRepository $repo, EntityManagerInterface $em): JsonResponse
    {
        $user = $this->getUser();
        $notification = $repo->find($id);

        if (!$notification || $notification->getRecipient() !== $user) {
            return new JsonResponse(['ok' => false], 403);
        }

        $notification->setIsRead(true);
        $em->flush();

        return new JsonResponse(['ok' => true]);
    }

    #[Route('/read-all', name: 'api_notifications_read_all', methods: ['POST'])]
    public function markAllRead(NotificationRepository $repo, EntityManagerInterface $em): JsonResponse
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return new JsonResponse(['ok' => false], 401);
        }

        foreach ($repo->findUnreadForUser($user) as $n) {
            $n->setIsRead(true);
        }
        $em->flush();

        return new JsonResponse(['ok' => true]);
    }
}
