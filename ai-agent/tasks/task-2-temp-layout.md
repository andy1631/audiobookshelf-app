# Task 2: Per-Item Temp Layout & Cleanup

## Goal
Standardize temp file locations per item and ensure proper cleanup without altering user-facing behavior.

## Implementation Steps
- Define per-item temp dir (e.g., `<app files>/downloads/<itemId>/`).
- Update `DownloadItemPart.make` to place internal temp files in that dir; keep final destinations unchanged.
- Add utilities to create/delete temp dirs and files; ensure cleanup on success, cancel, or failure.
- Keep filenames sanitized to avoid collisions; ensure dirs exist before writes.

## Acceptance Criteria
- Downloads still complete and land in correct final destinations.
- Temp files/dirs are removed after success/cancel/failure; no orphaned temp dirs remain.
- No changes to JS/Capacitor APIs or visible UX.

## Dependencies / Notes
- Builds on Task 1 service scaffolding but can be developed in parallel; minimal risk of conflicts.
