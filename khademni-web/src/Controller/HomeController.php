<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class HomeController extends AbstractController
{
    #[Route('/', name: 'app_home', methods: ['GET'])]
    public function __invoke(): Response
    {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_profile');
        }

        return $this->redirectToRoute('app_login');
    }

    #[Route('/favicon.ico', name: 'app_favicon', methods: ['GET'])]
    public function favicon(): Response
    {
        return new Response('', Response::HTTP_NO_CONTENT, ['Content-Type' => 'image/x-icon']);
    }
}
