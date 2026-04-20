<?php


namespace App\Controller\Article;

use App\Entity\Article;
use App\Entity\Commentaire;
use App\Entity\Favori;
use App\Entity\User;
use App\Repository\ArticleRepository;
use App\Repository\CommentaireRepository;
use App\Repository\FavoriRepository;
use App\Repository\GigRepository;
use App\Service\CommentaireInapproprieService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use App\Service\SentimentAnalysisService; 
use App\Service\TranslationService;
use Knp\Component\Pager\PaginatorInterface;
use Symfony\Component\HttpFoundation\JsonResponse;

#[Route('/publications')]
class ArticlePublicationController extends AbstractController
{
#[Route('/blogs', name: 'article_blogs', methods: ['GET'])]
public function blogs(
    ArticleRepository $articleRepository,
    FavoriRepository $favoriRepository,
    CommentaireRepository $commentaireRepository
): Response {
    $articles = $articleRepository
        ->createQueryBuilderForVisibleOrderedBy('newest')
        ->setMaxResults(6)
        ->getQuery()
        ->getResult();

    $articleIds = array_map(static fn (Article $article): int => $article->getId(), $articles);

    return $this->render('article/blogs.html.twig', [
        'articles' => $articles,
        'favoriCounts' => [] === $articleIds ? [] : $favoriRepository->countByArticleIds($articleIds),
        'commentCounts' => [] === $articleIds ? [] : $commentaireRepository->countByArticleIds($articleIds),
    ]);
}

#[Route('', name: 'article_publications', methods: ['GET'])]
public function index(
    Request $request,
    ArticleRepository $articleRepository,
    FavoriRepository $favoriRepository,
    CommentaireRepository $commentaireRepository,
    SentimentAnalysisService $sentimentService, // <-- Ajout du service ici
    PaginatorInterface $paginator
): Response {
    $sort = $request->query->get('sort', 'favoris');
    $q = $request->query->get('q', '');
    $page = $request->query->getInt('page', 1);
    $queryBuilder = $articleRepository->createQueryBuilderForVisibleOrderedBy($sort, $q);
    
    $pagination = $paginator->paginate(
        $queryBuilder,
        $page,
        5
    );
    
    $articles = $pagination->getItems();
    $articleIds = [];
    foreach ($articles as $a) {
        $articleIds[] = $a->getId();
    }
    
    $favoriCounts = $favoriRepository->countByArticleIds($articleIds);
    $commentCounts = $commentaireRepository->countByArticleIds($articleIds);
    $commentsByArticle = [];
    $pourcentagesPositifs = []; // <-- Tableau pour stocker le score de chaque article
    foreach ($articles as $article) {
        $commentaires = $commentaireRepository->findVisibleByArticle($article);
        $commentsByArticle[$article->getId()] = $commentaires;
        
        // --- Calcul dynamique du pourcentage ici ---
        $nombreFavoris = $favoriCounts[$article->getId()] ?? 0;
        $nombreCommentairesPositifs = 0;
        
        foreach ($commentaires as $commentaire) {
            // Analyse le sentiment pour chaque commentaire de cet article
            if ($sentimentService->isPositif($commentaire->getContent())) {
                $nombreCommentairesPositifs++;
            }
        }
        
        $interactionsTotales = count($commentaires) + $nombreFavoris;
        $interactionsPositives = $nombreCommentairesPositifs + $nombreFavoris;
        
        // On calcule et on associe le pourcentage à l'ID de l'article
        $pourcentagesPositifs[$article->getId()] = $interactionsTotales > 0 
            ? round(($interactionsPositives / $interactionsTotales) * 100, 2) 
            : 0;
    }
    $currentUser = $this->getCurrentAppUser();
    $userFavoris = [];
    if ($currentUser) {
        foreach ($favoriRepository->findArticleIdsByUser($currentUser, $articleIds) as $id) {
            $userFavoris[(int) $id] = true;
        }
    }
    return $this->render('article/publications.html.twig', [
        'articles' => $articles,
        'pagination' => $pagination,
        'sort' => $sort,
        'q' => $q,
        'favoriCounts' => $favoriCounts,
        'commentCounts' => $commentCounts,
        'commentsByArticle' => $commentsByArticle,
        'userFavoris' => $userFavoris,
        'currentUser' => $currentUser,
        'pourcentagesPositifs' => $pourcentagesPositifs, // <-- On envoie le tableau à Twig
    ]);
}





    #[Route('/{id}/comments', name: 'article_publication_comment_add', methods: ['POST'])]
    public function addComment(
         Article $article,
    Request $request,
    EntityManagerInterface $em,
    ValidatorInterface $validator,
    CommentaireInapproprieService $commentaireInapproprieService
): RedirectResponse {
    $comment = new Commentaire();
    $comment->setArticle($article);
    $content = trim((string) $request->request->get('content', ''));

     // Vérifier si le commentaire contient des propos inappropriés
    if (!$commentaireInapproprieService->isCommentAcceptable($content)) {
        $this->addFlash('error', 'Votre commentaire contient des propos inappropriés et ne peut pas être publié.');
        return $this->redirectToRoute('article_publications');
    }
    // Check if the comment has more than 3 words
    if (str_word_count($content) < 3) {
        $this->addFlash('error', 'Le commentaire doit contenir au moins 3 mots.');
        return $this->redirectBack($request);
    }

    $comment->setContent($content);
    $comment->setStatus('VISIBLE');
    $comment->setCreatedAt(new \DateTime());

    // Associer l'utilisateur connecté au commentaire
    $user = $this->getCurrentAppUser();
    if ($user) {
        $comment->setUser($user);
    }

    // Validate the entity
    $errors = $validator->validate($comment);
    if (count($errors) > 0) {
        foreach ($errors as $error) {
            $this->addFlash('error', $error->getMessage());
        }
        return $this->redirectBack($request);
    }

    $em->persist($comment);
    $em->flush();

    $this->addFlash('success', 'Commentaire ajouté avec succès.');
    return $this->redirectBack($request);
}



  
// #[Route('/comments/{id}/edit', name: 'article_publication_comment_edit', methods: ['POST'])]
// public function editComment(
//     Commentaire $commentaire,
//     Request $request,
//     EntityManagerInterface $em,
//     ValidatorInterface $validator,
//     commentaireInapproprieService $commentaireInapproprieService
// ): RedirectResponse {
//     if (!$this->isCsrfTokenValid('comment_edit_' . $commentaire->getId(), $request->request->get('_token'))) {
//         $this->addFlash('error', 'Token CSRF invalide.');
//         return $this->redirectBack($request);
//     }

//     $commentaire->setContent(trim((string) $request->request->get('content', '')));
//  // Vérifier si le commentaire contient des propos inappropriés
//     if (!$commentaireInapproprieService->isCommentAcceptable($content)) {
//         $this->addFlash('error', 'Votre commentaire contient des propos inappropriés et ne peut pas être publié.');
//         return $this->redirectToRoute('article_publications');
//     }
//     // Validate the entity
//     $errors = $validator->validate($commentaire);
//     if (count($errors) > 0) {
//         foreach ($errors as $error) {
//             $this->addFlash('error', $error->getMessage());
//         }
//         return $this->redirectBack($request);
//     }

//     $em->flush();

//     $this->addFlash('success', 'Commentaire modifié avec succès.');
//     return $this->redirectBack($request);
// }



#[Route('/comments/{id}/edit', name: 'article_publication_comment_edit', methods: ['POST'])]
public function editComment(
    Commentaire $commentaire,
    Request $request,
    EntityManagerInterface $em,
    ValidatorInterface $validator,
    CommentaireInapproprieService $commentaireInapproprieService
): RedirectResponse {
    if (!$this->isCsrfTokenValid('comment_edit_' . $commentaire->getId(), $request->request->get('_token'))) {
        $this->addFlash('error', 'Token CSRF invalide.');
        return $this->redirectBack($request);
    }

    $content = trim((string) $request->request->get('content', ''));
    $commentaire->setContent($content);

    // Vérifier si le commentaire contient des propos inappropriés
    if (!$commentaireInapproprieService->isCommentAcceptable($content)) {
        $this->addFlash('error', 'Votre commentaire contient des propos inappropriés et ne peut pas être publié.');
        return $this->redirectBack($request);
    }

    // Valider l'entité
    $errors = $validator->validate($commentaire);
    if (count($errors) > 0) {
        foreach ($errors as $error) {
            $this->addFlash('error', $error->getMessage());
        }
        return $this->redirectBack($request);
    }

    $em->flush();

    $this->addFlash('success', 'Commentaire modifié avec succès.');
    return $this->redirectBack($request);
}


    #[Route('/comments/{id}/delete', name: 'article_publication_comment_delete', methods: ['POST'])]
    public function deleteComment(
        Commentaire $commentaire,
        Request $request,
        EntityManagerInterface $em
    ): RedirectResponse {
        // Remove user authentication check

        // Validate the CSRF token
        if ($this->isCsrfTokenValid('comment_delete_' . $commentaire->getId(), (string) $request->request->get('_token'))) {
            $em->remove($commentaire);
            $em->flush();
        }

        return $this->redirectBack($request);
    }
    private function getCurrentAppUser(): ?User
    {
        $user = $this->getUser();
        return $user instanceof User ? $user : null;
    }

    private function redirectBack(Request $request): RedirectResponse
    {
        return $this->redirect($request->headers->get('referer') ?: $this->generateUrl('article_publications'));
    }

#[Route('/{id}/toggle-comments', name: 'article_publication_toggle_comments', methods: ['POST'])]
public function toggleComments(Article $article, Request $request): Response
{
    $commentsVisible = $request->request->get('commentsVisible', false);

    return $this->render('article/publications.html.twig', [
        'article' => $article,
        'commentsVisible' => $commentsVisible,
    ]);
}



#[Route('/comments/{id}/save', name: 'article_publication_comment_save', methods: ['POST'])]
public function saveComment(
    Commentaire $commentaire,
    Request $request,
    EntityManagerInterface $em,
    ValidatorInterface $validator
): RedirectResponse {
    if (!$this->isCsrfTokenValid('comment_save_' . $commentaire->getId(), $request->request->get('_token'))) {
        $this->addFlash('error', 'Token CSRF invalide.');
        return $this->redirectBack($request);
    }

    $commentaire->setContent(trim((string) $request->request->get('content', '')));

    // Validate the entity
    $errors = $validator->validate($commentaire);
    if (count($errors) > 0) {
        foreach ($errors as $error) {
            $this->addFlash('error', $error->getMessage());
        }
        return $this->redirectBack($request);
    }

    $em->flush();

    $this->addFlash('success', 'Commentaire sauvegardé avec succès.');
    return $this->redirectBack($request);
}

    #[Route('/{id}/favorite', name: 'article_publication_toggle_favorite', methods: ['POST'])]
    public function toggleFavorite(
        Article $article,
        Request $request,
        FavoriRepository $favoriRepository,
        EntityManagerInterface $em
    ): RedirectResponse {
        // Permettre les favoris sans utilisateur connecté
        $user = $this->getCurrentAppUser();

        if (!$this->isCsrfTokenValid('favorite_' . $article->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Token CSRF invalide.');
            return $this->redirectBack($request);
        }

        // Rechercher un favori existant pour cet article (user_id peut être NULL)
        $existing = $favoriRepository->findOneBy(['article' => $article, 'user' => $user]);

        if ($existing) {
            // Supprimer le favori si déjà existant
            $em->remove($existing);
        } else {
            // Ajouter un nouveau favori
            $favori = new Favori();
            $favori->setArticle($article);
            $favori->setUser($user); // Peut être NULL
            $favori->setCreatedAt(new \DateTime());
            $em->persist($favori);
        }

        $em->flush();

        return $this->redirectBack($request);
    }





#[Route('/favoris', name: 'favoris_list', methods: ['GET'])]
public function listFavoris(FavoriRepository $favoriRepository): Response
{
    // Get the currently logged-in user
    $user = $this->getUser();

    // Ensure the user is logged in
    if (!$user) {
        $this->addFlash('error', 'Vous devez être connecté pour voir vos favoris.');
        return $this->redirectToRoute('app_login');
    }

    // Fetch the favorites for the logged-in user
    $favoris = $favoriRepository->findBy(['user' => $user]);

    return $this->render('article/favoris_cards.html.twig', [
        'favoris' => $favoris,
    ]);
}


    #[Route('/favoris/delete/{id}', name: 'favoris_delete', methods: ['POST'])]
    public function deleteFavori(Favori $favori, EntityManagerInterface $em, Request $request): RedirectResponse
    {
        if (!$this->isCsrfTokenValid('delete' . $favori->getId(), $request->request->get('_token'))) {
            $this->addFlash('error', 'Token CSRF invalide.');
            return $this->redirectToRoute('favoris_list');
        }

        $em->remove($favori);
        $em->flush();

        $this->addFlash('success', 'Favori supprimé avec succès.');
        return $this->redirectToRoute('favoris_list');
    }


    #[Route('/favoris/add/{id}', name: 'favoris_add', methods: ['POST'])]
    public function addFavori(Article $article, EntityManagerInterface $em): RedirectResponse
    {
        $user = $this->getCurrentAppUser();

        if (!$user) {
            $this->addFlash('error', 'Vous devez être connecté pour ajouter un favori.');
            return $this->redirectToRoute('app_login');
        }

        $favori = new Favori();
        $favori->setUser($user);
        $favori->setArticle($article);
        $favori->setCreatedAt(new \DateTime());

        $em->persist($favori);
        $em->flush();

        $this->addFlash('success', 'Article ajouté aux favoris.');
        return $this->redirectToRoute('favoris_list');
    }







    #[Route('/{id}/gigs', name: 'article_gigs', methods: ['GET'])]
    public function showGigs(Article $article, GigRepository $gigRepository): Response
    {
         $keywords = $article->getTitle() . ' ' . $article->getContent();
    
    $gigs = $gigRepository->findByArticleKeywords($keywords);

    

        return $this->render('article/gigs.html.twig', [
            'article' => $article,
            'gigs' => $gigs,
        ]);
    }







 
#[Route('/comments/{id}/translate/{lang}', name: 'comment_translate', methods: ['GET'])]
public function translateComment(
    Commentaire $commentaire,
    string $lang,
    TranslationService $translationService
): JsonResponse {
    $translatedContent = $translationService->translate($commentaire->getContent(), $lang);

    return new JsonResponse([
        'success' => true,
        'translatedContent' => $translatedContent,
    ]);
}


}