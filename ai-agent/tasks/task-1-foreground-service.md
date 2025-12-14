# Task 1: Foreground Download Service

## Goal
Host download orchestration in a dedicated foreground service with an aggregate progress notification while keeping current behavior intact.

## Implementation Steps
- Create `DownloadService` with its own `CoroutineScope` (`SupervisorJob + Dispatchers.IO`).
- Add a notification channel (low importance) and a single ongoing notification showing aggregate progress and active count.
- Move queue-watching from `GlobalScope` in `DownloadItemManager` into the service scope; keep API/JS usage unchanged.
- Start the service when downloads begin; stop it when no internal downloads remain.

## Acceptance Criteria
- Downloads still function as before; no API/JS contract changes.
- A persistent “Downloads” notification is shown only while downloads are active.
- No `GlobalScope` usage for download loops; work is tied to the service scope.
- External DownloadManager path remains unaffected.

## Dependencies / Notes
- None; this should be the first landing step.
