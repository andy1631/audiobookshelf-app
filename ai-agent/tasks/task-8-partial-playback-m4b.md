# Task 8: Partial Playback Support (with M4B Considerations)

## Goal
Allow playback of partially downloaded files (playable up to downloaded bytes), with specific handling for M4B/MP4 containers.

## Implementation Steps
- Expose temp file paths to the player before completion; tag tracks as `partial` while downloading.
- Player changes:
  - Read from temp files; clamp seeks to the downloaded byte length.
  - Detect container support: MP3 is generally progressive-friendly; M4B/MP4 may require `moov` atom at the front.
  - If `moov` is missing (non-faststart M4B), either block partial playback or fall back to a message “available after download completes”.
- Metadata handling:
  - Delay chapter/bookmark parsing for M4B until the moov atom is available; update metadata on completion.
- Finalization:
  - On completion, move temp to final path and re-scan/refresh metadata; switch the player source from temp to final seamlessly.

## Acceptance Criteria
- MP3 partial downloads can start playback and seek only within downloaded bytes without crashes.
- M4B faststart files can play partially; non-faststart M4B is either blocked for partial playback or handled gracefully with messaging.
- On completion, playback continues from temp/final seamlessly with full seek/chapter support.

## Dependencies / Notes
- Depends on resumable temp-file downloads (Tasks 1–4).
- For best M4B behavior, prefer faststart remuxing (`-movflags +faststart`) upstream; otherwise partial M4B playback may be limited.
