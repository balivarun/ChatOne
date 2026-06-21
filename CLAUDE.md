# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project: ConnectChat

Real-time messaging app (Telegram-like). Monorepo with two sub-projects:
- `backend/` — Spring Boot 3.3.5, Java 17, Gradle (package `com.connectchat`)
- `frontend/` — Next.js 16 + React 19 + TypeScript + Tailwind CSS v4

## Commands

### Backend (`cd backend/`)
```bash
./gradlew bootRun                            # start dev server on :8080
./gradlew build                              # compile + run tests + package
./gradlew test                               # run all tests
./gradlew test --tests "com.connectchat.service.MessageServiceTest"  # single test
./gradlew bootJar                            # build fat JAR
```

### Frontend (`cd frontend/`)
```bash
npm run dev      # dev server on :3000
npm run build    # production build
npm run start    # serve production build
npm run lint     # ESLint
npx tsc --noEmit # type check without building
```

### Full stack
```bash
docker compose up postgres redis -d          # infra only (for local backend/frontend dev)
docker compose up --build                    # full stack via Docker
```

## Architecture

### Backend
- **Package root**: `com.connectchat`
- **Layers**: `entity → repository → service → controller`. Keep business logic in services, never in controllers.
- **Security**: JWT filter (`JwtAuthenticationFilter`) runs before every request. `UserPrincipal` is the `Authentication` principal — get current user ID via `principal.getId()` in controllers.
- **OAuth flow**: `GET /api/auth/google` → Google → `GET /api/auth/callback` → issues JWT → redirects to `${frontend-url}/auth/callback?token=…&refresh=…`.
- **WebSocket**: STOMP over SockJS at `/ws`. JWT validated in `ChannelInterceptor.preSend()` on CONNECT frame. Personal queue: `/user/queue/messages`. Group topic: `/topic/conversation/{id}`.
- **Database migrations**: Flyway, files in `src/main/resources/db/migration/V{n}__{desc}.sql`. Hibernate `ddl-auto: validate` — schema changes require a new migration file.
- **Redis**: Used for online presence (`online:{userId}` key with 5-min TTL) and future caching.
- **File uploads**: Cloudinary via `FileService`. Max 50 MB.

### Frontend
- **App Router** (`app/` directory). All routes use the `app/` convention.
- **Auth flow**: Login page → Google OAuth → `/auth/callback?token=` → `useAuthStore.setTokens()` → redirect to `/chat`.
- **State**: Zustand stores in `store/`. `authStore` (user + tokens), `chatStore` (conversations + messages + typing), `notificationStore`, `uiStore` (theme + sidebar).
- **API client**: `lib/api.ts` (axios instance with JWT interceptor and auto-refresh on 401).
- **WebSocket client**: `lib/websocket.ts` (STOMP over SockJS). Call `connectWebSocket(token)` after login, `disconnectWebSocket()` on logout.
- **Tailwind v4**: Uses `@import "tailwindcss"` in `globals.css`. Custom tokens defined with `@theme inline {}`. No `tailwind.config.js`.
- **Components**: UI primitives in `components/ui/` (Radix-based, ShadCN pattern). Feature components in `components/chat/`, `components/sidebar/`, `components/group/`, `components/common/`.

### Database (PostgreSQL)
Key tables: `users`, `conversations`, `conversation_participants`, `messages`, `message_reads`, `groups`, `group_members`, `attachments`, `notifications`, `blocked_users`.
- A `Conversation` is DIRECT or GROUP. GROUP conversations are linked to a `groups` row.
- `conversation_participants` holds per-user settings (archived, pinned, muted, last_read_message_id).
- Read receipts tracked via `message_reads` table.

## Android App (`android/`)

Open the `android/` folder directly in Android Studio (not the repo root).

**Tech stack:** Kotlin, Jetpack Compose, Material 3, Hilt (DI), Retrofit + OkHttp (HTTP), Room (local cache), DataStore (prefs/tokens), Coil (images), Navigation Compose, Paging 3, Google Sign-In SDK.

**Auth flow (different from web):** Android uses `GoogleSignInClient` → gets `idToken` → sends to `POST /api/auth/google/mobile` → receives JWT. No browser redirect involved.

**WebSocket:** Custom `StompClient` using OkHttp WebSocket connects to `/ws-native` (raw STOMP, no SockJS). Web frontend still uses `/ws` with SockJS.

**Configuration:** Copy `android/local.properties.example` → `android/local.properties`. Set `sdk.dir`, `BASE_URL=http://10.0.2.2:8080` (emulator → host), `WEB_CLIENT_ID` (Google web client ID).

**Package:** `com.connectchat` — same as backend, but fully separate project.

**Key design rules:**
- All network calls are `suspend` functions in repositories, called from `viewModelScope`
- `Result<T>` + `runCatching {}` for error handling — never throw from repositories
- Room is the single source of truth for conversations and messages; API calls update Room, UI reads from Room
- `StompClient` is a `@Singleton` — connect on login, disconnect on logout

## Environment
Copy `.env.example` to `.env`. Required: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET` (≥64 chars), `CLOUDINARY_*`. Infrastructure (Postgres/Redis) can be started with `docker compose up postgres redis -d`.

## Important Constraints
- Spring Boot was downgraded from `4.0.6` (scaffold) to `3.3.5` for API stability.
- Never run `git add -A` or commit `.env` (it's gitignored).
- Flyway validates schema on startup — always create a new migration file instead of editing existing ones.
- WebSocket subscriptions in React components must return an unsubscribe cleanup from `useEffect`.
- The `@Data` Lombok annotation is banned on JPA entities (causes infinite recursion in toString/hashCode). Use `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(of="id")` instead.
