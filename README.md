# CoffeeLab

A small scheduling app for coffee chats, built for the TeamLift Internship Challenge. The host publishes open time slots, visitors book one with their name and email, and the app makes sure no slot can be booked twice. After booking, the visitor gets a short confirmation message, optionally written by Gemini.

**Live:** https://coffee-chat-ruddy-rho.vercel.app/#

## Features

- **Booking page** (`/`): open slots grouped by day, with date, time and length. Pick one, enter name, email and an optional topic, and get a confirmation ticket. The booked slot disappears from the list.
- **Host page** (`/#host`): publish slots (date, start time, 15/30/45/60 min), see upcoming bookings with guest details, and remove open slots. **This page is intentionally unauthenticated for the demo**, since auth is outside the challenge scope.
- **No double bookings**, enforced on the server and in the database (see below).
- **Optional AI confirmation** via Gemini, with a predefined fallback so bookings never depend on it.
- Loading, empty, error and success states; client-side validation backed by server-side validation; keyboard-accessible controls with visible focus.

## Tech stack

| Part     | Tech                                                                    |
| -------- | ----------------------------------------------------------------------- |
| Frontend | React 19, TypeScript, Vite, plain CSS                                   |
| Backend  | Java 21, Spring Boot 3.5 (Web, Data JPA, Validation, Actuator), Flyway  |
| Database | PostgreSQL 16                                                           |
| AI       | Gemini API (`gemini-3.5-flash-lite`), optional                          |
| Infra    | Docker, Docker Compose, Nginx (local); Vercel + Neon (production)       |

## Architecture

```
backend/src/main/java/com/dulo/coffeechat/
├── model/        JPA entities: TimeSlot, Booking
├── dto/          Request/response records; entities are never returned from the API
├── repository/   Spring Data JPA repositories (incl. the locking query)
├── service/      Business rules and transactions: TimeSlotService, BookingService, ConfirmationService
├── web/          Thin REST controllers and the global error handler
└── config/       Typed app properties, CORS, Clock

frontend/src/
├── pages/        BookingPage, HostPage
├── components/   Header, BookingForm, Confirmation
├── services/     API client (configurable base URL)
├── hooks/        useApi (load/reload/error), useHashRoute
├── types/        Shared TypeScript types
├── utils/        Date formatting and grouping
└── styles/       Global CSS with design tokens
```

### Preventing double bookings

1. `BookingService.book` runs in one transaction and loads the slot with `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`). A second request for the same slot waits until the first commits, then sees `booked = true` and gets `409 Conflict`.
2. `booking.time_slot_id` is `UNIQUE`, so even if the lock were bypassed, the database rejects a second booking. The error handler maps that to `409` too.
3. If anything fails, the transaction rolls back, so a slot is never marked booked without a booking row.

The AI confirmation is generated **after** the transaction commits, so a slow Gemini call never holds the row lock.

### Time zone

All times are wall-clock times in **Europe/Skopje**. They are stored as `TIMESTAMP` (no zone), compared against a `Clock` in that zone (`APP_TIME_ZONE`), sent as ISO strings without an offset (`2026-11-10T10:00:00`), and displayed as-is by the frontend, labelled "Skopje time".

### Routing

Two views use hash routing (`#host`) instead of React Router: no extra dependency, and static hosting needs no rewrite rules.

## Running with Docker Compose

Prerequisites: Docker with Compose v2.

```bash
cp .env.example .env        # optional: set a password and a Gemini key
docker compose up --build
```

- App: http://localhost:3000 (host page: http://localhost:3000/#host)
- API: http://localhost:8080/api/slots
- Health: http://localhost:8080/actuator/health

Nginx serves the frontend and proxies `/api` to the backend, so the browser never needs to resolve Docker service names. The backend waits for the database health check, and Flyway creates the schema on startup.

```bash
docker compose logs -f backend   # follow logs (or frontend, db)
docker compose down              # stop, keep data
docker compose down -v           # stop and delete the database volume
```

## Running for development

Prerequisites: Java 21, Maven, Node 20.19+ (or 22.12+), Docker for Postgres.

```bash
docker compose up -d db

cd backend
mvn spring-boot:run          # :8080, reads ../.env if present

cd frontend
npm install
npm run dev                  # :5173, proxies /api to :8080
```

## API

| Method   | Path              | Description                                   | Success | Errors          |
| -------- | ----------------- | --------------------------------------------- | ------- | --------------- |
| `GET`    | `/api/slots`      | Available future slots, ordered by start time | 200     |                 |
| `GET`    | `/api/slots/{id}` | One slot                                      | 200     | 404             |
| `POST`   | `/api/slots`      | Create a slot (host)                          | 201     | 400, 409 overlap |
| `DELETE` | `/api/slots/{id}` | Remove an open slot (host)                    | 204     | 404, 409 booked |
| `GET`    | `/api/bookings`   | Upcoming bookings with guest details (host)   | 200     |                 |
| `POST`   | `/api/bookings`   | Book a slot                                   | 201     | 400, 404, 409   |

```bash
curl -X POST localhost:8080/api/slots -H 'Content-Type: application/json' \
  -d '{"startTime": "2026-11-10T10:00:00", "durationMinutes": 30}'

curl -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' \
  -d '{"slotId": 1, "name": "Alex Morgan", "email": "alex@example.com"}'
```

Rules: slots start in the future, last 10 to 240 minutes and don't overlap; bookings need a non-blank name, a valid email and a future, unbooked slot. `topic` (max 280 chars) is optional.

Errors use Spring's RFC 7807 `ProblemDetail` format:

```json
{ "type": "about:blank", "title": "Conflict", "status": 409, "detail": "Someone just booked this time. Please pick another one.", "instance": "/api/bookings" }
```

`GET /api/slots` returns no guest data. Names and emails are only on `GET /api/bookings`.

## Database

PostgreSQL with a schema managed by Flyway (`backend/src/main/resources/db/migration`). Hibernate only validates it (`ddl-auto=validate`). Compose stores data in the `postgres-data` named volume, so bookings survive restarts and rebuilds.

- `time_slot`: `start_time`, `duration_minutes` (check 10 to 240), `booked`, `created_at`
- `booking`: `time_slot_id` (unique foreign key), `name`, `email`, `topic`, `created_at`

## Configuration

| Variable                     | Default                                         | Used by          |
| ---------------------------- | ----------------------------------------------- | ---------------- |
| `POSTGRES_DB`                | `coffeelab`                                     | db, backend      |
| `POSTGRES_USER`              | `coffeelab`                                     | db, backend      |
| `POSTGRES_PASSWORD`          | `coffeelab` (local only)                        | db, backend      |
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://localhost:5432/coffeelab`    | backend          |
| `SPRING_DATASOURCE_USERNAME` | value of `POSTGRES_USER`                        | backend          |
| `SPRING_DATASOURCE_PASSWORD` | value of `POSTGRES_PASSWORD`                    | backend          |
| `CORS_ORIGINS`               | `http://localhost:5173,http://localhost:3000`   | backend          |
| `APP_TIME_ZONE`              | `Europe/Skopje`                                 | backend          |
| `HOST_NAME`                  | `Dulo`                                          | backend (AI)     |
| `GEMINI_API_KEY`             | empty: fallback message is used                 | backend          |
| `GEMINI_MODEL`               | `gemini-3.5-flash-lite`                         | backend          |
| `VITE_API_URL`               | empty: same origin                              | frontend (build) |

`.env` is gitignored and excluded from Docker build contexts. Only `.env.example` with placeholders is committed.

## Optional AI confirmation

After a booking commits, `ConfirmationService` calls the Gemini Interactions API (`POST https://generativelanguage.googleapis.com/v1beta/interactions`, model `gemini-3.5-flash-lite`, `store: false` so Google doesn't keep the interaction) and asks for a 2 to 3 sentence confirmation that mentions the guest's first name, the time and the topic. The text comes back in the booking response's `message` field and is shown on the confirmation ticket.

- **Setup:** create a free key at [Google AI Studio](https://aistudio.google.com/apikey) and set `GEMINI_API_KEY` in `.env` locally, or as a Sensitive environment variable in the Vercel project. Never put it in the frontend or a `VITE_` variable: Vite embeds those in the public JavaScript bundle. Don't enable billing; without it the key stays on the free tier.
- **Free-tier limits change**, so check [pricing](https://ai.google.dev/gemini-api/docs/pricing) and [rate limits](https://ai.google.dev/gemini-api/docs/rate-limits) before relying on it. When the limit is hit, Gemini returns `429` and the app falls back.
- **Never required:** with no key, a timeout (3 s connect, 6 s read), an error, or an empty or overlong reply, the API returns a predefined message. The booking is already committed before Gemini is called, and any failure while building the message falls back too, so the booking response is always `201`.
- **Backend only:** the key is sent in the `x-goog-api-key` header and never reaches the browser.
- **Form input is data:** name and topic are sanitized, the system prompt says to ignore instructions inside them, and the reply is stripped of markdown and capped at 400 characters.
- Google may use free-tier prompts to improve its products, so this suits a demo, not real customer data.

## Tests

```bash
cd backend
mvn test
```

Tests run against in-memory H2 (PostgreSQL mode) with Flyway off, so they need no Docker:

- `SchedulingApiTest` (MockMvc): valid and invalid slots, overlap `409`, available-slot listing and order, valid and invalid bookings, duplicate booking `409`, unknown slot `404`, booked slot no longer listed.
- `BookingConcurrencyTest`: 8 threads book the same slot at once; exactly one succeeds and seven get a conflict.
- `ConfirmationServiceTest`: against a fake Gemini server (JDK `HttpServer`), checks the request (path, API-key header, model, `store: false`), returns the model's text, strips markdown, and falls back on `500`, `429`, an empty reply, an unreachable server and a missing key.
- `GeminiBookingFlowTest`: full `POST /api/bookings` with the fake server: the Gemini text is returned in `message`; when Gemini returns `503` the booking still returns `201` with the fallback and the slot is no longer available.

Frontend: `cd frontend && npm run build` type-checks and builds.

## Deployment

Everything runs in **one Vercel project** using [Vercel Services](https://vercel.com/docs/services) (`vercel.json`):

| Path           | Service    | How it's built                                          |
| -------------- | ---------- | ------------------------------------------------------- |
| `/api/*`, `/actuator/*` | `backend`  | `backend/Dockerfile` as a container (tests run during the build) |
| everything else | `frontend` | Vite static build                                      |
| database       | Neon       | added from the Vercel Marketplace                       |

Frontend and API share one domain, so the frontend calls `/api` on the same origin: no `VITE_API_URL` and no CORS setup is needed.

1. **Push to GitHub**: create an empty repository, then `git remote add origin https://github.com/<you>/coffee-chat.git` and `git push -u origin main`.
2. **Import into Vercel**: Add New → Project → pick the repo, leave Root Directory as the repo root (Vercel reads `vercel.json`).
3. **Database**: project → Storage → Create Database → Neon (free plan) → connect it to the project. Neon injects `PGHOST_UNPOOLED`, `PGDATABASE`, `PGUSER` and `PGPASSWORD`, which `application.properties` reads directly.
4. **Environment variables** (project → Settings → Environment Variables):
   - `PORT=8080` (container services receive traffic on `PORT`, default `80`)
   - `GEMINI_API_KEY` as a **Sensitive** variable (optional)
5. **Redeploy** so the new variables apply. Flyway creates the tables on the first backend start. Check `https://<project>.vercel.app/actuator/health` returns `{"status":"UP"}`.

### Redeploying

Vercel watches the GitHub repo: every push to `main` builds and deploys to production, and every other branch or pull request gets its own preview URL. A failing backend test fails the build, so broken code never replaces the running version. Flyway applies new `V2__...` migrations on the next backend start; never edit a migration that has already run.

The backend scales to zero after about 5 minutes without traffic, so the first request after a quiet period waits for Spring Boot to start; the booking page shows a "waking up" message meanwhile. Data lives in Neon, so bookings survive redeploys.

## Known limitations and improvements

- **Host auth:** host endpoints are open. A host password or magic link would be next.
- **Email confirmations** with an `.ics` invite, and **cancellation links** for guests.
- **Google Calendar sync** to hide slots that clash with the host's calendar.
- **Recurring availability** instead of adding slots one by one.
- **Tests against real Postgres** with Testcontainers, plus an end-to-end browser test of the booking flow.

## AI tools used

ChatGPT: Used for guidance on project architecture, deployment planning, Gemini API configuration, troubleshooting, and testing strategies.

Claude Code: Used for AI-assisted coding, implementation, debugging, and project development where applicable.

Google Gemini API: Integrated to generate AI-powered coffee chat booking confirmation messages, with a fallback confirmation when the API is unavailable.
