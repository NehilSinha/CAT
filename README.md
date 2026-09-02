# Smart Rental Tracking System (SRTS) — Backend

Backend for a Caterpillar hackathon problem statement: track rented equipment in real time, log usage, alert on problems, forecast which equipment a client no longer needs, and detect misuse — with an AI assistant on top that explains data, never computes it.

## Core principle

```
DATA -> JAVA BUSINESS LOGIC -> CALCULATIONS/RULES -> ANALYTICS -> OPTIONAL AI EXPLANATION -> USER ACTION
```

Every number that matters — utilization, alerts, anomalies, right-sizing — is computed by plain deterministic Java (`EquipmentCalculationService`). The AI chatbot (`GroqChatService`) is only ever handed numbers Java has already calculated and asked to explain them in plain language. It cannot invent a number, and it never decides anything. This is the answer to give if anyone asks "how do you keep the AI honest."

## Tech stack

- **Java 21**, **Spring Boot 4.1.1**, embedded **Apache Tomcat**
- **MongoDB Atlas** via Spring Data MongoDB + the MongoDB Java driver
- **Maven**, **Lombok**, **Jackson** (note: Spring Boot 4 ships **Jackson 3.x**, package `tools.jackson.*`, not the classic `com.fasterxml.jackson.*`)
- **Groq API** for the chatbot only
- **Telegram Bot API** for push notifications
- Plain `java.net.http.HttpClient` for all outbound HTTP (Groq, Telegram, and the standalone tools calling this backend) — no extra HTTP library

## Project structure

```
com.SRTS.CAT
├── entity/       EquipmentEntry, EquipmentType, EquipmentStatus, Client
├── repo/         EquipmentRepo, ClientRepo (Spring Data MongoDB)
├── service/      EquipmentService, EquipmentCalculationService, AlertService,
│                 ClientService, ClientDashboardService, GroqChatService
├── controller/   REST endpoints (see API reference below)
├── dto/          Request/response shapes — never expose entities directly
├── exception/    GlobalExceptionHandler (404 / 400 / 502 mapping)
├── config/       WebConfig (CORS)
├── util/         EnvLoader (.env file support)
├── simulator/    TelemetrySimulator — standalone, NOT a Spring bean
├── seed/         DataSeeder — standalone, NOT a Spring bean
└── notifier/     TelegramNotifier — standalone, NOT a Spring bean
```

The three `simulator` / `seed` / `notifier` classes are plain Java with their own `main()`. They are never started by the Spring app — each is its own separate process you run yourself, and each talks to MongoDB directly (via the driver) or to this backend's own REST API (never duplicating business logic).

## Setup

Create a `.env` file in the project root (already gitignored — never commit it):

```
MONGODB_URI=mongodb+srv://<user>:<pass>@<cluster>/CAT?retryWrites=true&w=majority
GROQ_API_KEY=gsk_...
TELEGRAM_BOT_TOKEN=123456:ABC...
```

`EnvLoader` (in `util/`) loads these automatically for every entry point below — a real OS/IDE environment variable always wins if set, `.env` is just the fallback. No per-run-configuration setup needed. `.env.example` documents the required keys with no values, safe to commit.

## Running it — 4 separate processes

| # | What | How | Needs |
|---|---|---|---|
| 1 | **Backend** | Run `CatApplication` (IntelliJ ▶ or `mvn spring-boot:run`) | `MONGODB_URI`, `GROQ_API_KEY` |
| 2 | **Frontend** | `npm run dev` in `Frontend CAT/my-cat` (separate repo) | Backend running on `:8080` |
| 3 | **Telemetry simulator** | Run `TelemetrySimulator.main()` | `MONGODB_URI` |
| 4 | **Telegram notifier** | Run `TelegramNotifier.main()` | `TELEGRAM_BOT_TOKEN`, `MONGODB_URI`, backend running |

Start the backend first — everything else calls it or shares its database. **Run only one `TelegramNotifier` instance at a time** — Telegram's `getUpdates` API doesn't support multiple concurrent consumers for one bot token; running two splits/loses messages unpredictably.

## Data model

**`EquipmentEntry`** (Mongo collection `equipment`):

| Field | Meaning |
|---|---|
| `equipmentCode`, `equipmentName`, `type`, `status` | Identity. `type`: EXCAVATOR / CRANE / BULLDOZER / GRADER / DUMP_TRUCK / WHEEL_LOADER / COMPACTOR / FORKLIFT. `status`: AVAILABLE / RENTED / MAINTENANCE (IDLE, OVERDUE exist on the enum but aren't used as real transitions) |
| `siteId`, `currentLocation`, `lastOperatorId`, `clientId` | Where it is, which operator (by `Operator.id`) is assigned, who rented it (`clientId` is `null` unless `RENTED`) |
| `checkOutDate`, `checkInDate`, `expectedReturnDate` | Rental dates |
| `engineHoursPerDay`, `idleHoursPerDay`, `operatingDays` | Usage counters |
| `engineTemperature`, `fuelLevel`, `seatbeltEngaged`, `activeState` | Live sensor telemetry |
| `engineHoursHistory`, `idleHoursHistory`, `fuelLevelHistory` | One entry appended per recorded reading (index 0 = first reading) — feeds averages and the anomaly rule |

**`Client`** (collection `clients`): `id` is a randomly generated 8-character code (excludes `O`/`0`/`I`/`1`) — that code **is** the client's login, no password. `companyName`, `createdDate`.

**`Operator`** (collection `operators`): `id`, `name`, `clientId`. A client creates their own operators and assigns them to any of their currently-rented machines from the dashboard table — pure assignment (who's driving which machine), no login or credentials of their own. Intentionally minimal, no separate operator management beyond add + assign.

## The calculation rules (all in `EquipmentCalculationService`)

| Rule | Logic |
|---|---|
| `isOverdue` | `RENTED` and `expectedReturnDate` is in the past |
| `isUpcomingReturn` | `RENTED` and return date within 2 days |
| `isUnderutilized` | `RENTED` and utilization < 30% |
| `isOverheating` | engine temperature > 100°C |
| `isLowFuel` | fuel level < 15% |
| `isSeatbeltViolation` | `RENTED` and seatbelt not engaged |
| `isLikelyUnused` (demand forecasting) | `RENTED` and (fuel ≥ 95% untouched **or** underutilized) — predicts, from actual historical usage trend rather than a guess, which currently-rented machines the client no longer needs |
| `isIdleHoursAnomaly` | **True historical anomaly detection**: today's idle hours vs. *this machine's own* average of prior readings (needs ≥3 readings), flagged if > 1.5× baseline — not a fixed global number |
| `isUnassignedUse` | `RENTED`, engine actively running, seatbelt **not** engaged — proxy for "unassigned/unauthorized operation" since there's no real operator-login system to check against |

`utilization = engineHoursPerDay / (engineHoursPerDay + idleHoursPerDay)`.

## API reference

**Equipment** — `/api/equipment` (unscoped — internal/retail use, sees the whole fleet)
```
GET    /api/equipment                       list all
GET    /api/equipment/{id}                  one machine
POST   /api/equipment                       create
PATCH  /api/equipment/{id}/checkout         { clientId, location, expectedReturnDate, siteId?, operatorId? } — clientId + expectedReturnDate required
PATCH  /api/equipment/checkout-batch        { equipmentIds[], clientId, location, expectedReturnDate } — partial success, per-id failure reasons
PATCH  /api/equipment/{id}/checkin          resets location to "CAT Yard", stamps checkInDate
PATCH  /api/equipment/{id}/maintenance/start  only from AVAILABLE — resets sensors to a clean baseline (clears stale alert flags)
PATCH  /api/equipment/{id}/maintenance/end    only from MAINTENANCE -> AVAILABLE
PATCH  /api/equipment/{id}/assign-operator  { operatorId } — only valid while RENTED; sets lastOperatorId
PATCH  /api/equipment/{id}/usage            { engineHoursPerDay?, idleHoursPerDay?, operatingDays? }
PATCH  /api/equipment/{id}/telemetry        { engineTemperature?, fuelLevel?, seatbeltEngaged? }
GET    /api/equipment/{id}/history          one machine's history + averages
GET    /api/equipment/history               fleet-wide history + averages, one entry per machine
```

**Alerts** — `/api/alerts`
```
GET /api/alerts    only machines with at least one flag active (fleet-wide)
```

**Clients** — `/api/clients`
```
POST /api/clients          { companyName } -> generates the access code
GET  /api/clients          list all
GET  /api/clients/{id}     look up one (404 if code is invalid)
```

**Client dashboard** — `/api/clients/{clientId}` (scoped to that client only — this is the boundary that must never leak to other clients)
```
GET /api/clients/{clientId}/equipment        their rented machines + computed flags + history + averages
GET /api/clients/{clientId}/fleet-summary    demand forecast: { totalRented, activelyUsed, underutilizedCount, underutilizedEquipment[] }
```

**Operators** — `/api/clients/{clientId}/operators` (scoped to that client)
```
POST /api/clients/{clientId}/operators    { name } -> creates an operator for this client
GET  /api/clients/{clientId}/operators    list this client's operators
```
Assigning one to a machine is a separate call against the unscoped equipment resource: `PATCH /api/equipment/{id}/assign-operator` (see above).

**Chat** — `/api/clients/{clientId}/chat`
```
POST { message } -> { reply }    grounded in that client's own data only, via GroqChatService
```
Because the chat is grounded in the same `fleet-summary`/`isLikelyUnused` data as the dashboard, it can turn the forecast into a direct, actionable answer — e.g. asked "how can I save money," it can say *"you're paying for 4 machines you're barely using — returning them cuts your rental cost."* That reasoning is still Java's (the underused machines and the numbers are computed server-side); the model's only job is phrasing it.

⚠️ Anything under `/api/equipment/*` is fleet-wide/unscoped — never point a client-facing UI or the chatbot at it, only at `/api/clients/{clientId}/*`.

## Standalone tools in detail

**`TelemetrySimulator`** — generates realistic live data for `RENTED` equipment every tick (default 60s):
- Momentum: ~75% chance to stay in the same active/idle state as last tick (not a fresh coin flip every time)
- Temperature rises while active, cools while idle
- Fuel burns faster while active than idle
- Seatbelt: ~90% engaged per tick
- `operatingDays` increments by 1 every tick regardless of active/idle (one simulated day passing) — this reaches the AI chat context too, so it can answer questions like "how many operating days does this machine have"
- Pushes into the `*History` arrays every tick

**`DataSeeder`** — one-shot, inserts sample `AVAILABLE` machines and exits. Generates them programmatically: `MACHINES_PER_TYPE` (default 7) × all 8 types in `EquipmentType` = 56 machines, each type keeping its own numeric code block (Excavators 101-107, Cranes 201-207, ... Forklifts 801-807), spread across 3 yard sites. Adjust `MACHINES_PER_TYPE` or the `TYPES` array to change the count or add categories.

**`TelegramNotifier`** — a client messages the bot their access code once (validated against `/api/clients/{id}`) to link their Telegram chat; every 30s it checks each registered client's `/api/clients/{id}/equipment` and sends one message per *newly* triggered flag (not a repeat every tick). Registration handling runs on a fast long-poll loop (near-instant `/start` reply); alert-checking runs on its own slower timer.

## Security notes

- All secrets load from `.env` (gitignored) via `EnvLoader` — nothing is hardcoded in source anymore
- The Mongo password, Groq key, and Telegram bot token currently in use were shared during development chat sessions — rotate them before this repo or demo goes fully public
- No authentication at all on the retail side (`/api/equipment/*`, `/api/clients` POST/GET) — anyone with network access can call it. Client access is gated only by knowing the 8-character code (not a password, just an unguessable-ish string) — fine for a hackathon demo, not production-grade
