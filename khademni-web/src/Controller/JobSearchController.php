<?php

namespace App\Controller;

use App\Repository\JobRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/api/jobs', name: 'api_jobs_')]
class JobSearchController extends AbstractController
{
    #[Route('/search', name: 'search', methods: ['GET'])]
    public function search(Request $request, JobRepository $jobRepository): JsonResponse
    {
        $query = $request->query->get('q', '');
        $category = $request->query->get('category', '');
        $location = $request->query->get('location', '');
        $jobType = $request->query->get('job_type', '');
        $minSalary = (int) $request->query->get('min_salary', 0);
        $maxSalary = (int) $request->query->get('max_salary', 1000000);
        $sort = $request->query->get('sort', 'newest');

        $jobs = $jobRepository->searchJobs($query, $category, $location, $jobType, $sort, $minSalary, $maxSalary);

        $data = [];
        foreach ($jobs as $job) {
            $user = $job->getUser();
            $data[] = [
                'id'         => $job->getId(),
                'title'      => $job->getTitle(),
                'company'    => $job->getCompany(),
                'location'   => $job->getLocation(),
                'salary'     => $job->getSalaryDisplay(),
                'postedAt'   => $job->getPostedAgo(),
                'category'   => $job->getCategory(),
                'jobType'    => $job->getJobType(),
                'posterName' => $user ? $user->getFirstName() . ' ' . $user->getLastName() : null,
                'url'        => $this->generateUrl('app_job_show', ['id' => $job->getId()]),
            ];
        }

        return new JsonResponse($data);
    }

    #[Route('/salary-range', name: 'salary_range', methods: ['GET'])]
    public function getSalaryRange(JobRepository $jobRepository): JsonResponse
    {
        $range = $jobRepository->getMinMaxSalaries();
        return new JsonResponse([
            'min' => (int) ($range['minSal'] ?? 0),
            'max' => (int) max(($range['maxSal'] ?? 10000) * 1.15, 10000),
        ]);
    }
}
