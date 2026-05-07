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

# Doctrine Doctor Report

## Scope

Doctrine Doctor was installed and enabled in the Symfony dev profiler because it was not present in the project before this check.

Package used:

```bash
composer require --dev ahmed-bhs/doctrine-doctor --no-interaction
```

Doctrine Doctor is a runtime Web Profiler analyzer, so the app was run locally and these pages were opened in `dev` mode:

- `/`
- `/profile`
- `/jobs/`
- `/gigs`
- `/articles`
- `/publications`
- `/categories`
- `/messages`
- `/contrat/`
- `/payment/`
- `/reviews`

## Summary

- Highest count seen: `10` critical, `10` warnings, `39` info.
- Common baseline on most pages: `8` critical, `8` warnings, `36` info.
- Doctor categories reported: Performance, Security, Integrity, Configuration.
- No application code was fixed.

## Page Results

- `/` and `/profile`: 8 critical, 8 warnings, 36 info, 1 query analyzed.
- `/jobs/`: 9 critical, 10 warnings, 39 info, 8 queries analyzed.
- `/gigs`: 8 critical, 9 warnings, 37 info, 3 queries analyzed.
- `/articles`: 9 critical, 10 warnings, 36 info, 5 queries analyzed.
- `/publications`: 10 critical, 9 warnings, 36 info, 7 queries analyzed.
- `/categories`: 8 critical, 8 warnings, 36 info, 1 query analyzed.
- `/messages`: 8 critical, 9 warnings, 36 info, 2 queries analyzed.
- `/contrat/`: 8 critical, 8 warnings, 36 info, 6 queries analyzed.
- `/payment/`: 8 critical, 8 warnings, 37 info, 6 queries analyzed.
- `/reviews`: 10 critical, 9 warnings, 36 info, 7 queries analyzed.

## Security Issues

- Critical: empty database password for user `root`. This should be fixed before any shared, staging, or production use. It is less urgent only if this database is strictly local development and unreachable from other machines.
- Warning: overprivileged database user `root`. This should be fixed before staging/production by using a dedicated application DB user. It is acceptable temporarily in local development.
- Warning: unprotected sensitive field `User::$password`. This should be reviewed because serialization can leak the field if the entity is serialized directly. If this entity is never serialized into API/log output, it is less urgent, but still worth protecting.
- Info: public getter exposes sensitive field `User::getPassword()`. This may be required by Symfony's `UserInterface`, so it is not necessarily wrong by itself. It should be treated as a review item, not an automatic fix.

## Integrity Issues

- Critical: composite primary key in `ConversationMember` with `conversation` and `user`. Not necessary to fix immediately if the mapping is working, but it can limit Doctrine features and makes future changes harder.
- Critical: float used for money in `User::$balance`, `Gig::$price`, and `Payment::$amount`. This should be fixed before handling real money amounts because floats can introduce rounding errors.
- Critical: missing `orphanRemoval=true` on composition-like relationships, including `Job::$milestones`. Not necessarily urgent if child rows are intentionally retained, but should be reviewed to avoid orphan records.
- Critical: foreign key `invited_by` does not use the `_id` suffix. This is a convention issue and is not necessary to fix unless the team wants naming consistency.
- Warning: `Gig::$userId` is mapped as a primitive integer foreign key instead of a Doctrine association. This should be fixed when touching the gig ownership model, but it is not an immediate runtime bug if current code relies on the integer field.
- Warning: `User::$savedJobs` uses a ManyToMany join table with an extra column `saved_at`. Doctrine Doctor recommends an explicit join entity. This is a design refactor, not an urgent fix unless `saved_at` is important behavior.
- Warning: property type mismatches, including `User::$dateOfBirth` and `Job::$location`, with 18 similar findings for date/nullability and 2 similar findings for string/nullability. These should be fixed gradually because mismatches can cause false updates or runtime expectation bugs.
- Warning: nullable creation timestamp on `Payment::$createdAt`. This should be fixed if every payment must always have a creation date.
- Warning: mutable timestamp datetime on `Article::$createdAt`. Not urgent, but using immutable dates is safer.
- Info: missing blameable/audit fields on entities with timestamps, including `JobMilestone`, `Commentaire`, `WorkLog`, `Payment`, `Favori`, `Notification`, and `Article`. Not necessary unless the project needs created-by/updated-by audit tracking.
- Info: mutable datetime fields across entities such as `User::$dateOfBirth`, `Job::$postedDate`, `JobMilestone::$createdAt`, `JobApplication::$applicationDate`, `Commentaire::$createdAt`, `WorkLog::$createdAt`, `Contrat::$dateContrat`, `Payment::$createdAt`, `Favori::$createdAt`, `Conversation::$dateCreation`, `Notification::$createdAt`, `Article::$createdAt`, and `Message::$dateEnvoi`. Not mandatory, but immutable dates reduce accidental state changes.
- Info: table names should be singular, with 9 similar findings. This is a convention preference and is not necessary to fix unless the team wants a schema naming cleanup.
- Info: public setters on timestamp fields, including `Job::$postedDate`, `JobMilestone::$createdAt`, `JobApplication::$applicationDate`, `Gig::$deliveryTime`, `Commentaire::$createdAt`, `WorkLog::$createdAt`, `Payment::$createdAt`, `Favori::$createdAt`, `Conversation::$dateCreation`, `Article::$createdAt`, and `Message::$dateEnvoi`. Not urgent, but safer if timestamps are system-managed.

## Configuration Issues

- Critical: missing metadata cache configuration in production Doctrine config. This should be fixed before production because it can significantly hurt performance.
- Info: 1 table uses a different collation from the database default. Doctrine Doctor says this appears intentional and is only problematic if joining against tables with another collation.
- Info: InnoDB full ACID durability is enabled in development. Not necessary to fix; changing it only improves local development write speed. Keep full durability in production.
- Info: binary logging is enabled in development. Not necessary to fix unless local disk usage/performance matters or replication is not being tested.

## Runtime Performance Issues

- `/jobs/`: critical over-eager loading with 3 collection joins in one query. This should be fixed if the page grows or has production data volume.
- `/jobs/`: warning for excessive eager loading with 4 joins, with 2 similar findings. This should be reviewed, but may be acceptable for small datasets.
- `/jobs/`: warning for unrestricted `findAll()` or SELECT without `LIMIT`, with 1 similar finding. This should be fixed with pagination/filtering for production-sized tables.
- `/jobs/`: info for inefficient `find()` queries where `getReference()` may be enough. Not necessary unless profiling shows this path matters.
- `/jobs/`: info for `ORDER BY` without `LIMIT`. Not urgent with small tables, but should be paginated for growth.
- `/jobs/`: info for unused eager load of `users`. This is a cleanup/performance improvement, not urgent.
- `/gigs`: warning for unrestricted `findAll()` or SELECT without `LIMIT`, with 1 similar finding. Should be fixed before large datasets.
- `/gigs`: info for `ORDER BY` without `LIMIT`. Not urgent with small data, but pagination is better.
- `/articles`: critical cartesian product from 2 collection joins on `favori` and `commentaire`. This should be fixed if the page uses real data volume.
- `/articles`: warning for unrestricted SELECT without `LIMIT`. Should be fixed with pagination/filtering.
- `/articles`: warning for over-eager loading with 2 collection joins, with 2 similar findings. Should be reviewed with the cartesian product issue.
- `/publications`: critical cartesian product from 2 collection joins on `favori` and `commentaire`. Should be fixed for production data volume.
- `/publications`: critical `setMaxResults()` with a collection join. This can partially hydrate collections and cause incorrect behavior, so it is more important than a simple performance warning.
- `/publications`: warning for over-eager loading with 2 collection joins, with 2 similar findings.
- `/messages`: warning for excessive eager loading with 4 joins. Review if the conversations page becomes slow.
- `/payment/`: info for inefficient `find()` queries where `getReference()` may be enough. Not necessary unless profiling shows an issue.
- `/reviews`: critical `setMaxResults()` with collection joins, reported twice. This can cause incorrect result hydration and should be reviewed.
- `/reviews`: warning for an unused `INNER JOIN` on `users`. This is a cleanup/performance fix, not urgent.

## What Does Not Need Immediate Fixing

- Naming convention findings, such as singular table names and `_id` suffixes, are not necessary unless the team wants consistency cleanup.
- Development-only database settings, such as binlog and InnoDB durability, do not need fixing unless local performance/disk usage is a problem.
- Audit/blameable suggestions are optional unless the product requires created-by/updated-by tracking.
- `getReference()` suggestions are micro-optimizations unless the affected pages are hot paths.
- Mutable datetime and public timestamp setters are not immediate bugs, but they are good hardening changes over time.

## Items Worth Prioritizing Later

- Database credentials: avoid empty password and root user outside isolated local development.
- Money fields: replace floats for balances/prices/payments before relying on financial accuracy.
- Production metadata cache: configure before deployment.
- Collection joins with `setMaxResults()` and cartesian products: review because they can cause incorrect hydration or poor scaling.

## Fixes Applied

- `src/Entity/User.php`: added Symfony serializer `#[Ignore]` to `User::$password`.
- `src/Entity/User.php`: added Symfony serializer `#[Ignore]` to `User::getPassword()`.

## What Happened

- Doctrine Doctor reported that the password field and its public getter could be exposed during serialization.
- The fix only changes serialization metadata; it does not change the database, Doctrine mapping, login behavior, or password storage.
- After rechecking Doctrine Doctor, the two password-related security warnings disappeared. The remaining security warnings are about using the database `root` user with an empty password.
