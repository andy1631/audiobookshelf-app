# Task 7: Testing Harness & Manual Matrix

## Goal
Add automated coverage for the new download engine and document manual scenarios to guard against regressions.

## Implementation Steps
- Unit tests:
  - State machine transitions across success/fail/retry/pause/cancel.
  - Range resume offset math and validation of server responses (200 vs 206).
  - Retry sequencing and length-validation logic.
- Integration tests:
  - Mock HTTP server that throttles, drops connections, and returns 416/5xx; verify resume, backoff, and temp file reuse.
  - Ensure no file-descriptor leaks across retries.
- Manual matrix (document and execute):
  - Lock screen/Doze while downloading; observe continuity.
  - Toggle Wi‑Fi ↔ cellular with Wi‑Fi-only policy; expect pauses/resumes per policy.
  - Swipe-kill/force-stop app during internal download; relaunch and observe resume.
  - External/SAF downloads via DownloadManager still complete and move.
  - Low-storage behavior and notification permission denied handling.

## Acceptance Criteria
- Unit and integration suites pass locally.
- Manual matrix executed at least once pre-release; findings documented.
- No regressions in non-download areas of the app.

## Dependencies / Notes
- Best run after Tasks 3 (resumable engine) and 5 (DM reconciliation) so both paths are covered.
