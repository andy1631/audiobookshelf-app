# Task 3: Resumable Internal Engine (Range + Fixed Retries)

## Goal
Make internal downloads resumable with Range requests, fixed timeouts, and fixed retry attempts while keeping the JS/Capacitor contract unchanged.

## Implementation Steps
- Replace internal download flow with OkHttp that:
  - Reads existing temp file length; adds `Range` header if >0; validates 206/200 responses and truncates/restarts if Range is ignored.
  - Uses fixed timeouts (e.g., connect 10–15s, read/write ~30s) and a stall watchdog.
  - Implements fixed retry attempts (e.g., 3) with linear backoff for retriable errors (network/5xx/408/429); treat other 4xx as terminal.
  - Streams to temp file, updates progress bytes/states, and moves to final path on success.
- Keep notifications and JS events stable; map internal states to existing outbound events.
- Ensure streams are closed and retries do not leak file descriptors.

## Acceptance Criteria
- Interrupting network mid-download and restoring connection resumes from prior byte count (verify via logs/file size).
- Fixed retry attempts occur on transient failures; after max attempts the part is marked failed.
- Length-only validation succeeds; mismatch triggers restart/retry.
- External DownloadManager path and JS interface remain unchanged.

## Dependencies / Notes
- Depends on Task 1 (service scope) and Task 2 (temp layout) for correct file handling.
