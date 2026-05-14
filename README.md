# 5ademni.tn

5ademni.tn is a freelancing platform implemented in two versions that share the same project domain and database model:

- `khademni`: JavaFX desktop application built with Maven.
- `khademni-web`: Symfony 6.4 web application.
- `face_backend`: Python Face ID service used by the authentication flow.

The platform includes user accounts, gigs, jobs, applications, contracts, payments, messaging, reviews, articles, notifications, AI-assisted features, and Face ID authentication.

## Repository Structure

```text
.
├── khademni/       # JavaFX desktop version
├── khademni-web/   # Symfony web version
├── face_backend/   # Python Face ID API
├── schema.sql      # Database schema
└── sample_jobs.sql # Optional sample job data
```

## Requirements

- Java 17
- Maven
- PHP 8.1 or newer
- Composer
- MySQL or MariaDB
- Python with `uvicorn` and the dependencies from `face_backend/requirements.txt`
- Symfony CLI, optional but recommended for the web app

## Database

Both versions expect a MySQL database. The JavaFX app currently connects to:

```text
jdbc:mysql://localhost:3306/appdb
user: root
password: empty
```

Create and import the database:

```bash
mysql -u root -e "CREATE DATABASE IF NOT EXISTS appdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root appdb < schema.sql
```

Optional sample data:

```bash
mysql -u root appdb < sample_jobs.sql
```

For the Symfony app, configure `DATABASE_URL` in `khademni-web/.env.local` to point to the same database.

## JavaFX Desktop App

The desktop app is located in `khademni` and uses JavaFX, MySQL, Stripe, WebSocket messaging, OpenPDF, Jackson, JavaCV/OpenCV, Twilio, and AI-related HTTP services.

Run it with Maven:

```bash
cd khademni
mvn clean javafx:run
```

Main class:

```text
com.khademni.App
```

Important notes:

- The app uses Java 17 and JavaFX 22.
- Database settings are defined in `khademni/src/main/java/com/khademni/utils/MyDataBase.java`.
- Some features require external API keys or local services, such as Stripe, Twilio, AI services, and the Face ID backend.

## Symfony Web App

The web app is located in `khademni-web` and uses Symfony 6.4, Doctrine ORM, Twig, Symfony UX, Stripe, Messenger, Mailer, PDF tools, and AI-related services.

Install dependencies:

```bash
cd khademni-web
composer install
```

Create your local environment file:

```bash
cp .env .env.local
```

Update `DATABASE_URL` and any required service keys in `.env.local`.

Run database migrations if needed:

```bash
php bin/console doctrine:migrations:migrate
```

Start the web server:

```bash
symfony server:start
```

Alternative without Symfony CLI:

```bash
php -S 127.0.0.1:8000 -t public
```

## Face ID Backend

The Symfony web app uses the Face ID service on port `5003` for login and enrollment.

Install Python dependencies:

```bash
pip install -r face_backend/requirements.txt
```

Start the service from the project root:

```bash
python -m uvicorn face_backend.main:app --host 0.0.0.0 --port 5003
```

Check that it is running:

```bash
curl http://127.0.0.1:5003/health
```

Expected response:

```json
{ "status": "ok", "backend": "face_recognition+dlib", "threshold": 0.55 }
```

Stop the service with `Ctrl+C` if it is running in the foreground.

## Test Accounts

Use these accounts for local testing:

```text
freelancer@gmail.com
client@gmail.com
admin@gmail.com
```

Password for all test accounts:

```text
secret123
```

## Common Commands

Run JavaFX app:

```bash
cd khademni && mvn clean javafx:run
```

Run Symfony app:

```bash
cd khademni-web && symfony server:start
```

Run Symfony migrations:

```bash
cd khademni-web && php bin/console doctrine:migrations:migrate
```

Clear Symfony cache:

```bash
cd khademni-web && php bin/console cache:clear
```

Run Face ID backend:

```bash
python -m uvicorn face_backend.main:app --host 0.0.0.0 --port 5003
```

## Notes

- Do not commit `.env.local` files or real API keys.
- Keep the JavaFX and Symfony database schemas aligned when adding new features.
- External integrations such as Stripe, mail, Twilio, AI providers, and blob storage may require additional environment variables or credentials.
