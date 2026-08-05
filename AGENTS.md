# Summary

## Goal
Build a real-time collaborative document editor with Electron desktop client, Spring Boot backend, Apache Kafka event bus, and WebSocket-based live sync.

## Constraints & Preferences
- Maven as build tool for backend.
- KRaft mode for Kafka (no Zookeeper).
- MinIO for content-addressable document storage.
- PostgreSQL for metadata only.
- Conflict resolution: version-based now, CRDT in V3.
- **Upgrades:** Spring Boot 4.1.0, React 19, TanStack Router v1, TanStack Query v5, Vite 7 (fallback from 8), TypeScript 6.

## Progress
### Done
- Phases 1–15: project setup, auth, document CRUD, WebSocket, Kafka, editing ops, conflict resolution, persistence, presence, version history, permissions, audit, testing, deployment CI, offline queue.
- Phase 16 (Migration + fixes) — All blocking issues resolved.
- **Real-time sync now works end-to-end.** Two browser tabs (`user2` and `testuser`) successfully sync edits via WebSocket → STOMP → Spring controller → Kafka producer → Kafka consumer → STOMP broadcast → other client.
- **Phase 17 (UI Redesign + Local Files + Revoke Fix)** — All features implemented.
- **Phase 18 (Test Fixes + Coverage Complete)** — 127 tests, 0 failures, 0 flaky. 16 new test files covering all controllers, services, security, and config.
- **Phase 19 (Manual Regression)** — Full automated + manual regression verified.
- **Phase 20 (Edge Cases, Error Handling, Cross-Browser)** — 154 tests, 0 failures, 0 flaky. All 22 inline styles → Tailwind. Permission WebSocket notifications. Connected indicator. Offline queue TTL + version check. 27 new edge case tests.
- **Phase 21 (Kafka Event Persistence)** — 166 tests (+12), 0 failures. `StoredEvent` entity + repository for persistent event store. `StoredEventService` persists ALL Kafka events. `KafkaConsumerService` stores events before processing. `GET /api/documents/{id}/events?after={eventId}` replay endpoint. Proper version-based conflict resolution (rejects future versions). Client fetches and replays missed events on WebSocket reconnect before flushing offline queue.

### Phase 17 Summary
- Tailwind CSS v4 replaces inline styles across all components (Home, DocumentList, DocumentEditor, ShareDialog, Login, Register, PresenceBar, VersionPanel, SyncEditor).
- Local document management via `LocalDocumentManager` (localStorage): create, save, list, delete, promote to server.
- `LocalShareService` + `LocalShareController` for session-based local file sharing.
- `UserController` with `GET /api/users/search?q=x` for autocomplete in ShareDialog.
- Electron IPC channels: `dialog:openLocalFile`, `menu:file-imported`, `set-title`.
- Electron window title syncs via `document.title` + `set-title` IPC channel.
- DocumentEditor redesigned with collapsible toolbar (grid-rows animation), auto-hide on local edit, merged dual header bars.
- Backend revoke permission fix: `permissionRepository.findById(permissionId)` instead of `userRepository.findById(userId)`, added `@Transactional`.
- Frontend hides "Revoke" button for OWNER-level permissions.
- Backend Docker image rebuild required for source changes to take effect (`docker compose build backend`).
- Test failures on Jackson 2→3 imports (`tools.jackson.databind.ObjectMapper`) and deleted-user JWT (`UsernameNotFoundException` caught in filter) fixed.

### Issues Fixed
- **WebSocket transport mismatch** — Raw WS endpoint added alongside SockJS.
- **Jackson `int`→`null` deserialization** — `position`/`length` changed from `int` to `Integer`.
- **Jackson `long`→`null` deserialization** — `timestamp` changed from `long` to `Long`.
- **Kafka `Instant` serialization** — changed `Instant timestamp` to `long`.
- **Kafka `ADVERTISED_LISTENERS`** — changed from `localhost:9092` to `kafka:9092` for Docker routing.
- **Stale closure in SyncEditor** — `onOperationRef` pattern added.
- **`Map.of()` null issue** — replaced with `HashMap` in `EditOperationController`.
- **Kafka consumer not active** — Added `@EnableKafka` and `ConcurrentKafkaListenerContainerFactory` bean.
- **Revoke permission used user ID, not permission ID** — Fixed `PermissionController.revoke()` to use `permissionRepository.findById()`. Docker required `build` to pick up changes.
- **Revoke button visible on OWNER permission** — Hidden in ShareDialog; only EDITOR/VIEWER show the Revoke button.
- **Jackson 2→3 conflict** — Spring Boot 4.1.0 ships Jackson 3.x (`tools.jackson.databind.ObjectMapper`). Integration tests re-imported from Jackson 3 package.
- **Deleted user JWT 500** — `JwtAuthenticationFilter` now catches `UsernameNotFoundException` and clears security context, letting entry point return 401.

### Key Architecture
- Backend port 8080, frontend dev server 5173, WebSocket `ws://localhost:8080/ws` (raw WebSocket + SockJS).
- Four Docker Compose services: postgresql (5432), kafka (9092/9093), minio (9000/9001), backend (8080).
- **Spring Kafka 4.1.0 removed ALL auto-configuration** — all beans must be defined manually. `@EnableKafka` is required for `@KafkaListener`.
- Frontend uses singleton STOMP client (`useWebSocket.ts`) with `@stomp/stompjs` v7, Monaco Editor v0.55.1.
- Two test users seeded in `dev` profile: `testuser / password123` (ROLE_OWNER), `user2 / password123` (ROLE_EDITOR).
- Tailwind CSS v4 via `@tailwindcss/vite` Vite plugin.
- Electron IPC bridge with 8 preload-exposed methods.
- Local files stored in `localStorage` under `syncdocs_local_docs` key.
- **Backend Docker image must be rebuilt** (`docker compose build backend`) to pick up Java source changes — `docker compose up -d` alone restarts with the old image.

### Phase 18 — Test Fixes + Coverage Complete
- Fixed all 9 broken tests: **66 → 75 baseline** (JwtAuthIntegrationTest FK violation, PermissionControllerIntegrationTest hardcoded ID, JJWT tampered-token flakiness).
- Expanded `AuthControllerIntegrationTest` from 16 to 25 tests.
- **Total test count: 127** — 0 failures, 0 flaky (verified 3 consecutive runs).
- **New test files (16):** UserControllerTest, AuditControllerTest, PresenceRestControllerTest, LocalShareControllerTest, EditOperationControllerTest, PresenceControllerTest, EditHistoryServiceTest, MinioServiceTest, LocalShareServiceTest, KafkaConsumerService (3 new handlers), JwtAuthenticationFilterTest, WebSocketAuthInterceptorTest, CustomUserDetailsServiceTest, JwtAuthenticationEntryPointTest, SecurityConfigTest, WebSocketConfigTest, KafkaProducerConfigTest.
- **Frontend:** TypeScript compiles clean (0 errors).
- **Docker:** Backend healthy, revoke fix verified end-to-end.

### Phase 19 — Manual Regression
- Full automated + manual regression of all Phase 1-17 features verified.
- Frontend TypeScript compiles clean. Docker backend healthy with revoke fix confirmed end-to-end.

### Phase 20 (Edge Cases, Error Handling, Cross-Browser) — DONE
- **Stream 1 (Error Handling):**
  - `GlobalExceptionHandler` (`@ControllerAdvice`) centralizes exception handling: Runtime→400, AccessDenied→403, MethodArgumentNotValid→400 (field-level messages), HttpMessageNotReadable→400, MissingParam→400, TypeMismatch→400, MethodNotAllowed→405, generic→500.
  - `ErrorBoundary.tsx` React component wrapping the app in `main.tsx`.
  - `useWebSocket.ts`: `onStompError` + `onWebSocketError` handlers, `onError` callback option. Removed silent `catch(() => {})` in `deactivate()`.
- **Stream 2 (Input Validation):**
  - `DocumentCreateRequest`/`DocumentUpdateRequest`: `@Length(max=10_000_000)` on content.
  - `PromoteRequest`: `@Min(0)` on `fileSize`.
  - `LocalShareRequest` DTO with `@NotBlank`/`@Size` — `LocalShareController.share()` uses `@Valid @RequestBody`.
  - Frontend title input: `maxLength={255}` in `DocumentEditor.tsx`.
- **Stream 3 (Permission/Session Edge Cases):**
  - Renamed `userId`→`permissionId` in `revokePermission()` API function.
  - `PermissionController`: injects `SimpMessagingTemplate`, sends WebSocket notification to affected user on `share()` and `revoke()` via `/user/queue/permissions`.
  - `useDocumentSync.ts`: subscribes to `/user/queue/permissions`, triggers `onPermissionRevoked` callback.
  - `DocumentEditor.tsx`: redirects to `/docs` on permission revoked, "Access denied", or "Document not found".
- **Stream 4 (Offline Queue + Connectivity):**
  - `DocumentEditor.tsx`: green "Connected" badge when WS is live (previously only showed amber "Offline").
  - `offlineQueue.ts`: TTL (30s) stale message filtering in `getQueue()`.
  - `useDocumentSync.ts`: version check on queue flush — skips edits with `version < currentVersion`. Queued messages include version number.
- **Stream 5 (Edge Case Tests):**
  - Added 27 new edge case tests across 8 files.
  - **Total: 154 tests, 0 failures, 0 flaky** — a net +27 from the Phase 18 baseline.
  - New tests cover: null/empty/negative/large/invalid inputs, non-existent documents/users, SQL injection, auth guard, duplicate shares, invalid permission levels, missing fields, concurrent access, and 401/404/400 status code verification.
- **Stream 6 (Cross-Browser):**
  - Added `browserslist` to `package.json` (production: >0.2%, not dead; dev: last 1 chrome/firefox/safari).
  - Converted all 22 remaining inline styles (`style={{}}`) to Tailwind CSS classes across 5 components: `SyncEditor` (1), `Login` (2), `Register` (2), `VersionPanel` (12), `PresenceBar` (5).

### Remaining Issues
- (none)


