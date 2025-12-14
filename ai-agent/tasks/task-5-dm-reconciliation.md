# Task 5: DownloadManager Reconciliation (External/SAF)

## Goal
Continue using Android DownloadManager for external/SAF targets but reconcile statuses on app launch to keep moves and events in sync.

## Implementation Steps
- Persist `downloadId` for external parts when enqueuing with DownloadManager.
- On app start, query DownloadManager for pending IDs:
  - On success: perform final move, mark part success, emit completion to JS.
  - On failure or missing row: mark failed, clean temp, emit failure.
- Prevent duplicate moves by marking parts as moved; guard idempotency.
- Integrate reconciliation into the same bootstrap used for internal resume.

## Acceptance Criteria
- External downloads survive app kill; on relaunch, completed ones are moved and reported, failed ones are marked failed.
- No double moves or duplicate events for the same download.
- Internal downloads remain stable and unaffected.

## Dependencies / Notes
- Should land after Task 1; can be developed alongside Task 3 but integrates with the bootstrap logic from Task 4.
