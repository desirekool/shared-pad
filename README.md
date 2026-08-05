# SyncDocs — Real-Time Collaborative Document Editor

A full-stack, real-time collaborative document editor with a Monaco-based editor, live multi-user sync over WebSocket/STOMP, an Apache Kafka event bus with persistent event replay, and a desktop Electron client.

## Features

- **Real-time collaboration** — multiple users edit the same document simultaneously; edits propagate over WebSocket → STOMP → Spring → Kafka → broadcast.
- **Monaco Editor** — full-featured code/text editor with a collapsible toolbar.
- **Live presence** — see who is viewing/editing a document, with connection indicators.
- **Version history** — version-based conflict resolution (CRDT planned for V3), offline edit queue with TTL, and missed-event replay on reconnect.
- **Permissions** — OWNER / EDITOR / VIEWER roles, share dialog with user search, revoke access (WebSocket permission notifications).
- **Local documents** — create, save, list, and promote local (browser) documents to the server.
- **Desktop app** — Electron client with native file open/import, window-title sync, and offline queue.
- **Authentication & audit** — JWT auth with registration, audit log, centralized error handling.
- **Persistence** — Kafka events stored in a persistent event store (`StoredEvent`), replayable via `GET /api/documents/{id}/events?after={eventId}`.

## Tech Stack

**Frontend** — React 19 · TypeScript 6 · Vite 7 · TanStack Router v1 · TanStack Query v5 · Monaco Editor · STOMP + SockJS · Electron 33 · Tailwind CSS 4

**Backend** — Spring Boot 4.1 · Java 21 · Spring Web / WebSocket / Security (JWT) / Data JPA / Validation / Actuator · Spring Kafka 4.1 (KRaft mode) · MinIO (content-addressable document storage) · PostgreSQL (metadata only) · Lombok

**Infrastructure** — Docker Compose: PostgreSQL 16 · Apache Kafka 4.0 · MinIO · backend

**Testing** — 166 backend tests (unit + integration), 0 failures / 0 flaky.

## Architecture

```
┌────────────┐  STOMP/WebSocket   ┌──────────────┐  Kafka   ┌───────────────┐
│  React app │ ◄───────────────► │ Spring Boot  │ ──────► │ Kafka (KRaft) │
│  + Electron│   (raw WS + SockJS)│   backend    │ ◄────── │  event bus    │
└────────────┘                    └──────┬───────┘         └───────────────┘
                                         │
                                ┌────────┴────────┐
                          ┌─────┴─────┐     ┌─────┴──────┐
                          │ PostgreSQL │     │  MinIO     │
                          │ (metadata) │     │ (documents)│
                          └───────────┘     └────────────┘
```

- Backend on `8080`; frontend dev server on `5173`; WebSocket at `ws://localhost:8080/ws`.
- Kafka runs in **KRaft** mode (no Zookeeper).
- PostgreSQL stores metadata only; document content is content-addressed in MinIO.

## Quick Start

### Prerequisites

- Java 21+
- Node.js 20+
- Maven
- Docker Desktop

### Run everything with Docker Compose

```bash
docker compose up --build
```

This starts PostgreSQL, Kafka, MinIO, and the backend. Then start the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

### Run backend locally (dev profile)

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Seeded dev users: `testuser / password123` (ROLE_OWNER) and `user2 / password123` (ROLE_EDITOR).

### Environment

Copy `.env.example` to `.env` and adjust values (PostgreSQL credentials, MinIO keys, JWT secret, Kafka bootstrap servers).

## Project Structure

```
shared-pad/
├── backend/                 # Spring Boot 4.1 + Java 21
│   ├── src/main/java/com/syncdocs/
│   │   ├── config/          # Security, WebSocket, Kafka, MinIO
│   │   ├── controller/      # REST + WebSocket controllers
│   │   ├── dto/             # Request/response DTOs
│   │   ├── events/          # Kafka document events
│   │   ├── model/           # JPA entities (incl. StoredEvent)
│   │   ├── repository/      # Spring Data repositories
│   │   ├── security/        # JWT filter, entry point
│   │   └── service/         # Document, conflict, Kafka, presence, audit
│   └── src/test/            # 166 unit + integration tests
├── frontend/                # React 19 + Vite + Electron
│   ├── electron/            # Main + preload (IPC bridge)
│   └── src/
│       ├── api/             # REST + WebSocket clients
│       ├── components/      # SyncEditor, PresenceBar, ShareDialog, ...
│       ├── hooks/           # useWebSocket, useDocumentSync, usePresence
│       ├── pages/           # Login, Register, DocumentList, DocumentEditor
│       └── utils/           # offlineQueue, localDocManager
├── docker-compose.yml       # PostgreSQL + Kafka + MinIO + backend
├── docker-compose.prod.yml  # Production images
└── .env.example
```

## Testing

```bash
cd backend
mvn test
```

## Roadmap

- **V3** — CRDT-based conflict resolution, offline-first collaborative editing.
