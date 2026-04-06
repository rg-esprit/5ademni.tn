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

#[Route('/articles')]
class ArticleController extends AbstractController
{
    private $entityManager;

    public function __construct(EntityManagerInterface $entityManager)
    {
        $this->entityManager = $entityManager;
    }

    #[Route('', name: 'article_index', methods: ['GET'])]
    public function index(ArticleRepository $articleRepository): Response
    {
        $rows = $articleRepository->findAllWithStats();
        $statusStats = $articleRepository->getStatusStats();

        $chartLabels = [];
        $favorisData = [];
        $commentairesData = [];

        foreach ($rows as $row) {
            /** @var Article $article */
            $article = $row[0];
            $chartLabels[] = $article->getTitle();
            $favorisData[] = (int) $row['favorisCount'];
            $commentairesData[] = (int) $row['commentairesCount'];
        }

        return $this->render('article/list.html.twig', [
            'rows' => $rows,
            'statusStats' => $statusStats,
            'chartLabels' => $chartLabels,
            'favorisData' => $favorisData,
            'commentairesData' => $commentairesData,
        ]);
    }



    #[Route('/new', name: 'article_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em, SluggerInterface $slugger): Response
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

                    // Enregistrer le chemin complet dans la base de données
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
            return $this->redirectToRoute('article_index');
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
        SluggerInterface $slugger
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

                    // ✅ EXACTEMENT comme new()
                    $article->setImagePath(
                        $this->getParameter('images_directory') . DIRECTORY_SEPARATOR . $newFilename
                    );
                } catch (FileException $e) {
                    $this->addFlash('error', 'Erreur upload image');
                }
            }

            $em->flush();

            $this->addFlash('success', 'Article modifié avec succès');
            return $this->redirectToRoute('article_index');
        }

        return $this->render('article/edit.html.twig', [
            'article' => $article,
            'form' => $form->createView(),
        ]);
    }



    #[Route('/{id}/delete', name: 'article_delete', methods: ['POST'])]
    public function delete(Article $article, Request $request, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete_article_' . $article->getId(), (string) $request->request->get('_token'))) {
            $em->remove($article);
            $em->flush();
        }

        return $this->redirectToRoute('article_index');
    }

    #[Route('/articles/dashboard', name: 'article_dashboard', methods: ['GET'])]
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
    public function list(ArticleRepository $articleRepository, Request $request): Response
    {
        // Récupérer les paramètres de recherche et de filtrage
        $search = $request->query->get('search', '');
        $status = $request->query->get('status', '');

        // Appeler une méthode personnalisée dans le repository pour appliquer les filtres
        $rows = $articleRepository->findAllWithFilters($search, $status);

        return $this->render('article/list.html.twig', [
            'rows' => $rows,
        ]);
    }
}
