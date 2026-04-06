<?php
namespace App\Controller\Article;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class IndexController extends AbstractController
{
    #[Route('/articles', name: 'article_home', methods: ['GET'])]
    public function __invoke(): Response
    {
        return $this->render('article/index.html.twig');
    }
}