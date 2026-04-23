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

        try {
            $notifications = $repo->findUnreadForUser($user);
        } catch (\Throwable) {
            return new JsonResponse([]);
        }

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

        try {
            return new JsonResponse(['count' => $repo->countUnreadForUser($user)]);
        } catch (\Throwable) {
            return new JsonResponse(['count' => 0]);
        }
    }

    #[Route('/{id}/read', name: 'api_notifications_read', methods: ['POST'])]
    public function markRead(int $id, NotificationRepository $repo, EntityManagerInterface $em): JsonResponse
    {
        $user = $this->getUser();
        try {
            $notification = $repo->find($id);
        } catch (\Throwable) {
            return new JsonResponse(['ok' => false], 404);
        }

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

        try {
            foreach ($repo->findUnreadForUser($user) as $n) {
                $n->setIsRead(true);
            }
            $em->flush();
        } catch (\Throwable) {
            return new JsonResponse(['ok' => true]);
        }

        return new JsonResponse(['ok' => true]);
    }
}
