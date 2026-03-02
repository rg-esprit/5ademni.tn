# 5ademni.tn — Copilot Instructions

## Project Overview

A Tunisian freelance/job marketplace desktop app. Two-process architecture:

- **JavaFX 22** (`khademni/`) — UI, business logic, direct MySQL access via JDBC
- **Python FastAPI** (`face_backend/`) — face recognition microservice (camera + AI), port `5003`

## Running the Project

### 1. Start MySQL (Docker)

```bash
cd docker && docker compose up -d
```

DB: `appdb`, host: `localhost:3306`, user: `root`, password: _(empty)_

### 2. Start the Face Backend

```bash
/opt/anaconda3/bin/python -m uvicorn face_backend.main:app --host 0.0.0.0 --port 5003 --reload
```

Must run from repo root. Port 5003 specifically — 5000 is macOS AirPlay, 5001/5002 have stale sockets.

### 3. Run the JavaFX App

```bash
cd khademni && mvn javafx:run
```

## Architecture — Key Decisions

### Navigation Pattern

`App.setRoot("fxml-name")` replaces the single `Scene`'s root and swaps the stylesheet simultaneously. No separate windows except dialogs. All FXMLs live in `khademni/src/main/resources/com/khademni/`. Each FXML has a paired `.css` with the same base name.

### Global Session State

`App.currentUser` (static `UserModel`) is the only session store. Set on login/face-login, read everywhere. Check `App.getCurrentUser() != null` before any authenticated action.

### Database Access

`MyDataBase.getConnection()` returns a **shared singleton** connection — no connection pooling. It also runs `checkAndFixSchema()` on first connect, which safely runs `ALTER TABLE` migrations (e.g., adding `face_embedding` column), ignoring error code `1060` (duplicate column).

### Face ID — Cross-Process Flow

Java delegates **all** webcam/AI work to Python via HTTP form POST (forced HTTP/1.1 — uvicorn doesn't support h2c upgrades that Java's `HttpClient` defaults to):

- Enrollment: `POST /capture-and-enroll` with `user_id` + `frame_count=12`
- Login: `POST /capture-and-login` with `frame_count=5`

Python stores a **128-dim dlib ResNet-34 embedding** as a JSON array in `users.face_embedding`. Login uses nearest-neighbor L2 distance with threshold `0.55`. The `FaceController` must be configured before `dialog.showAndWait()`:

```java
fc.configureEnroll(userId);  // or fc.configureLogin()
```

### Password Hashing

SHA-256 hex digest, no salt. See `UserController.hashPassword()`.

## Database Schema (5 tables)

| Table              | Purpose                                                                        |
| ------------------ | ------------------------------------------------------------------------------ |
| `users`            | All users (clients + freelancers); `is_admin` flag; `face_embedding JSON NULL` |
| `jobs`             | Job listings posted by users                                                   |
| `job_applications` | Applications with status `PENDING/ACCEPTED/REJECTED`                           |
| `reviews`          | client_id → freelancer_id ratings (1–5)                                        |
| `contrats`         | Contracts between client and freelancer                                        |

## Controllers — One Controller, Multiple Views

`UserController` handles **login, signup, and profile** — three different FXMLs share one controller class. Null-check `@FXML` fields in `initialize()` to detect which view is active (e.g., `if (profileFirstNameField != null)`).

## Key Files

| File                                                                 | Role                                                                                  |
| -------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `khademni/src/main/java/com/khademni/App.java`                       | Entry point, navigation, session                                                      |
| `khademni/src/main/java/com/khademni/utils/MyDataBase.java`          | Singleton DB connection + schema migration                                            |
| `khademni/src/main/java/com/khademni/controller/FaceController.java` | Face ID dialog controller                                                             |
| `face_backend/main.py`                                               | FastAPI face service (webcam + dlib + MySQL)                                          |
| `schema.sql`                                                         | Canonical schema (not auto-applied — run manually or rely on `MyDataBase` migrations) |
| `docker/docker-compose.yml`                                          | MySQL + phpMyAdmin (port 8080)                                                        |

## Conventions

- Icons: Ikonli with FontAwesome 5 (`fas-*`) or Material2 (`mdi2-*`) via `<FontIcon iconLiteral="..."/>`
- CSS brand color: `#6c63ff` (purple accent), `#6c0df2` (logo), background: `#f7f5f8`
- All background threads use `new Thread(..., "descriptor-name"); t.setDaemon(true)` — never block the JavaFX thread for I/O
- Face backend HTTP responses always include `{"success": bool, "detail": "..."}` — Java reads `json.get("success").getAsBoolean()`
