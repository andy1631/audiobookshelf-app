# Android Download Reliability Overhaul — Design

## Scope & Goals
- Make internal downloads resumable and reliable under lock screen/Doze/app-kill.
- Keep the existing JS/Capacitor surface and DownloadManager UX unchanged for now.
- Lay foundations for future tuning (user-configurable concurrency, Room-backed state) without blocking this pass.

## Decisions (locked for v1)
- State model: rich per-part states (`queued`, `running`, `paused-user`, `paused-policy`, `moving`, `verifying`, `success`, `failed`, `canceled`).
- Concurrency: fixed cap of 5 concurrent parts; later we can expose a user setting/adaptive rules.
- Network policy: keep current behavior, only adapt code to new plumbing if needed.
- Execution: dedicated foreground `DownloadService`; rely on foreground priority (no extra wake/Wi‑Fi locks in v1).
- Resume: Range-based continuation for internal downloads against a temp file.
- Retries: fixed attempt count with simple backoff; more sophisticated policies later.
- Integrity: length check only (Content-Length vs bytes written).
- Storage: per-item temp directory; clean up on success/cancel/fail.
- External/SAF: continue using system DownloadManager; reconcile events only as needed.
- Capacitor/UI: no contract changes; emit the same events with improved reliability underneath.
- Notifications: single persistent “Downloads” notification with aggregate progress.
- Resume trigger: resume unfinished work on app launch (no BOOT_COMPLETED handler yet).
- Testing: add unit + integration (mock server) and run a manual matrix.

## Architecture Outline
- **DownloadService (foreground)**: owns a coroutine scope, manages the queue, enforces concurrency, and updates the aggregate notification.
- **DownloadRepository**: orchestrates items/parts, exposes commands (start/pause/resume/cancel), and maps states to JS events without changing the existing API.
- **Internal engine**: OkHttp with Range support, fixed connect/read/write timeouts, fixed-attempt retry; writes to temp file then moves to final path; marks state transitions in memory (future: Room).
- **DownloadManager path**: unchanged; track downloadId and finalize/move when DM reports success; keep the UI contract stable.
- **Persistence (future)**: keep a thin adapter so we can swap in Room later; for now, maintain minimal state needed for resume-on-launch.

## State & Temp Layout
- Per-item temp dir under app files/cache (e.g., `files/downloads/<itemId>/`), one temp file per part.
- Part states flow: `queued → running → (paused-*) → running → moving → verifying → success | failed | canceled`.
- On app launch: load in-flight parts, validate temp file length, issue Range request from the current size, or restart if the server rejects Range.

## Retry & Timeout Defaults (v1)
- Fixed attempts (e.g., 3) with a short linear backoff; classify 5xx/network timeouts as retriable, 4xx as terminal except 408/429.
- Timeouts: ~10–15s connect, ~30s read/write, plus a stall detector to abort stuck transfers.

## Notifications
- Single ongoing notification with aggregate progress and status text (e.g., “Downloading 3 items — 57%”); keep it running while any internal part is active.
- If POST_NOTIFICATIONS is denied, continue silently but keep the service alive.

## Task Checklist
- [x] Task 1: Foreground Download Service (`docs/tasks/task-1-foreground-service.md`)
- [ ] Task 2: Temp Layout (`docs/tasks/task-2-temp-layout.md`)
- [ ] Task 3: Resumable Engine (`docs/tasks/task-3-resumable-engine.md`)
- [ ] Task 4: Resume on Launch (`docs/tasks/task-4-resume-on-launch.md`)
- [ ] Task 5: DownloadManager Reconciliation (`docs/tasks/task-5-dm-reconciliation.md`)
- [ ] Task 6: State and Errors (`docs/tasks/task-6-state-and-errors.md`)
- [ ] Task 7: Testing (`docs/tasks/task-7-testing.md`)
- [ ] Task 8: Partial Playback M4B (`docs/tasks/task-8-partial-playback-m4b.md`)

## Testing Plan
- Unit: state machine transitions, Range resume math (offset validation), length checks, retry/backoff sequencing.
- Integration (mock server): throttle/interrupt connections, drop after partial write, return 416/5xx, verify resume and retry paths, ensure temp file reuse.
- Manual: lock screen/Doze, toggle Wi‑Fi↔cellular with Wi‑Fi-only policy, swipe-kill app then relaunch (resume), SAF/DownloadManager path still completes and moves, low-storage handling, notification denied.
