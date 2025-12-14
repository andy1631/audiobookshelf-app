# Task 6: Rich State & Error Plumbing (Internal)

## Goal
Adopt the rich internal state model and clearer error categorization while keeping the external JS/Capacitor contract unchanged.

## Implementation Steps
- Represent parts/items with states: `queued`, `running`, `paused-user`, `paused-policy`, `moving`, `verifying`, `success`, `failed`, `canceled`.
- Map internal states to existing outbound events so JS remains compatible; add logging/metrics for internal states and error categories.
- Categorize errors (retriable vs terminal) and reflect them in state transitions and diagnostics.
- Ensure retry/backoff paths update state appropriately for visibility in logs/notifications.

## Acceptance Criteria
- Internal logs show correct state transitions and error categories.
- JS/Capacitor consumers continue to receive the expected events; no breaking changes.
- Retries/pauses/failures are distinguishable in diagnostics.

## Dependencies / Notes
- Builds on Tasks 1–3; can land alongside Task 5. No UI/API changes.
