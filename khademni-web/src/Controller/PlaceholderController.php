<?php

namespace App\Controller;

use App\Entity\User;
use App\Service\BlobStorageService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class PlaceholderController extends AbstractController
{
    private const MODULES = [
        'jobs' => [
            'title' => 'Jobs',
            'description' => 'The desktop jobs area is the next module to port into Symfony.',
        ],
        'jobs-management' => [
            'title' => 'Jobs Management',
            'description' => 'The admin jobs management area will be ported here next.',
        ],
        'messages' => [
            'title' => 'Messages',
            'description' => 'Messaging will reuse the same shared database once the web port is added.',
        ],
        'blogs' => [
            'title' => 'Blogs',
            'description' => 'The blogs publishing screens are still pending in the Symfony port.',
        ],
        'articles' => [
            'title' => 'Articles',
            'description' => 'The articles module still needs its Symfony MVC version.',
        ],
        'contracts' => [
            'title' => 'Contracts',
            'description' => 'Contracts are not ported yet, but this route keeps the shared header complete.',
        ],
        'categories' => [
            'title' => 'Categories',
            'description' => 'Category management is still pending on the web side.',
        ],
        'gigs' => [
            'title' => 'Gigs',
            'description' => 'Gig management will be added on top of the existing shared data next.',
        ],
    ];

    #[Route('/modules/{slug}', name: 'app_module_placeholder', methods: ['GET'])]
    public function show(string $slug, BlobStorageService $blobStorageService): Response
    {
        if ('categories' === $slug) {
            return $this->redirectToRoute('app_categories');
        }

        if ('gigs' === $slug) {
            return $this->redirectToRoute('app_gigs');
        }

        if ('jobs-management' === $slug && !$this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_module_placeholder', ['slug' => 'jobs']);
        }

        $module = self::MODULES[$slug] ?? null;

        if (null === $module) {
            throw $this->createNotFoundException();
        }

        $user = $this->getUser();

        return $this->render('modules/placeholder.html.twig', [
            'module' => $module,
            'avatar_proxy_enabled' => $user instanceof User && $blobStorageService->isConfigured() && $user->hasProfileImage(),
        ]);
    }
}
