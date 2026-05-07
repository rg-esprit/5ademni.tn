# PHPStan Controller Report

## Scope

Command run:

```bash
vendor/bin/phpstan analyse src/Controller/ProfileController.php src/Controller/GigController.php src/Controller/JobController.php src/Controller/ContratController.php src/Controller/MessageController.php src/Controller/Article/ArticleController.php --no-progress --error-format=raw --memory-limit=1G
```

Controllers checked:

- User module: `src/Controller/ProfileController.php`
- Gigs module: `src/Controller/GigController.php`
- Jobs module: `src/Controller/JobController.php`
- Contract module: `src/Controller/ContratController.php`
- Messages module: `src/Controller/MessageController.php`
- Articles module: `src/Controller/Article/ArticleController.php`

## Warnings Found

- `src/Controller/ProfileController.php:85`: Instantiated class `Symfony\Component\Security\Core\Exception\CsrfTokenNotFoundException` not found. Identifier: `class.notFound`.

## Fixes Applied

- `src/Controller/ProfileController.php`: Replaced the non-existent `Symfony\Component\Security\Core\Exception\CsrfTokenNotFoundException` import and instantiation with the installed Symfony exception `Symfony\Component\Security\Core\Exception\InvalidCsrfTokenException`.
  => commit hash : 63092dd505959821f00d0695bc99a7055850de74

## Verification

- Re-ran the same PHPStan command after the fix.
- Result: no PHPStan warnings reported for the selected six controllers.

# PHPUnit Unit Test Report

## Scope

Command run:

```bash
vendor/bin/phpunit --testdox
```

## How to Run the PHPUnit Tests

Run all PHPUnit tests from the `khademni-web` folder:

```bash
vendor/bin/phpunit --testdox
```

Run only the six module-related test files:

```bash
vendor/bin/phpunit --testdox tests/Service/ReviewAiServiceTest.php tests/Entity/GigTest.php tests/Entity/JobTest.php tests/Service/ContractPaymentServiceTest.php tests/Entity/MessageTest.php tests/Entity/ArticleTest.php
```

Unit tests checked:

- User module: `tests/Service/ReviewAiServiceTest.php`
- Gigs module: `tests/Entity/GigTest.php`
- Jobs module: `tests/Entity/JobTest.php`
- Contract module: `tests/Service/ContractPaymentServiceTest.php`
- Messages module: `tests/Entity/MessageTest.php`
- Articles module: `tests/Entity/ArticleTest.php`

## Six Tests Reviewed

- `ReviewAiServiceTest::testGenerateUsesExtendedLegacyTimeoutWhenOpenRouterIsDisabled`: verifies AI review generation for the user review feature.
- `GigTest::testApprovedGigIsExpiredAfterDeliveryDate`: verifies an approved gig becomes expired after its delivery date.
- `JobTest::testSalaryDisplayShowsRange`: verifies a job salary range is formatted and stored correctly.
- `ContractPaymentServiceTest::testGetCheckoutDetailsReturnsFullAmountWithoutMilestones`: verifies contract checkout uses the full amount when there are no milestones.
- `MessageTest::testMessageDetectsAttachment`: verifies a message correctly detects an attached file.
- `ArticleTest::testArticleCreatedAtAcceptsImmutableDate`: verifies an article creation date is stored correctly.

## Result

- Added separate PHPUnit unit test files for the missing Gigs, Jobs, Messages, and Articles modules.
- Reused existing valid tests for User reviews and Contract payments.

## Verification

- PHPUnit result: `OK (15 tests, 60 assertions)`.
- Each required module now has at least one unit test for a feature.
