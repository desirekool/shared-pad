# Phase 17 — UI Polish, Local Files, Autocomplete, Doc List Badges, Tests & Presence

## Streams

| # | Stream | Scope |
|---|--------|-------|
| 1 | Tailwind CSS | Install tailwindcss 4 + @tailwindcss/vite. Swap 9 components inline → Tailwind classes. Inter font. |
| 2 | User autocomplete | Backend: `UserRepository.findByUsernameContainingIgnoreCase` + `GET /api/users/search?q=x`. Frontend: debounced dropdown in ShareDialog. |
| 3 | Local file support | Electron "Open as Local" menu item + IPC. `localDocManager.ts` (localStorage). Promote to server via existing `POST /api/documents/promote`. |
| 4 | Doc list badges | Permission level badges (OWNER/EDITOR/VIEWER). In-memory `LocalShareService` for "Shared Local" section. Local files section with badge. |
| 5 | JWT + auth tests | `JwtTokenProviderTest` (10 unit), `JwtAuthIntegrationTest` (8 B2B), expand `AuthControllerIntegrationTest` (+10). |
| 6 | Presence completion | Wire `sendCursor()` + `sendTyping()` from SyncEditor → STOMP. Remote cursor rendering in Monaco. Remote selection decorations (colored highlights). |

## Phases 18-20 — Testing & Hardening

| Phase | Scope |
|-------|-------|
| 18 | Run all new tests, fix regressions |
| 19 | Full manual + automated regression of all Phase 1-17 features |
| 20 | Edge cases, error handling, cross-browser |

## Phase 21 — Kafka Event Persistence

`kafka_event_store` table, replay endpoint, recovery on reconnect.

## File Inventory

### New Files (12)
```
backend/src/main/java/com/syncdocs/controller/UserController.java
backend/src/main/java/com/syncdocs/controller/LocalShareController.java
backend/src/main/java/com/syncdocs/service/LocalShareService.java
backend/src/test/java/com/syncdocs/security/JwtTokenProviderTest.java
backend/src/test/java/com/syncdocs/security/JwtAuthIntegrationTest.java
frontend/src/index.css
frontend/src/types/localDocument.ts
frontend/src/utils/localDocManager.ts
```

### Modified Files (18)
```
backend/src/main/java/com/syncdocs/repository/UserRepository.java
backend/src/test/java/com/syncdocs/controller/AuthControllerIntegrationTest.java
frontend/package.json
frontend/vite.config.ts
frontend/index.html
frontend/src/main.tsx
frontend/src/vite-env.d.ts
frontend/src/api/auth.ts
frontend/src/pages/Login.tsx
frontend/src/pages/Register.tsx
frontend/src/pages/Home.tsx
frontend/src/pages/docs/DocumentList.tsx
frontend/src/pages/docs/DocumentEditor.tsx
frontend/src/components/SyncEditor.tsx
frontend/src/components/PresenceBar.tsx
frontend/src/components/ShareDialog.tsx
frontend/src/components/VersionPanel.tsx
electron/main.ts + preload.ts
```
