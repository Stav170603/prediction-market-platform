# Smart Prediction Market

A full-stack prediction market platform inspired by Polymarket. Users receive a virtual balance, trade YES and NO outcome shares, follow prices in real time, review their portfolio and trade history, and receive automatic payouts when an administrator resolves a market.

This repository contains:

- A Java 21 and Spring Boot REST API
- A Next.js 14 and React dashboard
- MySQL persistence through Spring Data JPA
- JWT authentication and role-based administration
- Server-Sent Events (SSE) for live market and portfolio updates

> This project uses virtual points only. It does not process real money.

## Project Context

Collaborative Full Stack academic project developed as part of my B.Sc. studies.

I took a leading role in the development of the platform and worked extensively across both the frontend and backend. My work included Next.js and TypeScript development, REST API integration, BUY and SELL flows, wallet and portfolio features, price history, trade history, profile and leaderboard functionality.

## Main Features

- User registration and login with JWT authentication
- Virtual wallets initialized with 10,000 points
- Browse, search, filter, and inspect prediction markets
- Buy and sell YES or NO outcome shares
- Dynamic market pricing based on outstanding shares and liquidity
- Trade-driven price history charts
- Personal portfolio, wallet ledger, and trade history
- Public market statistics, dashboard summary, and leaderboard
- Administrator market creation and resolution
- Automatic winning-position settlement
- Real-time price, trade, portfolio, and resolution updates through SSE
- Responsive light and dark frontend themes

## Architecture

```text
+-----------------------------+
| Next.js / React frontend    |
| React Query + EventSource   |
+-------------+---------------+
              | REST / SSE
              v
+-----------------------------+
| Spring Boot backend         |
| Controllers -> Services     |
| Security -> JPA Repositories|
+-------------+---------------+
              | JDBC
              v
+-----------------------------+
| MySQL                       |
| Users, markets, outcomes,   |
| trades, positions, wallets, |
| history, resolutions        |
+-----------------------------+
```

The frontend lives in `polymarket-dashboard/`. The backend follows a conventional layered structure under `src/main/java/com/virtualmarket/polymarket`, with controllers handling HTTP requests, services enforcing business rules, and Spring Data repositories managing persistence.

## Technologies

| Area | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Security |
| Persistence | Spring Data JPA, Hibernate, MySQL |
| Authentication | JWT (`jjwt`), BCrypt |
| Frontend | Next.js 14, React 18, TypeScript |
| Data fetching | Axios, TanStack React Query |
| UI | Tailwind CSS, Lucide React |
| Charts | Recharts |
| Real time | Server-Sent Events |
| Build tools | Maven Wrapper, npm |

## Prerequisites

- **Java Development Kit 21**
- MySQL 8 or a compatible MySQL server
- Node.js 18.17 or newer
- npm

### Java 21 Requirement

Install JDK 21 and ensure both `JAVA_HOME` and `PATH` point to that installation.
The Maven build validates the Java version before compilation and uses the JDK
running Maven for main compilation, test compilation, tests, and the Spring Boot
process.

Verify the complete toolchain:

```bash
java -version
javac -version
./mvnw -version
```

On Windows, use `.\mvnw.cmd -version` for the last command. Every command must
report Java 21 or newer.

## Database Setup

1. Start MySQL.
2. Create the application database:

```sql
CREATE DATABASE polymarket_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

3. Configure the connection with environment variables:

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/polymarket_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | Value configured in `application.properties` |

PowerShell example:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/polymarket_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
```

Bash example:

```bash
export DB_URL="jdbc:mysql://localhost:3306/polymarket_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME="root"
export DB_PASSWORD="your-password"
```

Hibernate uses `spring.jpa.hibernate.ddl-auto=update`, so tables are created or updated when the backend starts. For shared or production environments, provide credentials through environment variables rather than committing them.

## Backend Setup

From the repository root:

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### macOS or Linux

```bash
./mvnw spring-boot:run
```

The API starts at [http://localhost:8080](http://localhost:8080).

On first startup, the application seeds a sample market and an administrator account when needed:

```text
Username: admin
Password: admin123
```

These credentials are intended for local demonstration only.

## Frontend Setup

Open a second terminal:

```bash
cd polymarket-dashboard
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

The frontend defaults to `http://localhost:8080`. To use another backend URL, create `polymarket-dashboard/.env.local`:

```dotenv
NEXT_PUBLIC_API_BASE=http://localhost:8080
```

Production build:

```bash
cd polymarket-dashboard
npm run build
npm start
```

## API Overview

Public endpoints can be called without a token. Protected endpoints expect:

```http
Authorization: Bearer <jwt-token>
```

| Method | Endpoint | Purpose | Access |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Register a user and create a wallet | Public |
| `POST` | `/api/auth/login` | Authenticate and receive a JWT | Public |
| `GET` | `/api/markets` | List markets | Public |
| `GET` | `/api/markets/{id}` | Get market details | Public |
| `GET` | `/api/markets/{id}/history` | Get ascending price history | Public |
| `GET` | `/api/markets/{id}/statistics` | Get market statistics | Public |
| `GET` | `/api/trades/by-market/{id}` | List trades for a market | Public |
| `GET` | `/api/dashboard/summary` | Get platform summary data | Public |
| `GET` | `/api/events/stream` | Subscribe to SSE events | Public |
| `POST` | `/api/trades` | Execute a BUY or SELL | Authenticated |
| `GET` | `/api/trades/me` | Get the current user's trades | Authenticated |
| `GET` | `/api/positions/me` | Get the current user's positions | Authenticated |
| `GET` | `/api/wallets/me` | Get the current user's wallet | Authenticated |
| `GET` | `/api/wallets/me/transactions` | Get wallet transactions | Authenticated |
| `GET` | `/api/leaderboard` | Get ranked user performance | Authenticated |
| `POST` | `/api/markets/admin/markets` | Create a market | Admin |
| `POST` | `/api/markets/resolve` | Resolve and settle a market | Admin |

## Real-Time Updates with SSE

The frontend maintains an `EventSource` connection to:

```http
GET /api/events/stream
```

The backend publishes events only after the related database transaction commits. Event types include:

- `trade-created`
- `market-price-updated`
- `user-portfolio-updated`
- `market-resolved`
- `ping` heartbeat events every 25 seconds

The frontend uses these events to invalidate the relevant React Query cache entries, refreshing market prices, history, trades, wallet data, and portfolio views without a full page reload.

## Pricing Engine

Every market begins with YES and NO prices of `0.5000`. After each successful BUY or SELL, the pricing service recalculates the market:

```text
YES price = (YES shares + liquidity)
            / (YES shares + NO shares + 2 x liquidity)

NO price = 1 - YES price
```

Prices are rounded to four decimal places and constrained to the range `0.0100` through `0.9900`. Liquidity dampens price movement: higher liquidity means a trade has less price impact.

Once recalculated, both outcome prices are persisted and a price-history record is created. The market detail chart reads these records in chronological order, so it represents executed trading activity rather than generated sample points.

## Settlement

Only an administrator can resolve a market, and resolution is allowed only after its configured resolution date.

1. The administrator selects the winning YES or NO outcome.
2. The backend verifies the market is open, the date has been reached, and the outcome belongs to that market.
3. Each winning share pays `1.00` virtual point.
4. Losing shares pay `0`.
5. Wallet balances and transaction ledgers are updated atomically.
6. The market is marked `RESOLVED`.
7. SSE events refresh affected market and portfolio screens.

A stored resolution record prevents the same market from being settled twice.

### Reliability Score

Each resolved market counts as one prediction for every user who holds a positive
position in that market. Holding the winning outcome counts as correct; holding
only losing outcomes counts as incorrect. Position size does not change the result.

```text
Reliability score = correct resolved predictions
                    / total resolved predictions x 100
```

The score is rounded to two decimal places and constrained to `0–100`. Reliability
updates run inside the settlement transaction after the duplicate-resolution check,
so a market cannot be counted twice. The leaderboard ranks reliability first and
uses portfolio value as the tie-breaker.

## Demo Flow

For a concise presentation:

1. Start MySQL, the Spring Boot backend, and the Next.js frontend.
2. Open the dashboard and show the seeded market, summary statistics, filtering, and theme toggle.
3. Register a participant account and point out the initial 10,000-point wallet.
4. Open a market and place a BUY on YES or NO.
5. Show the immediate price movement, new chart point, recent trade, wallet debit, and portfolio position.
6. Place a SELL to demonstrate balance credit and another live price update.
7. Open trade history, wallet transactions, portfolio, and leaderboard.
8. Log in as `admin`, open the admin area, and create a market.
9. For a market whose resolution date has passed, choose the winning outcome and resolve it.
10. Return to the participant account and show the payout and resolved market state.

## Screenshots

Add project screenshots here as the presentation assets are finalized.

### Dashboard

> Screenshot placeholder: dashboard overview and active markets

### Market Detail and Price History

> Screenshot placeholder: market trading panel and YES/NO history chart

### Portfolio and Wallet

> Screenshot placeholder: open positions, balance, and transaction ledger

### Admin Console

> Screenshot placeholder: market creation and resolution workflow

## Troubleshooting

### Java 21 Is Not Being Used

If Maven reports `release version 21 not supported`, an older JDK is active.
Install JDK 21, set both `JAVA_HOME` and `PATH` to that same installation, open
a new terminal, and verify Maven before starting the application:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, confirm with `./mvnw -version`, then run
`./mvnw spring-boot:run`. Maven fails during validation with
`Java 21 or newer is required` if the wrapper is running on an older Java
runtime.

### Port 8080 Is Already in Use

Stop the process using port 8080, or start Spring Boot on another port:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Then set `NEXT_PUBLIC_API_BASE=http://localhost:8081` in `polymarket-dashboard/.env.local` and restart the frontend.

### Port 3000 Is Already in Use

Run Next.js on another port:

```bash
cd polymarket-dashboard
npm run dev -- -p 3001
```

Then open [http://localhost:3001](http://localhost:3001).

### MySQL Connection Issues

- Confirm the MySQL service is running and listening on the expected port.
- Confirm `polymarket_db` exists.
- Check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Ensure the database user can create and alter tables in `polymarket_db`.
- Keep `allowPublicKeyRetrieval=true` when required by local MySQL authentication.
- Check the backend startup log for `Access denied`, `Unknown database`, or connection timeout details.

## Project Structure

```text
.
|-- pom.xml                         # Backend dependencies and Java version
|-- mvnw / mvnw.cmd                 # Maven Wrapper
|-- src/main/java/...               # Spring Boot application
|-- src/main/resources/
|   `-- application.properties      # Database and JPA configuration
`-- polymarket-dashboard/
    |-- package.json                # Frontend dependencies and scripts
    `-- src/
        |-- app/                    # Next.js routes
        |-- components/             # Reusable UI
        |-- contexts/               # Authentication context
        |-- hooks/                  # Query and SSE hooks
        |-- services/               # REST API clients
        `-- types/                  # API TypeScript types
```

## Verification

Run both project checks before presenting or deploying:

```powershell
.\mvnw.cmd clean test
cd polymarket-dashboard
npm.cmd run build
```
