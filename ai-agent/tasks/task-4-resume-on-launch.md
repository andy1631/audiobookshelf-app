# Task 4: Resume on App Launch

## Goal
Automatically resume unfinished internal downloads when the app starts, without changing the UI contract.

## Implementation Steps
- On app start, load in-flight internal parts and inspect temp files.
- If a temp file exists, resume via Range from its length; if missing/invalid, restart from byte 0.
- Start the `DownloadService` when unfinished work exists; otherwise, no-op.
- Keep JS/Capacitor events the same; rely on existing progress/complete callbacks.

## Acceptance Criteria
- After force-stop or swipe-kill during an internal download, relaunching resumes or restarts pending parts automatically.
- No downloads start if none were pending.
- External DownloadManager behavior is unaffected.

## Dependencies / Notes
- Depends on Tasks 1–3; bootstrap logic should reuse temp layout and resumable engine.
