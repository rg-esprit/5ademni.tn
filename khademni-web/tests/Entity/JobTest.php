<?php

namespace App\Tests\Entity;

use App\Entity\Job;
use PHPUnit\Framework\TestCase;

class JobTest extends TestCase
{
    public function testSalaryDisplayShowsRange(): void
    {
        $job = (new Job())
            ->setMinSalary('1000')
            ->setMaxSalary('2500');

        self::assertSame('1000 - 2500', $job->getSalaryDisplay());
        self::assertSame('1000 - 2500', $job->getSalaryRange());
    }
}
