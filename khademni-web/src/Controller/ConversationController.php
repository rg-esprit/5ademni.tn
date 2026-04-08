<?php

namespace App\Controller;

use App\Entity\Conversation;
use App\Entity\ConversationMember;
use App\Entity\User;
use App\Repository\ConversationRepository;
use App\Repository\UserRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class ConversationController extends AbstractController
{
    private const ALLOWED_STATUSES = ['ACTIVE', 'ARCHIVED', 'CLOSED'];

    #[Route('/messages', name: 'app_conversations', methods: ['GET'])]
    public function index(Request $request, ConversationRepository $conversationRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $search = trim((string) $request->query->get('search', ''));
        $filter = (string) $request->query->get('filter', 'Toutes');

        $conversations = $conversationRepository->findForUser($user, $search, $filter);

        return $this->render('modules/conversations.html.twig', [
            'conversations' => $conversations,
            'search' => $search,
            'filter' => $filter,
            'filters' => ['Toutes', 'Actives', 'Inactives', 'Non lues'],
        ]);
    }

    #[Route('/messages/new', name: 'app_conversation_new', methods: ['GET', 'POST'])]
    public function create(Request $request, UserRepository $userRepository, ConversationRepository $conversationRepository, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $recipients = $userRepository->findAllExcept($user);
        $error = null;
        $selectedRecipientId = max(0, (int) $request->query->get('recipient', 0));

        if ($request->isMethod('POST')) {
            if (!$this->isCsrfTokenValid('conversation_new', (string) $request->request->get('_token'))) {
                $error = 'Jeton CSRF invalide. Veuillez réessayer.';
            } else {
                $recipientId = (int) $request->request->get('recipient');
                $selectedRecipientId = $recipientId;
                $title = trim((string) $request->request->get('title', '')) ?: 'Nouvelle conversation';

                $recipient = $userRepository->find($recipientId);
                if (!$recipient instanceof User) {
                    $error = 'Veuillez sélectionner un destinataire valide.';
                } elseif ($recipient->getId() === $user->getId()) {
                    $error = 'Vous ne pouvez pas envoyer un message à vous-même.';
                } else {
                    $existing = $conversationRepository->findExistingConversation($user, $recipient);
                    if ($existing instanceof Conversation) {
                        return $this->redirectToRoute('app_message_thread', ['id' => $existing->getId()]);
                    }

                    $conversation = new Conversation();
                    $conversation->setClient($user);
                    $conversation->setFreelance($recipient);
                    $conversation->setTitle($title);
                    $conversation->setStatut('ACTIVE');
                    $conversation->setNonLusClient(0);
                    $conversation->setNonLusFreelance(0);

                    $memberClient = new ConversationMember();
                    $memberClient->setConversation($conversation);
                    $memberClient->setUser($user);
                    $memberClient->setRole('CLIENT');
                    $memberClient->setInvitedBy($user);
                    $conversation->addMember($memberClient);
                    $entityManager->persist($memberClient);

                    $memberFreelance = new ConversationMember();
                    $memberFreelance->setConversation($conversation);
                    $memberFreelance->setUser($recipient);
                    $memberFreelance->setRole('FREELANCE');
                    $memberFreelance->setInvitedBy($user);
                    $conversation->addMember($memberFreelance);
                    $entityManager->persist($memberFreelance);

                    $entityManager->persist($conversation);
                    $entityManager->flush();

                    $this->addFlash('success', 'Conversation créée! Vous pouvez maintenant discuter avec ' . $recipient->getDisplayName());

                    // Ouvrir directement le chat et la conversation s'affichera dans la liste
                    return $this->redirectToRoute('app_message_thread', ['id' => $conversation->getId()]);
                }
            }
        }

        return $this->render('modules/conversation_new.html.twig', [
            'recipients' => $recipients,
            'error' => $error,
            'selectedRecipientId' => $selectedRecipientId,
        ]);
    }

    #[Route('/messages/{id}/status', name: 'app_conversation_status', methods: ['POST'])]
    public function changeStatus(int $id, Request $request, ConversationRepository $conversationRepository, EntityManagerInterface $entityManager): RedirectResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $conversation = $conversationRepository->findOneForUser($id, $user);

        if (!$conversation instanceof Conversation) {
            $this->addFlash('error', 'Conversation introuvable.');

            return $this->redirectToRoute('app_conversations');
        }

        if (!$this->isCsrfTokenValid('conversation_status_'.$conversation->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Jeton CSRF invalide. Le statut n’a pas été modifié.');

            return $this->redirectToRoute('app_conversations');
        }

        $status = strtoupper(trim((string) $request->request->get('status', 'ACTIVE')));
        if (!in_array($status, self::ALLOWED_STATUSES, true)) {
            $this->addFlash('error', 'Statut invalide.');

            return $this->redirectToRoute('app_conversations');
        }

        $conversation->setStatut($status);
        $entityManager->flush();

        $this->addFlash('success', 'Le statut a bien été mis à jour.');

        return $this->redirectToRoute('app_conversations');
    }

    #[Route('/messages/{id}/delete', name: 'app_conversation_delete', methods: ['POST'])]
    public function delete(int $id, Request $request, ConversationRepository $conversationRepository, EntityManagerInterface $entityManager): RedirectResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $conversation = $conversationRepository->findOneForUser($id, $user);

        if (!$conversation instanceof Conversation) {
            $this->addFlash('error', 'Conversation introuvable.');

            return $this->redirectToRoute('app_conversations');
        }

        if (!$this->isCsrfTokenValid('conversation_delete_'.$conversation->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Jeton CSRF invalide. Suppression annulée.');

            return $this->redirectToRoute('app_conversations');
        }

        $entityManager->remove($conversation);
        $entityManager->flush();
        $this->addFlash('success', 'Conversation supprimée avec succès.');

        return $this->redirectToRoute('app_conversations');
    }

    private function ensureConversationMembers(Conversation $conversation, EntityManagerInterface $entityManager): void
    {
        $existing = [];
        foreach ($conversation->getMembers() as $member) {
            if (null !== $member->getUser()) {
                $existing[$member->getUser()->getId()] = $member;
            }
        }

        $isDirty = false;
        if (null !== $conversation->getClient()) {
            $clientId = $conversation->getClient()->getId();
            if (isset($existing[$clientId])) {
                if ('CLIENT' !== $existing[$clientId]->getRole()) {
                    $existing[$clientId]->setRole('CLIENT');
                    $isDirty = true;
                }
            } else {
                $member = new ConversationMember();
                $member->setConversation($conversation);
                $member->setUser($conversation->getClient());
                $member->setRole('CLIENT');
                $entityManager->persist($member);
                $conversation->addMember($member);
                $isDirty = true;
            }
        }

        if (null !== $conversation->getFreelance()) {
            $freelanceId = $conversation->getFreelance()->getId();
            if (isset($existing[$freelanceId])) {
                if ('FREELANCE' !== $existing[$freelanceId]->getRole()) {
                    $existing[$freelanceId]->setRole('FREELANCE');
                    $isDirty = true;
                }
            } else {
                $member = new ConversationMember();
                $member->setConversation($conversation);
                $member->setUser($conversation->getFreelance());
                $member->setRole('FREELANCE');
                $entityManager->persist($member);
                $conversation->addMember($member);
                $isDirty = true;
            }
        }

        if ($isDirty) {
            $entityManager->flush();
        }
    }

    #[Route('/messages/{id}/members', name: 'app_conversation_members', methods: ['GET', 'POST'])]
    public function members(int $id, Request $request, ConversationRepository $conversationRepository, UserRepository $userRepository, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $conversation = $conversationRepository->findOneForUser($id, $user);

        if (!$conversation instanceof Conversation) {
            $this->addFlash('error', 'Conversation introuvable ou accès refusé.');

            return $this->redirectToRoute('app_conversations');
        }

        $this->ensureConversationMembers($conversation, $entityManager);

        if ($request->isMethod('POST')) {
            if (!$this->isGranted('ROLE_ADMIN')) {
                $this->addFlash('error', 'Seul un administrateur peut gérer les membres.');

                return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
            }

            if (!$this->isCsrfTokenValid('conversation_manage_'.$conversation->getId(), (string) $request->request->get('_token'))) {
                $this->addFlash('error', 'Jeton CSRF invalide.');

                return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
            }

            $action = (string) $request->request->get('action', '');

            if ('swap_roles' === $action) {
                $currentClient = $conversation->getClient();
                $currentFreelance = $conversation->getFreelance();

                $conversation->setClient($currentFreelance);
                $conversation->setFreelance($currentClient);
                $entityManager->flush();

                $this->addFlash('success', 'Les rôles des membres ont été inversés.');

                return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
            }

            if ('replace_member' === $action) {
                $role = (string) $request->request->get('role', '');
                $newUserId = (int) $request->request->get('new_user');
                $newUser = $userRepository->find($newUserId);

                if (!$newUser instanceof User) {
                    $this->addFlash('error', 'Utilisateur sélectionné invalide.');
                    return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
                }

                $otherParticipant = 'client' === $role ? $conversation->getFreelance() : $conversation->getClient();
                if (null !== $otherParticipant && $otherParticipant->getId() === $newUser->getId()) {
                    $this->addFlash('error', 'Le nouvel utilisateur ne peut pas être le second membre existant.');
                    return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
                }

                if ('client' === $role) {
                    $conversation->setClient($newUser);
                } elseif ('freelance' === $role) {
                    $conversation->setFreelance($newUser);
                } else {
                    $this->addFlash('error', 'Rôle invalide.');
                    return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
                }

                $entityManager->flush();
                $this->addFlash('success', 'Le membre a bien été remplacé.');

                return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
            }

            if ('remove_member' === $action) {
                $memberUserId = (int) $request->request->get('member_user');
                $memberToRemove = null;

                foreach ($conversation->getMembers() as $member) {
                    if ($member->getUser()?->getId() === $memberUserId) {
                        $memberToRemove = $member;
                        break;
                    }
                }

                if (!$memberToRemove instanceof ConversationMember) {
                    $this->addFlash('error', 'Membre introuvable ou non modifiable.');
                    return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
                }

                if (in_array($memberToRemove->getRole(), ['CLIENT', 'FREELANCE'], true)) {
                    $this->addFlash('error', 'Les participants principaux ne peuvent pas être retirés ici.');
                    return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
                }

                $conversation->removeMember($memberToRemove);
                $entityManager->remove($memberToRemove);
                $entityManager->flush();
                $this->addFlash('success', 'Le membre a été retiré de la conversation.');

                return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
            }

            if ('add_member' === $action) {
                $newUserId = (int) $request->request->get('new_user');
                $newUser = $userRepository->find($newUserId);

                if (!$newUser instanceof User) {
                    $this->addFlash('error', 'Utilisateur sélectionné invalide.');
                    return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
                }

                if ($conversation->hasMember($newUser)) {
                    $this->addFlash('error', 'Cet utilisateur fait déjà partie de la conversation.');
                    return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
                }

                $member = new ConversationMember();
                $member->setConversation($conversation);
                $member->setUser($newUser);
                $member->setRole('MEMBER');
                $member->setInvitedBy($user);
                $conversation->addMember($member);
                $entityManager->persist($member);
                $entityManager->flush();

                $this->addFlash('success', 'Le membre a été ajouté à la conversation.');
                return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
            }

            $this->addFlash('error', 'Action invalide.');
            return $this->redirectToRoute('app_conversation_members', ['id' => $conversation->getId()]);
        }

        $participants = $conversation->getParticipantUsers();
        $participantIds = array_map(fn(User $participant): int => $participant->getId(), $participants);

        $availableUsers = array_filter(
            $userRepository->findAll(),
            function (User $candidate) use ($participantIds): bool {
                return !in_array($candidate->getId(), $participantIds, true);
            }
        );

        return $this->render('modules/conversation_members.html.twig', [
            'conversation' => $conversation,
            'availableUsers' => $availableUsers,
        ]);
    }
}
