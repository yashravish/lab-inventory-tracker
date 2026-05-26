# Lab Inventory Tracker

A simple scientific lab inventory tracker inspired by reagent inventory and R&D
data-management workflows. Tracks reagents (name, supplier, quantity, unit, storage
location, expiration date, minimum quantity), surfaces low-stock and expired items
on a dashboard, and exposes the same data through a React web UI and a Java Swing
admin viewer backed by a Spring Boot REST API.

## Why this is relevant to DeltaSoft

DeltaSoft Inc. builds scientific and R&D workflow software. This project is
deliberately scoped to look like the kind of internal lab tooling DeltaSoft
ships: a reagent inventory CRUD app with a dense, table-first UI, a thin Swing
admin, and a typical Java + React + PostgreSQL stack — the same one called out
in the Java Full Stack Developer role.

## Tech stack

- **Frontend:** React, TypeScript, Vite, plain CSS (no UI libraries)
- **Backend:** Java 17, Spring Boot 3, Spring Web, Spring Data JPA, Bean Validation
- **Database:** PostgreSQL (runtime, via Docker), H2 in-memory (tests)
- **Desktop:** Java Swing admin viewer (read-only) using `java.net.http.HttpClient` + Jackson
- **Testing:** JUnit 5 + Spring Boot Test + MockMvc (backend), Vitest + React Testing Library (frontend), JUnit 5 (Swing)
- **CI:** GitHub Actions running backend, frontend, and Swing test jobs

## Features

- Reagent CRUD: create, list, update, delete
- Server-side search across name, supplier, and storage location
- Dashboard summary: total / low-stock / expired / in-stock counts
- Computed status logic with **EXPIRED > LOW_STOCK > IN_STOCK** precedence
- Status pills + clickable dashboard tiles that act as table filters
- Right-side drawer form for create/edit (table stays visible behind it)
- Sortable columns, expiration "expires in N days" hint, "expired" marker
- Java Swing admin viewer for the same API (read-only)

## Architecture

```
   React Frontend     ─┐
                       ├─► Spring Boot REST API ─► PostgreSQL
   Swing Admin Viewer ─┘
```

## API endpoints

All `/api/**` routes require authentication. The SPA uses a session cookie issued
by `POST /api/auth/login`; the Swing viewer and `curl` use HTTP Basic against the
same filter chain.

| Method | Path                              | Description                                  |
|--------|-----------------------------------|----------------------------------------------|
| POST   | `/api/auth/login`                 | `{username, password}` → 200 + JSESSIONID, or 401 |
| GET    | `/api/auth/me`                    | `{username, displayName, role}` if authenticated, else 401 |
| POST   | `/api/auth/logout`                | Invalidates the session, returns 204         |
| GET    | `/api/reagents`                   | List all reagents; `?search=` filters by name, supplier, or storage location |
| GET    | `/api/reagents/{id}`              | Get a single reagent                         |
| POST   | `/api/reagents`                   | Create a reagent                             |
| PUT    | `/api/reagents/{id}`              | Update a reagent                             |
| DELETE | `/api/reagents/{id}`              | Delete a reagent                             |
| GET    | `/api/dashboard/summary`          | Counts: `totalReagents`, `lowStockCount`, `expiredCount`, `inStockCount` |

### Response shape

```json
{
  "id": 1,
  "name": "Ethanol",
  "supplier": "Fisher Sci",
  "quantity": 12.5,
  "unit": "L",
  "storageLocation": "Lab-2 / Flammables",
  "expirationDate": "2027-01-15",
  "minimumQuantity": 5,
  "lowStock": false,
  "expired": false,
  "status": "IN_STOCK"
}
```

`status` is computed on the server: `EXPIRED` if `expirationDate` is before today,
else `LOW_STOCK` if `quantity <= minimumQuantity`, else `IN_STOCK`.

## Database schema

Single table `reagents`:

| Column            | Type          | Notes                          |
|-------------------|---------------|--------------------------------|
| id                | bigserial     | primary key                    |
| name              | varchar       | required                       |
| supplier          | varchar       |                                |
| quantity          | numeric(18,4) | required, ≥ 0                  |
| unit              | varchar       | e.g. g, mg, L, mL              |
| storage_location  | varchar       |                                |
| expiration_date   | date          |                                |
| minimum_quantity  | numeric(18,4) | required, ≥ 0, defaults to 5   |

Hibernate generates this with `spring.jpa.hibernate.ddl-auto=update` against
PostgreSQL and `create-drop` against H2 in the test profile.

## How to run

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This launches PostgreSQL 16 on `localhost:5434` (5432/5433 are intentionally
left free for other local Postgres instances) with database, user, and password
all set to `labinventory`.

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The API listens on **http://localhost:8082**. On the first run a seed loader
inserts 10 reagents (mix of in-stock, low-stock, expired).

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5175**. Override the backend URL with
`VITE_API_BASE_URL=http://localhost:8082`.

### 4. Run the Swing admin viewer

```bash
cd swing-admin
LAB_INVENTORY_USER=yash.s LAB_INVENTORY_PASSWORD=labtech mvn -q compile exec:java
```

Override the API URL with `LAB_INVENTORY_API=http://localhost:8082`. The Swing
viewer is read-only and uses the same REST API. Credentials default to
`yash.s` / `labtech` if the env vars are absent; a wrong password surfaces an
"Authentication failed — check LAB_INVENTORY_USER / LAB_INVENTORY_PASSWORD."
dialog instead of a stack trace.

## Authentication

The app ships with one seeded user — `yash.s` / `labtech` by default. The seed
loader runs on the first startup (skipped in the test profile) and inserts a row
into `app_users` with a BCrypt-hashed password.

Override the defaults via environment variables or a `.env` file at the project
root (see `.env.example`):

```env
APP_SEED_USERNAME=yash.s
APP_SEED_PASSWORD=labtech
APP_SEED_DISPLAY_NAME=Lab Tech
```

Spring Boot picks up `./.env` automatically (`spring.config.import=optional:file:./.env[.properties]`).
The file is gitignored.

### One filter chain, three clients

| Client                 | Auth mechanism                          |
|------------------------|-----------------------------------------|
| React frontend (SPA)   | Session cookie issued by `POST /api/auth/login` |
| Swing admin viewer     | HTTP Basic header (env-configured)      |
| `curl` / scripts       | HTTP Basic header                       |

The audit log is now tied to the real authenticated principal — every `ReagentEvent`
records `actor = SecurityContextHolder.getContext().getAuthentication().getName()`
instead of a hardcoded string. Events created by background loaders (e.g. the
seed runner) get `actor = "system"`.

### What's intentionally not here

- **One role only.** A `role` column exists on `app_users`; today every seeded
  user is `LAB_TECH`. Endpoints are not gated by role yet — adding `ROLE_ADMIN`
  is a row insert plus a `.hasRole()` constraint.
- **No CSRF token.** The SPA is same-origin and the cookie is session-scoped;
  CSRF is intentionally disabled. For a multi-tenant production deployment
  re-enable CSRF and switch to the cookie-token (`XSRF-TOKEN`) pattern.
- **Sessions over JWT.** Deliberate. Sessions live in an HTTP-only cookie,
  which sidesteps XSS token-leakage and removes the key/refresh-token plumbing
  a JWT setup would need.

## Deploy to Railway

The repo ships with Dockerfiles and `railway.toml` files for the backend and
frontend so each can be deployed as a separate Railway service against a
Railway PostgreSQL plugin. The Swing admin viewer stays local — it's a desktop
app and is not deployed.

```
Browser ─► frontend (Railway, nginx) ─► backend (Railway, Spring Boot) ─► PostgreSQL plugin
```

### Prerequisites

- A Railway account and a project linked to this GitHub repo.
- (Optional) the Railway CLI — every step below also works from the dashboard.

### 1. Provision PostgreSQL

In the Railway project, **+ New → Database → PostgreSQL**. Railway adds a
`DATABASE_URL` variable to the project; you don't need to copy the host/port
manually — the backend's `DatabaseUrlEnvironmentPostProcessor` parses
`DATABASE_URL` into `spring.datasource.*`.

### 2. Deploy the backend

1. **+ New → GitHub Repo →** pick this repo.
2. In the new service's **Settings**, set **Root Directory** to `backend`.
   Railway will detect `backend/railway.toml` and `backend/Dockerfile`.
3. Open the service's **Variables** tab and add:

   | Variable | Value | Notes |
   |---|---|---|
   | `DATABASE_URL` | (reference Postgres plugin) | Click *Add Reference Variable* → Postgres → `DATABASE_URL`. |
   | `SPRING_PROFILES_ACTIVE` | `prod` | Activates `SameSite=None; Secure` cookies. |
   | `APP_CORS_ALLOWED_ORIGINS` | `https://<frontend>.up.railway.app` | Exact origin of the frontend service, no trailing slash. Multiple values comma-separated. |
   | `APP_SEED_USERNAME` | `yash.s` | Change from defaults before going public. |
   | `APP_SEED_PASSWORD` | (strong password) | **Required** — do not ship the default to production. |
   | `APP_SEED_DISPLAY_NAME` | `Lab Tech` | Optional. |

   `PORT` is injected by Railway automatically; `application.properties` binds
   `server.port=${PORT:...}`.

4. **Deploy**. Wait for the build to finish, then open the public domain
   Railway assigns and hit `/api/health` — it should return
   `{"status":"UP"}`.

### 3. Deploy the frontend

1. **+ New → GitHub Repo →** same repo, second service.
2. **Settings → Root Directory** = `frontend`. Railway will use
   `frontend/Dockerfile`.
3. **Variables** — note `VITE_API_BASE_URL` must be a **Build Variable**
   (Vite bakes it into the bundle at build time):

   | Variable | Value |
   |---|---|
   | `VITE_API_BASE_URL` | `https://<backend>.up.railway.app` (no trailing slash) |

   In Railway: **Variables → Add Variable → Build Variable**, key
   `VITE_API_BASE_URL`, value the backend's public URL. The `Dockerfile`
   exposes it via `ARG VITE_API_BASE_URL`.

4. **Deploy**. Open the assigned domain — the **Sign in** card should render.

### 4. Wire CORS once both URLs exist

After the frontend's domain is provisioned, go back to the **backend**
service's `APP_CORS_ALLOWED_ORIGINS` and set it to the frontend's exact
`https://…up.railway.app` origin. Trigger a redeploy of the backend so the
new value is picked up.

### Smoke test

```bash
# 1. Health
curl -i https://<backend>.up.railway.app/api/health
#   → 200 {"status":"UP"}

# 2. Login (capture the session cookie)
curl -i -c /tmp/jar.txt \
  -H 'Content-Type: application/json' \
  -d '{"username":"yash.s","password":"<APP_SEED_PASSWORD>"}' \
  https://<backend>.up.railway.app/api/auth/login
#   → 200 + Set-Cookie: JSESSIONID=...; Secure; SameSite=None

# 3. Authenticated call
curl -i -b /tmp/jar.txt https://<backend>.up.railway.app/api/reagents
#   → 200 + paginated reagents JSON
```

Then in a browser: open `https://<frontend>.up.railway.app`, sign in,
verify the dashboard tiles, create/edit/delete a reagent.

### Known limitations

- **Cross-origin session cookies.** The SPA on `<frontend>.up.railway.app`
  calls the API on `<backend>.up.railway.app`. The browser only sends the
  session cookie when the backend issues it as `SameSite=None; Secure` *and*
  the frontend origin is in the CORS allow-list — the prod profile and
  `APP_CORS_ALLOWED_ORIGINS` exist exactly for that. If sign-in returns 200
  but `/api/auth/me` returns 401 on the next request, the cookie was rejected
  — re-check those two variables.
- **Swing admin viewer is local only.** It points at `LAB_INVENTORY_API`
  (defaults to `http://localhost:8082`); you can set it to the Railway
  backend URL to use it against the deployed API, but the desktop app itself
  is not deployed.
## How to run the tests

```bash
# Backend (Spring Boot + MockMvc against H2)
cd backend && mvn test

# Frontend (Vitest + React Testing Library, mocked API)
cd frontend && npm test

# Swing admin (JUnit 5)
cd swing-admin && mvn test
```

CI runs all three on every push and pull request — see
[`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Demo script

1. Run `docker compose up -d`, then the backend, frontend, and (optionally) Swing viewer.
2. Open the web app at http://localhost:5175 — the **Sign in** card renders.
3. Sign in as `yash.s` / `labtech`. The Inventory view loads and the user chip
   in the top right shows `yash.s · Lab Tech`.
4. Note the **Total Reagents / Low Stock / Expired / In Stock** tiles.
5. Click the **Low Stock** tile — the table filters to low-stock rows.
6. Click **+ New reagent**, fill in a new entry (e.g. Sodium Hydroxide, 1000g, min 100), save.
7. Edit any reagent and drop its quantity below the minimum — its row flips to **Low stock**.
   Open its History tab — the new event's `actor` reads `yash.s`, not a hardcoded label.
8. Delete a row.
9. Open the Swing admin viewer (with `LAB_INVENTORY_USER` / `LAB_INVENTORY_PASSWORD`)
   and click **Refresh** — it shows the same data through HTTP Basic against the
   same filter chain. Try with a wrong password to see the auth-failed dialog.
10. Click the user chip → **Sign out**. The login card returns immediately.

## Interview pitch

> I built this because DeltaSoft works in scientific R&D software, and I wanted a
> project directly connected to reagent inventory and lab workflow management
> while using the role's stack: React, TypeScript, Java, PostgreSQL, REST APIs,
> and Swing.

## Screenshots

_Placeholder — capture and add screenshots of:_

- `docs/dashboard.png` — main inventory view with dashboard tiles and table
- `docs/drawer.png` — right-side drawer form for creating/editing a reagent
- `docs/swing.png` — Swing admin viewer with the table and status column rendering

## Project layout

```
lab-inventory-tracker/
  backend/                 Spring Boot REST API + tests
  frontend/                React + TypeScript + Vite + tests
  swing-admin/             Java Swing admin viewer + tests
  docker-compose.yml       PostgreSQL for local development
  .github/workflows/ci.yml CI for all three modules
  README.md
```
