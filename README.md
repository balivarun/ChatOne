# ConnectChat

A real-time messaging application similar to Telegram, built with Spring Boot and Next.js.

## Features

- Google OAuth2 sign-in (no phone number required)
- One-to-one and group messaging in real time (WebSocket/STOMP)
- Online/offline status indicators and typing indicators
- Read receipts (sent → delivered → seen)
- Message editing, deletion, replies, and forwarding
- File, image, and voice message sharing via Cloudinary
- Group creation with admin roles, add/remove members
- Infinite scroll for message history
- Browser notifications for new messages
- Dark/light mode
- Chat archive, pin, and mute
- User blocking
- Message and group search

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3.5, Java 17, Spring Security, Spring Data JPA |
| Database | PostgreSQL 16 |
| Cache / Presence | Redis 7 |
| Real-time | WebSocket + STOMP (SockJS) |
| File Storage | Cloudinary |
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS v4 |
| State | Zustand |
| UI | Radix UI (ShadCN-style) |
| Auth | Google OAuth2 + JWT |

## Project Structure

```
one-on-one/
├── backend/                  # Spring Boot application
│   ├── src/main/java/com/connectchat/
│   │   ├── config/           # Security, WebSocket, Redis, Cloudinary
│   │   ├── controller/       # REST controllers
│   │   ├── dto/              # Request/response DTOs
│   │   ├── entity/           # JPA entities
│   │   ├── exception/        # Global exception handling
│   │   ├── repository/       # Spring Data JPA repos
│   │   ├── security/         # JWT, OAuth2, UserPrincipal
│   │   ├── service/          # Business logic
│   │   └── websocket/        # STOMP chat controller
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/     # Flyway SQL migrations
│   └── Dockerfile
├── frontend/                 # Next.js application
│   ├── app/                  # App Router pages
│   ├── components/           # UI components
│   ├── lib/                  # API client, WebSocket client, utils
│   ├── store/                # Zustand stores
│   ├── types/                # TypeScript types
│   └── Dockerfile
├── docker-compose.yml
├── .env.example
└── .github/workflows/ci.yml
```

## Database Schema (ERD)

```
users ──< conversation_participants >── conversations ──< messages
  │                                          │                │
  │                                        groups          message_reads
  │                                          │
  └──< group_members >──────────────────── groups
  │
  └──< blocked_users
  │
  └──< notifications
```

Key relationships:
- A `Conversation` is either `DIRECT` (2 users) or `GROUP` (linked to a `groups` row)
- `conversation_participants` stores per-user settings (archived, pinned, muted, last read)
- `message_reads` tracks which users have read each message (for read receipts)
- `group_members` stores per-user role (ADMIN or MEMBER)

## Clients

| Client | Directory | Entry point |
|---|---|---|
| Web (Next.js 16) | `frontend/` | `npm run dev` → http://localhost:3000 |
| Android (Kotlin + Compose) | `android/` | Open in Android Studio, run on emulator |

---

## Prerequisites

- Java 17+
- Node.js 20+
- Docker and Docker Compose (for infrastructure)
- A Google Cloud project with OAuth2 credentials
- A Cloudinary account (free tier is sufficient)

## Local Development Setup

### 1. Clone and configure environment

```bash
git clone <repo-url>
cd one-on-one
cp .env.example .env
# Edit .env and fill in all required values (see below)
```

### 2. Start infrastructure (PostgreSQL + Redis)

```bash
docker compose up postgres redis -d
```

### 3. Configure Google OAuth2

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Create an OAuth 2.0 Client ID (Web application)
3. Add authorized redirect URI: `http://localhost:8080/api/auth/callback`
4. Copy client ID and secret into `.env`

### 4. Configure Cloudinary

1. Sign up at [cloudinary.com](https://cloudinary.com) (free tier: 25GB)
2. Copy cloud name, API key, and API secret into `.env`

### 5. Start the backend

```bash
cd backend
./gradlew bootRun
# Flyway will auto-create the schema on first run
# Backend available at http://localhost:8080
```

### 6. Start the frontend

```bash
cd frontend
npm install
npm run dev
# Frontend available at http://localhost:3000
```

## Running with Docker Compose (full stack)

```bash
cp .env.example .env
# Fill in .env values

docker compose up --build
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080
```

## Required Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `REDIS_HOST` | Redis host |
| `REDIS_PASSWORD` | Redis password (blank for no auth) |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |
| `JWT_SECRET` | ≥64-char random string for JWT signing |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret |
| `FRONTEND_URL` | Frontend origin (for CORS and OAuth redirect) |

## API Overview

| Prefix | Description |
|---|---|
| `GET /api/auth/google` | Initiate Google OAuth flow |
| `GET /api/auth/callback` | OAuth callback — issues JWT |
| `POST /api/auth/refresh` | Refresh access token |
| `GET /api/users/me` | Current user profile |
| `PUT /api/users/me` | Update display name / bio |
| `POST /api/users/me/avatar` | Upload profile picture |
| `GET /api/users/search?query=` | Search users by name/email |
| `GET /api/conversations` | All DM conversations |
| `POST /api/conversations` | Get or create DM |
| `GET /api/conversations/:id/messages` | Paginated messages |
| `POST /api/messages` | Send a message |
| `PUT /api/messages/:id` | Edit a message |
| `DELETE /api/messages/:id` | Delete a message |
| `POST /api/groups` | Create a group |
| `GET /api/groups/mine` | Groups I belong to |
| `GET /api/groups/:id/messages` | Paginated group messages |
| `POST /api/files/upload` | Upload file to Cloudinary |
| `GET /api/notifications` | My notifications |

Full API docs available at `http://localhost:8080/swagger-ui.html` (if Springdoc OpenAPI is added).

## WebSocket Events (STOMP)

Connect to `/ws` with header `Authorization: Bearer <token>`.

| Destination | Direction | Description |
|---|---|---|
| `/user/queue/messages` | subscribe | New DM messages |
| `/user/queue/notifications` | subscribe | New notifications |
| `/user/queue/typing` | subscribe | Typing indicators |
| `/topic/conversation/:id` | subscribe | Group conversation messages |
| `/app/chat.message` | send | Send a message |
| `/app/chat.typing` | send | Broadcast typing status |
| `/app/chat.read` | send | Mark messages as read |

## Deployment

### Railway

1. Create a new project → deploy from GitHub
2. Add PostgreSQL and Redis services
3. Set all environment variables in the Railway dashboard
4. Deploy backend and frontend as separate services

### Render

1. Create a Web Service for the backend (Docker, port 8080)
2. Create a Web Service for the frontend (Docker, port 3000)
3. Add a PostgreSQL and Redis instance
4. Set environment variables

### AWS (ECS + RDS + ElastiCache)

1. Push Docker images to ECR
2. Create RDS PostgreSQL and ElastiCache Redis instances
3. Create ECS task definitions pointing to ECR images
4. Set up Application Load Balancer → ECS services
5. Configure environment variables via ECS task definition or Secrets Manager

### Required GitHub Actions Secrets (for CD)

| Secret | Description |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub access token |

## Android Setup

### Prerequisites
- Android Studio Hedgehog (2023.1+) or newer
- Android SDK 35 (API 35)
- JDK 17

### Configure
1. Copy `android/local.properties.example` → `android/local.properties`
2. Set your `sdk.dir` path
3. Fill in:
   - `BASE_URL=http://10.0.2.2:8080` (emulator reaches your host machine at 10.0.2.2)  
   - `WEB_CLIENT_ID=` — your Google OAuth **web** application client ID (not Android client ID)

### Google Sign-In Setup (Android)
1. In Google Cloud Console → Credentials, create an **Android** OAuth client:
   - Package name: `com.connectchat`
   - SHA-1 signing certificate: run `./gradlew signingReport` to get debug SHA-1
2. The **Web** client ID (already created for the web app) is what goes in `WEB_CLIENT_ID` — Android sign-in sends the ID token to your backend which was registered as a web client

### Run
Open `android/` folder in Android Studio → let Gradle sync → Run on emulator or device.

The emulator uses `http://10.0.2.2:8080` to reach your local backend. Start the backend first:
```bash
cd backend && ./gradlew bootRun
```

---

## Development Notes

- Flyway migrations live in `backend/src/main/resources/db/migration/`. New migrations must follow `V{n}__{description}.sql` naming.
- The backend uses Spring Boot 3.3.5. The `build.gradle` in the repo scaffold showed `4.0.6` but was downgraded for stability.
- Tailwind CSS v4 uses `@import "tailwindcss"` (not `@tailwind` directives). Theme tokens go in `@theme inline {}` blocks inside CSS.
- WebSocket auth: JWT is passed in the STOMP CONNECT frame header and validated by a `ChannelInterceptor`.
