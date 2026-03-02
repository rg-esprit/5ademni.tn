-- ============================================================
-- khademni.tn - Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS appdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE appdb;

-- ------------------------------------------------------------
-- 1. users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(100)  NOT NULL,
    last_name      VARCHAR(100)  NOT NULL,
    date_of_birth  DATE          NOT NULL,
    balance        DOUBLE        NOT NULL DEFAULT 0.0,
    email          VARCHAR(255)  NOT NULL UNIQUE,
    password       VARCHAR(255)  NOT NULL,
    is_admin       TINYINT(1)    NOT NULL DEFAULT 0,
    profile_img    VARCHAR(500)  NOT NULL DEFAULT '',
    bio            TEXT,
    face_embedding JSON          NULL COMMENT '128-dim Facenet embedding stored as JSON array'
);

-- ------------------------------------------------------------
-- 2. jobs
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS jobs (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(255)  NOT NULL,
    company       VARCHAR(255)  NOT NULL,
    location      VARCHAR(255)  NOT NULL,
    description   TEXT          NOT NULL,
    category      VARCHAR(100)  NOT NULL,
    salary_range  VARCHAR(100),
    job_type      VARCHAR(50),
    posted_date   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    requirements  TEXT,
    user_id       INT           NOT NULL,
    CONSTRAINT fk_jobs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 3. job_applications
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS job_applications (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    job_id           INT           NOT NULL,
    user_id          INT           NOT NULL,
    title            VARCHAR(255)  NOT NULL,
    description      TEXT          NOT NULL,
    cv_path          VARCHAR(500),
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    application_date TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_applications_job  FOREIGN KEY (job_id)  REFERENCES jobs(id)  ON DELETE CASCADE,
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 4. reviews
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    client_id      INT           NOT NULL,
    freelancer_id  INT           NOT NULL,
    rating         TINYINT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text    TEXT          NOT NULL,
    CONSTRAINT fk_reviews_client     FOREIGN KEY (client_id)     REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_freelancer FOREIGN KEY (freelancer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 5. contrats
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contrats (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    client_id      INT           NOT NULL,
    freelancer_id  INT           NOT NULL,
    date_contrat   DATE          NOT NULL,
    description    TEXT,
    CONSTRAINT fk_contrats_client     FOREIGN KEY (client_id)     REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contrats_freelancer FOREIGN KEY (freelancer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 6. paiments
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS paiments (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    contrat_id     INT           NOT NULL,
    montant        DOUBLE        NOT NULL,
    date_paiement  DATE          NOT NULL DEFAULT (CURDATE()),
    methode        VARCHAR(100)  NOT NULL DEFAULT 'Flouci',
    CONSTRAINT fk_paiments_contrat FOREIGN KEY (contrat_id) REFERENCES contrats(id) ON DELETE CASCADE
);
