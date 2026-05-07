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

## Verification

- Re-ran the same PHPStan command after the fix.
- Result: no PHPStan warnings reported for the selected six controllers.
