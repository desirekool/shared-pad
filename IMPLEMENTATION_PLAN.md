# SyncDocs – Implementation Plan

## Overview
Build a real-time collaborative document editor where multiple users can edit the same document simultaneously, with changes propagated instantly using Spring Boot, Apache Kafka, and WebSockets. Electron + React desktop client with Monaco Editor.

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│              Electron + React Desktop App                │
│                                                          │
│  ┌────────────────┐      ┌───────────────────────┐      │
│  │  Monaco Editor  │      │  File System API      │      │
│  │  (operations)   │◄────►│  (local files via     │      │
│  │                 │      │   Electron preload)    │      │
│  └───────┬────────┘      └───────────┬───────────┘      │
│          │                           │                   │
│          └───────────Sync────────────┘                   │
│                      │                                   │
└──────────────────────┼───────────────────────────────────┘
                       │
               WebSocket / HTTPS
                       │
               ┌───────▼───────────────────────┐
               │     Spring Boot Backend        │
               │  (REST + WebSocket + Kafka)    │
               └───────┬───────────────────────┘
                       │
               ┌───────▼──────┐
               │  Apache Kafka │
               └───────┬──────┘
                       │
          ┌────────────┼────────────┐
          │            │            │
   ┌──────▼────┐ ┌────▼────┐ ┌─────▼──────┐
   │ Persist   │ │ Audit   │ │ WebSocket  │
   │ Service   │ │ Service │ │ Broadcaster│
   └──────┬────┘ └─────────┘ └─────┬──────┘
          │                        │
   ┌──────▼──────┐          ┌──────▼──────┐
   │  PostgreSQL  │          │   MinIO     │
   │  (metadata)  │          │ (content)   │
   └─────────────┘          └─────────────┘
```

### Data Flow

```
User A edits
      │
      ▼
Spring Boot REST/WebSocket
      │
      ▼
Kafka topic: document.edit
      │
      ├────────► Persistence Service  (PostgreSQL + MinIO)
      ├────────► Audit Service        (PostgreSQL)
      ├────────► Analytics Service    (optional)
      └────────► WebSocket Broadcaster
                         │
                         ▼
             All connected users receive update
```

---

## Document Types

| Aspect | Server Documents | Local Files |
|--------|-----------------|-------------|
| Content stored | MinIO (hash-addressed) | NOT stored on server |
| Metadata | PostgreSQL | Session metadata only (PostgreSQL) |
| Audit logging | Every event (edits, comments, permissions, versions, exports) | Session metadata only (session ID, participants, timestamps, normal/abnormal end) |
| Edit operations persisted | Yes | No |
| Permission system | Owner / Editor / Viewer | N/A (ephemeral sessions) |
| Version history | Yes (snapshots in MinIO) | N/A |

### Local File → Server Document Promotion

When user chooses "Save to Server" or "Import as Shared Document":

| Field | Description |
|-------|-------------|
| Original filename | e.g. `requirements.md` |
| Original path | User-controlled, opt-in (default: filename only for privacy) |
| File size | bytes |
| Last modified (local) | Timestamp |
| SHA-256 checksum | Content hash |
| Imported by | User ID |
| Import timestamp | Timestamp |
| Original MIME type | e.g. `text/markdown` |

---

## Tech Stack

| Layer | Choice |
|-------|--------|
| Build tool | **Maven** |
| Desktop | **Electron + React + TypeScript** |
| Editor | **Monaco Editor** |
| IPC | Electron preload API (`contextIsolation: true`, `nodeIntegration: false`) |
| Backend | **Spring Boot** (Web, WebSocket, Security, JPA, Validation, Kafka) |
| Auth | **Spring Security + JWT** (BCrypt, optional refresh tokens) |
| Event bus | **Apache Kafka** (KRaft mode — no Zookeeper) |
| Database | **PostgreSQL** (all metadata, sessions, audit) |
| Object store | **MinIO** (document content, version snapshots, promoted local content) |
| CI/CD | **GitHub Actions** + **Act** (for local pipeline testing) |
| Orchestration | **Docker Compose** |

---

## Phases

### Phase 1 – Project Setup
- Spring Boot + Maven POM with all dependencies
- React + TypeScript + Electron project
- Monaco Editor integration
- Docker Compose stack: Spring Boot, Kafka (KRaft), PostgreSQL, MinIO
- Application profiles: `dev`, `prod`

### Phase 2 – Authentication
- User registration, login, JWT tokens
- Password hashing (BCrypt), optional refresh tokens
- DB tables: `users`, `roles`, `user_roles`

### Phase 3 – Document Management
- REST endpoints: create, read, update metadata, delete, upload, download, list user docs
- DB tables: `documents`, `document_permissions`
- Document metadata in PostgreSQL, content in MinIO (SHA-256 key)
- Local file support via Electron preload FS API
- Promotion: local file → hash content → MinIO + metadata in PostgreSQL with origin fields
- Privacy: never auto-store full local paths; default to filename only

### Phase 4 – WebSocket Communication
- WebSocket endpoint: `/ws`
- Session management, room/document subscriptions
- Subscribe: `/topic/document/{documentId}`
- Join/leave notifications
- Auto-reconnection handling

### Phase 5 – Kafka Integration
- Topics: `document.edit`, `document.save`, `document.created`, `document.deleted`, `document.download`, `document.upload`, `user.presence`, `audit.events`
- Flow: User action → WebSocket → Spring Boot → Kafka → Consumer branches to Persist + Audit + Analytics + WebSocket Broadcaster

### Phase 6 – Operation-Based Editing
- Granular operations instead of full document sync:
  - `INSERT` — `{ documentId, userId, type: "INSERT", position, text }`
  - `DELETE` — `{ documentId, userId, type: "DELETE", position, length }`
  - `REPLACE` — `{ documentId, userId, type: "REPLACE", position, length, text }`

### Phase 7 – Conflict Resolution
- **V1 (initial):** Version-based — each operation carries document version; reject stale edits or transform before applying
- **V3 (stretch):** CRDT — character-level unique IDs, deterministic merge, zero conflicts

### Phase 8 – Persistence
- Server documents: content in MinIO, metadata + edit history in PostgreSQL
- Local files: NOT persisted on server (session metadata only)
- Optional: persist Kafka events for replay and recovery

### Phase 9 – Presence & Collaboration
- Active user list, cursor positions, current selections, typing indicators
- Join/leave events via Kafka `user.presence` topic
- Applied to both server docs and local file collaboration sessions

### Phase 10 – Version History (Server Documents Only)
- Snapshots stored in MinIO, references in PostgreSQL
- Browse, diff, restore previous versions

### Phase 11 – Permissions (Server Documents Only)
- Roles: **Owner**, **Editor**, **Viewer**
- Share documents, revoke access, read-only mode

### Phase 12 – Auditing & Monitoring

#### Server Document Audit (full logging)
- User login/logout
- Document opened/closed
- User joined/left collaboration session
- Every edit operation
- Comments added/removed
- Permissions changed
- Version created/restored
- File renamed/deleted
- Export/download events

#### Local File Audit (metadata only)
- Session ID
- Participants
- Join/leave timestamps
- Session start/end time
- Optional file fingerprint (SHA-256)
- Whether session ended normally

#### Monitoring
- Structured logging (SLF4J + Logback)
- Spring Boot Actuator (health, metrics, info endpoints)
- Kafka dead-letter topics, retry handling
- Optional: Prometheus + Grafana

### Phase 13 – Offline Support (Electron)
- When offline: save edits locally, queue sync events
- On reconnect: upload pending operations to server
- Conflict resolution on reconnect (version-based → CRDT in V3)

### Phase 14 – Testing
- Unit: services, Kafka producers/consumers, business logic
- Integration: REST APIs, WebSockets, Kafka event flow
- End-to-end: two simulated users editing same document concurrently
- Target: 80%+ coverage on core business logic

### Phase 15 – Deployment
- Dockerfiles for each service
- Docker Compose for local development
- Environment configuration (profiles / `.env`)
- CI pipeline: GitHub Actions + Act (local testing)
- Deployment guide
- Optional: Kubernetes / cloud deployment

---

## Feature Roadmap

| Version | Features |
|---------|----------|
| **V1** | Open/save local files, real-time collaboration, auto-reconnect, authentication, version-based conflict resolution |
| **V2** | Presence indicators + live cursors, version history, comments/annotations, permissions, full audit logging |
| **V3** | CRDT-based conflict resolution, end-to-end encryption, plugin system, multi-file workspace support |

---

## Build Order

1. Project setup (backend + frontend + Docker)
2. Authentication
3. Document CRUD + Local files + Promotion (MinIO + PostgreSQL)
4. WebSocket infrastructure
5. Kafka integration
6. Real-time editing (operation sync)
7. Persistence
8. Conflict resolution (version-based)
9. Presence indicators
10. Version history
11. Permissions
12. Auditing + Monitoring
13. Offline support
14. Testing
15. Deployment

---

## Nice-to-Have (Post-MVP)

- Markdown preview / rich text editing
- Comments and suggestions
- Image embedding
- Full-text search
- Export to PDF / DOCX
- Real-time analytics dashboard
- AI-assisted writing features
