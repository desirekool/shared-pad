# Progress — June 21-22, 2026

## Goal
Build real-time collaborative document editor with Electron, Spring Boot, Kafka, and WebSocket.

## Fixed Today — Real-time Sync Pipeline

### 1. WebSocket Transport Mismatch
- **File:** `backend/.../config/WebSocketConfig.java`
- **Problem:** Frontend uses raw WebSocket (`brokerURL: "ws://localhost:8080/ws"`) but backend only had SockJS (`withSockJS()`). Connection never established.
- **Fix:** Added `registry.addEndpoint("/ws").setAllowedOriginPatterns("*")` (raw WS) alongside existing SockJS endpoint.

### 2. Jackson `null` → `int` Deserialization Error
- **File:** `backend/.../events/DocumentOperation.java`
- **Problem:** `length` field was `int` (primitive), but INSERT operations don't set `length` → sent as `null` → Jackson throws `MessageConversionException`.
- **Fix:** Changed `int length` → `Integer length`, `int position` → `Integer position`.
- **Also:** `frontend/.../components/SyncEditor.tsx` — added `op.length = 0` for INSERT operations.

### 3. Kafka `Instant` Serialization Error
- **File:** `backend/.../events/KafkaDocumentEvent.java`
- **Problem:** `timestamp` was `java.time.Instant`, which Jackson's `JsonSerializer` can't serialize without `jackson-datatype-jsr310` module.
- **Fix:** Changed `Instant timestamp` → `long timestamp`. Updated all call sites (`EditOperationController.java`, `PresenceController.java`, test files) from `Instant.now()` to `System.currentTimeMillis()`.

### 4. Kafka ADVERTISED_LISTENERS Wrong Inside Docker
- **File:** `docker-compose.yml`
- **Problem:** `KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092` — after backend connects to `kafka:9092`, Kafka tells client to reconnect to `localhost:9092` (wrong inside Docker).
- **Fix:** Changed to `PLAINTEXT://kafka:9092`.

### 5. Stale Closure in SyncEditor
- **File:** `frontend/.../components/SyncEditor.tsx`
- **Problem:** `onOperation` captured in `useEffect(() => {...}, [])` closure at mount time. When WebSocket connects async, `isConnected` flips → new `sendOperation`/`handleOperation` created, but editor still calls the old one (which queues offline).
- **Fix:** Added `const onOperationRef = useRef(onOperation); onOperationRef.current = onOperation;` and replaced `onOperation?.(op)` → `onOperationRef.current?.(op)`.

## Remaining Issue
- **No `SEND /app/document.{id}.edit` visible in backend logs** even though WebSocket connects and subscriptions work.
- **Hypothesis:** `isConnected` state in `useWebSocket.ts` may not reflect actual connection when `sendOperation` runs, OR browser cache serving stale frontend code.
- **Next step:** Check browser console for "WebSocket not connected" warnings and actual `sharedClient.connected` status.

## Other Changes
- `ConflictResolutionService.java` — always returns `true` (optimistic accept for real-time edits; version check only used in Save flow).
- `ConflictResolutionService.java` — guards `Long.valueOf("new")` with try-catch.
- `DocumentServiceTest.java` — updated mock from `findOwnedDocuments` → `findAccessibleDocuments`.
- `ConflictResolutionServiceTest.java` — updated expected result for version-mismatch (now `true` instead of `false`).

## Tests Status
- All service unit tests pass (26 tests).
- Integration tests (ObjectMapper bean) pre-existing failure, unrelated.
