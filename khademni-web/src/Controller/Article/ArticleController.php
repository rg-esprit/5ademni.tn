<?php

namespace App\Controller\Article;

use App\Entity\Article;
use App\Repository\CommentaireRepository;
use App\Repository\FavoriRepository;
use App\Repository\ArticleRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use App\Form\ArticleType;
use Symfony\Component\String\Slugger\SluggerInterface;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use App\Service\EmailService;
use App\Service\SentimentAnalysisService;
use Dompdf\Dompdf;
use Dompdf\Options;
use Knp\Component\Pager\PaginatorInterface;

#[Route('/articles')]
class ArticleController extends AbstractController
{
    private $entityManager;

    public function __construct(EntityManagerInterface $entityManager)
    {
        $this->entityManager = $entityManager;
    }

    #[Route('', name: 'article_index', methods: ['GET'])]
    public function index(
        ArticleRepository $articleRepository, 
        PaginatorInterface $paginator, 
        Request $request
    ): Response {
        $queryBuilder = $articleRepository->createQueryBuilder('a')
            ->addSelect('(SELECT COUNT(f2.id) FROM App\\Entity\\Favori f2 WHERE f2.article = a) AS favorisCount')
            ->addSelect('(SELECT COUNT(c2.id) FROM App\\Entity\\Commentaire c2 WHERE c2.article = a) AS commentairesCount')
            ->orderBy('a.createdAt', 'DESC');

        $pagination = $paginator->paginate(
            $queryBuilder,
            $request->query->getInt('page', 1),
            10
        );

        $statusStats = $articleRepository->getStatusStats();

        $chartLabels = [];
        $favorisData = [];
        $commentairesData = [];

        // For the charts, we might want all visible ones or just a subset, 
        // but let's keep the logic of using current rows if that's what was intended.
        foreach ($pagination as $row) {
            $article = $row[0];
            $chartLabels[] = $article->getTitle();
            $favorisData[] = (int) $row['favorisCount'];
            $commentairesData[] = (int) $row['commentairesCount'];
        }

        return $this->render('article/list.html.twig', [
            'pagination' => $pagination,
            'statusStats' => $statusStats,
            'chartLabels' => $chartLabels,
            'favorisData' => $favorisData,
            'commentairesData' => $commentairesData,
        ]);
    }



    #[Route('/new', name: 'article_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em, SluggerInterface $slugger, EmailService $emailService): Response
    {
        $article = new Article();
    $form = $this->createForm(ArticleType::class, $article);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {
        $imageFile = $form->get('image')->getData();

        if ($imageFile) {
            $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
            $safeFilename = $slugger->slug($originalFilename);
            $newFilename = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

            try {
                $imageFile->move(
                    $this->getParameter('images_directory'),
                    $newFilename
                );

                $article->setImagePath($this->getParameter('images_directory') . DIRECTORY_SEPARATOR . $newFilename);
            } catch (FileException $e) {
                $this->addFlash('error', 'Une erreur est survenue lors du téléchargement de l\'image.');
                return $this->redirectToRoute('article_new');
            }
        }

        $article->setCreatedAt(new \DateTime());
        $em->persist($article);
        $em->flush();

        $this->addFlash('success', 'Article ajouté avec succès.');

        // Récupérer l'utilisateur connecté
        /** @var \App\Entity\User|null $user */
        $user = $this->getUser();
        if ($user && method_exists($user, 'getEmail')) {
            $emailService->sendEmail(
                $user->getEmail(),
               'Nouvel article créé : ' . $article->getTitle(),
    'Vous avez ajouté un nouvel article avec succès.',
    $article
            );
        }

        return $this->redirectToRoute('article_index', ['download_pdf' => $article->getId()]);
    }

    return $this->render('article/new.html.twig', [
        'form' => $form->createView(),
    ]);
}




#[Route('/{id}/edit', name: 'article_edit', methods: ['GET', 'POST'])]
public function edit(
    Request $request,
    Article $article,
    EntityManagerInterface $em,
    SluggerInterface $slugger,
    EmailService $emailService
): Response {
    $form = $this->createForm(ArticleType::class, $article);
    $form->handleRequest($request);

    if ($form->isSubmitted() && $form->isValid()) {
        $imageFile = $form->get('image')->getData();

        if ($imageFile) {
            $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
            $safeFilename = $slugger->slug($originalFilename);
            $newFilename = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

            try {
                $imageFile->move(
                    $this->getParameter('images_directory'),
                    $newFilename
                );

                $article->setImagePath(
                    $this->getParameter('images_directory') . DIRECTORY_SEPARATOR . $newFilename
                );
            } catch (FileException $e) {
                $this->addFlash('error', 'Erreur upload image');
            }
        }

        $em->flush();

        $this->addFlash('success', 'Article modifié avec succès.');

        // Récupérer l'utilisateur connecté
        /** @var \App\Entity\User|null $user */
        $user = $this->getUser();
        if ($user && method_exists($user, 'getEmail')) {
            $emailService->sendEmail(
                $user->getEmail(),
                  'Article modifié : ' . $article->getTitle(),
    'Vous avez modifié un article avec succès.',
    $article
            );
        }

        return $this->redirectToRoute('article_index');
    }

    return $this->render('article/edit.html.twig', [
        'article' => $article,
        'form' => $form->createView(),
    ]);
}



#[Route('/{id}/delete', name: 'article_delete', methods: ['POST'])]
public function delete(Article $article, Request $request, EntityManagerInterface $em, EmailService $emailService): Response
{
    if ($this->isCsrfTokenValid('delete_article_' . $article->getId(), (string) $request->request->get('_token'))) {
        $em->remove($article);
        $em->flush();

        $this->addFlash('success', 'Article supprimé avec succès.');

        // Récupérer l'utilisateur connecté
        /** @var \App\Entity\User|null $user */
        $user = $this->getUser();
        if ($user && method_exists($user, 'getEmail')) {
            $emailService->sendEmail(
               $user->getEmail(),
                'Article supprimé : ' . $article->getTitle(),
    'Vous avez supprimé un article avec succès. Titre de l\'article supprimé : ' . $article->getTitle()
            );
        }
    } else {
        $this->addFlash('error', 'Échec de la suppression de l\'article.');
    }

    return $this->redirectToRoute('article_index');
}

    #[Route('/dashboard', name: 'article_dashboard', methods: ['GET'])]
    public function dashboard(
        ArticleRepository $articleRepository,
        CommentaireRepository $commentaireRepository,
        FavoriRepository $favoriRepository,
        Request $request
    ): Response {
        $start = (new \DateTimeImmutable('today'))->modify('-29 days');
        $end = new \DateTimeImmutable('today');

        $rows = $articleRepository->findAllWithStats();
        $statusStats = $articleRepository->getStatusStats();

        $chartLabels = [];
        $favorisData = [];
        $commentairesData = [];

        foreach ($rows as $row) {
            $article = $row[0];
            $chartLabels[] = $article->getTitle();
            $favorisData[] = (int) $row['favorisCount'];
            $commentairesData[] = (int) $row['commentairesCount'];
        }
        // Get the selected period from the request (default to 1 month)
        $period = $request->query->get('period', '1'); // Default to 1 month if not provided

        // Calculate the start and end dates based on the selected period
        $end = new \DateTimeImmutable('today');
        switch ($period) {
            case '2': // Last 2 months
                $start = $end->modify('-2 months');
                break;
            case '12': // Last 1 year
                $start = $end->modify('-1 year');
                break;
            default: // Default to last 1 month
                $start = $end->modify('-1 month');
                break;
        }
        // Calcul des données pour le graphique
        $labelsByDay = [];
        $articlesByDay = [];
        $favorisByDay = [];
        $commentairesByDay = [];

        $cursor = $start;
        while ($cursor <= $end) {
            $key = $cursor->format('Y-m-d');
            $labelsByDay[] = $cursor->format('d/m');
            $articlesByDay[$key] = 0;
            $favorisByDay[$key] = 0;
            $commentairesByDay[$key] = 0;
            $cursor = $cursor->modify('+1 day');
        }

        $articles = $articleRepository->createQueryBuilder('a')
            ->andWhere('a.createdAt >= :start')
            ->andWhere('a.createdAt <= :end')
            ->setParameter('start', $start)
            ->setParameter('end', $end)
            ->getQuery()
            ->getResult();

        foreach ($articles as $a) {
            $d = $a->getCreatedAt()?->format('Y-m-d');
            if ($d && array_key_exists($d, $articlesByDay)) {
                $articlesByDay[$d]++;
            }
        }

        $favoris = $favoriRepository->createQueryBuilder('f')
            ->andWhere('f.createdAt >= :start')
            ->andWhere('f.createdAt <= :end')
            ->setParameter('start', $start)
            ->setParameter('end', $end)
            ->getQuery()
            ->getResult();

        foreach ($favoris as $f) {
            $d = $f->getCreatedAt()?->format('Y-m-d');
            if ($d && array_key_exists($d, $favorisByDay)) {
                $favorisByDay[$d]++;
            }
        }

        $commentaires = $commentaireRepository->createQueryBuilder('c')
            ->andWhere('c.createdAt >= :start')
            ->andWhere('c.createdAt <= :end')
            ->setParameter('start', $start)
            ->setParameter('end', $end)
            ->getQuery()
            ->getResult();

        foreach ($commentaires as $c) {
            $d = $c->getCreatedAt()?->format('Y-m-d');
            if ($d && array_key_exists($d, $commentairesByDay)) {
                $commentairesByDay[$d]++;
            }
        }

        $totalInteractions =
            $this->countByPeriod($favoriRepository, 'f', new \DateTimeImmutable('1970-01-01'), new \DateTimeImmutable('now')) +
            $this->countByPeriod($commentaireRepository, 'c', new \DateTimeImmutable('1970-01-01'), new \DateTimeImmutable('now'));
        $statusStats['interactions'] = $totalInteractions;
        $currentMonthStart = new \DateTimeImmutable('first day of this month 00:00:00');
        $nextMonthStart = $currentMonthStart->modify('+1 month');
        $previousMonthStart = $currentMonthStart->modify('-1 month');

        // ARTICLES
        $currentArticles = $this->countArticlesByPeriod($articleRepository, $currentMonthStart, $nextMonthStart);
        $previousArticles = $this->countArticlesByPeriod($articleRepository, $previousMonthStart, $currentMonthStart);

        // VISIBLE
        $currentVisibleArticles = $this->countArticlesByPeriod($articleRepository, $currentMonthStart, $nextMonthStart, 'VISIBLE');
        $previousVisibleArticles = $this->countArticlesByPeriod($articleRepository, $previousMonthStart, $currentMonthStart, 'VISIBLE');

        // MASQUE
        $currentHiddenArticles = $this->countArticlesByPeriod($articleRepository, $currentMonthStart, $nextMonthStart, 'MASQUE');
        $previousHiddenArticles = $this->countArticlesByPeriod($articleRepository, $previousMonthStart, $currentMonthStart, 'MASQUE');

        // INTERACTIONS
        $currentInteractions =
            $this->countByPeriod($favoriRepository, 'f', $currentMonthStart, $nextMonthStart) +
            $this->countByPeriod($commentaireRepository, 'c', $currentMonthStart, $nextMonthStart);

        $previousInteractions =
            $this->countByPeriod($favoriRepository, 'f', $previousMonthStart, $currentMonthStart) +
            $this->countByPeriod($commentaireRepository, 'c', $previousMonthStart, $currentMonthStart);

       
        $trends = [
            'total' => $this->buildTrend($currentArticles, $previousArticles),
            'visible' => $this->buildTrend($currentVisibleArticles, $previousVisibleArticles),
            'hidden' => $this->buildTrend($currentHiddenArticles, $previousHiddenArticles),
            'interactions' => $this->buildTrend($currentInteractions, $previousInteractions),
        ];
        
        return $this->render('article/dashboard.html.twig', [
            'rows' => $rows,
            'statusStats' => $statusStats,
            'chartLabels' => $chartLabels,
            'favorisData' => $favorisData,
            'commentairesData' => $commentairesData,
            'trends' => $trends,
            'activityLabels' => $labelsByDay,
            'articlesByDay' => array_values($articlesByDay),
            'favorisByDay' => array_values($favorisByDay),
            'commentairesByDay' => array_values($commentairesByDay),
        ]);
    }



    private function countByPeriod(object $repository, string $alias, \DateTimeImmutable $start, \DateTimeImmutable $end): int
    {
        return (int) $repository->createQueryBuilder($alias)
            ->select(sprintf('COUNT(%s.id)', $alias))
            ->andWhere(sprintf('%s.createdAt >= :start', $alias))
            ->andWhere(sprintf('%s.createdAt < :end', $alias))
            ->setParameter('start', $start)
            ->setParameter('end', $end)
            ->getQuery()
            ->getSingleScalarResult();
    }

    private function countArticlesByPeriod(
        ArticleRepository $articleRepository,
        \DateTimeImmutable $start,
        \DateTimeImmutable $end,
        ?string $status = null
    ): int {
        $qb = $articleRepository->createQueryBuilder('a')
            ->select('COUNT(a.id)')
            ->andWhere('a.createdAt >= :start')
            ->andWhere('a.createdAt < :end')
            ->setParameter('start', $start)
            ->setParameter('end', $end);

        if ($status !== null) {
            $qb->andWhere('a.status = :status')
                ->setParameter('status', $status);
        }

        return (int) $qb->getQuery()->getSingleScalarResult();
    }



    private function buildTrend(int $current, int $previous): array
    {
        if ($previous === 0) {
            $percent = $current > 0 ? 100 : 0;
        } else {
            $percent = (($current - $previous) / $previous) * 100;
        }

        $arrow = $percent >= 0 ? '↑' : '↓';
        $class = $percent >= 0 ? 'up' : 'down';

        return [
            'percent' => min(abs(round($percent)), 100), // 🔥 fix cercle
            'arrow' => $arrow,
            'class' => $class,
        ];
    }




    #[Route('/articles/list', name: 'article_list', methods: ['GET'])]
    public function list(ArticleRepository $articleRepository, PaginatorInterface $paginator, Request $request): Response
    {
        // Récupérer les paramètres de recherche et de filtrage
        $search = $request->query->get('search', '');
        $status = $request->query->get('status', '');

        // Utiliser le QueryBuilder pour la pagination
        $queryBuilder = $articleRepository->createQueryBuilder('a')
            ->addSelect('(SELECT COUNT(f2.id) FROM App\\Entity\\Favori f2 WHERE f2.article = a) AS favorisCount')
            ->addSelect('(SELECT COUNT(c2.id) FROM App\\Entity\\Commentaire c2 WHERE c2.article = a) AS commentairesCount');

        if ($search) {
            $queryBuilder->andWhere('a.title LIKE :search OR a.content LIKE :search')
                ->setParameter('search', '%' . $search . '%');
        }

        if ($status) {
            $queryBuilder->andWhere('a.status = :status')
                ->setParameter('status', $status);
        }

        $queryBuilder->orderBy('a.createdAt', 'DESC');

        $pagination = $paginator->paginate(
            $queryBuilder,
            $request->query->getInt('page', 1),
            10
        );

        return $this->render('article/list.html.twig', [
            'pagination' => $pagination,
        ]);
    }


      #[Route('/article/{id}', name: 'app_article_show', methods: ['GET'])]
    public function show(Article $article, SentimentAnalysisService $sentimentService): Response
    {
        $commentaires = $article->getCommentaires();
        $nombreFavoris = $article->getFavoris()->count();
        $nombreTotalCommentaires = $commentaires->count();
        
        // 1. On compte les commentaires positifs sans rien sauvegarder en base
        $nombreCommentairesPositifs = 0;
        foreach ($commentaires as $commentaire) {
            // On vérifie le texte en direct
            if ($sentimentService->isPositif($commentaire->getContent())) {
                $nombreCommentairesPositifs++;
            }
        }
        // 2. Le calcul des interactions
        $interactionsTotales = $nombreTotalCommentaires + $nombreFavoris;
        $interactionsPositives = $nombreCommentairesPositifs + $nombreFavoris;
        
        // Calcul du pourcentage (on évite la division par zéro)
        $pourcentage = 0;
        if ($interactionsTotales > 0) {
            $pourcentage = ($interactionsPositives / $interactionsTotales) * 100;
        }
        return $this->render('article/publications.html.twig', [
            'article' => $article,
            'pourcentagePositif' => round($pourcentage, 2) // On envoie le chiffre final à la vue !
        ]);
    }

    #[Route('/article/{id}/suggest-comments', name: 'api_article_suggest_comments', methods: ['GET'])]
    public function suggestComments(Article $article, Request $request, \App\Service\AiCommentSuggestionService $aiService): \Symfony\Component\HttpFoundation\JsonResponse
    {
        try {
            $context = trim((string) $request->query->get('context', ''));
            $suggestions = $aiService->suggest($article->getTitle() ?? '', $article->getContent() ?? '', $context);
            return $this->json(['success' => true, 'suggestions' => $suggestions]);
        } catch (\Exception $e) {
             return $this->json(['success' => false, 'error' => $e->getMessage()]);
        }
    }

    #[Route('/{id}/download-pdf', name: 'article_download_pdf', methods: ['GET'])]
    public function downloadPdf(Article $article): Response
    {
        $pdfOptions = new Options();
        $pdfOptions->set('defaultFont', 'Arial');
        $pdfOptions->set('isRemoteEnabled', true);

        $dompdf = new Dompdf($pdfOptions);

        $user = $this->getUser();
        $userName = 'Anonyme';
        if ($user) {
            if (method_exists($user, 'getDisplayName')) {
                $userName = $user->getDisplayName();
            } elseif (method_exists($user, 'getEmail')) {
                $userName = $user->getEmail();
            }
        }

        // Encoder le logo en base64 pour être sûr qu'il s'affiche dans le PDF
        $logoPath = $this->getParameter('kernel.project_dir') . '/public/images/logo.png';
        $logoBase64 = '';
        if (file_exists($logoPath)) {
            $logoData = file_get_contents($logoPath);
            $logoBase64 = 'data:image/png;base64,' . base64_encode($logoData);
        }

        // Encoder l'image de l'article si elle existe
        $articleImageBase64 = '';
        $articleImagePath = $article->getImagePath();
        // $articleImagePath est censé être un chemin absolu d'après la création
        if ($articleImagePath && file_exists($articleImagePath)) {
            $ext = pathinfo($articleImagePath, PATHINFO_EXTENSION);
            $imgData = file_get_contents($articleImagePath);
            $articleImageBase64 = 'data:image/' . $ext . ';base64,' . base64_encode($imgData);
        }

        $html = $this->renderView('article/pdf.html.twig', [
            'article' => $article,
            'userName' => $userName,
            'logoBase64' => $logoBase64,
            'articleImageBase64' => $articleImageBase64
        ]);

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return new Response(
            $dompdf->output(),
            Response::HTTP_OK,
            [
                'Content-Type' => 'application/pdf',
                'Content-Disposition' => 'attachment; filename="article-' . $article->getId() . '.pdf"'
            ]
        );
    }
}
