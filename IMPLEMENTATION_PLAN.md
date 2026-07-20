# Implementation Plan

## Phase gates

Each phase ends with `assembleDebug`, `testDebugUnitTest`, and `lintDebug`; device-dependent work also runs connected tests on at least API 28, API 33, and API 35. A phase cannot advance with failing mandatory checks or undocumented security compromises.

## Phase 0 — planning

Create the base project, protect local references from Git and packaging, freeze architecture/security/storage decisions, define measurable UI targets, and record the baseline. No user-facing feature code or destructive media behavior is introduced.

## Phase 1 — visual foundation

Create modules and design tokens, select and license an original rounded font, add original icons/branding, configure both launcher entries, and implement responsive shells for Pictures, Albums, All Albums, Stories, Menu, Settings, and selection states. Add accessibility semantics and deterministic generated fixtures for screenshot tests. All visible actions either navigate to implemented Phase 1 destinations or remain absent.

## Phase 2 — public gallery engine

Implement permission education, full/partial/denied states, incremental MediaStore indexing, Paging, thumbnails, date/album grouping, viewers, Media3 playback, favourites, selection, shares, user-approved modifications/deletions, trash, and basic offline search. Validate with 100,000-record synthetic datasets and corrupt-media fixtures.

## Phase 3 — secure architecture

Implement secure first-use setup, six-digit-or-longer PIN/passphrase policy, biometric convenience, recovery-key option, Android Keystore wrapping, encrypted metadata store, versioned chunked files, isolated encrypted thumbnails, lock lifecycle, `FLAG_SECURE`, neutral recents preview, and secure empty UI. Real media import remains disabled until security and corruption tests pass.

## Phase 4 — secure import and playback

Implement persistent copy/move/folder-tree transactions, storage estimation, pause/resume/cancel/recovery, duplicate conflicts, safe source deletion approval, encrypted viewers, custom Media3 data source, secure albums, export/share cleanup, and secure trash.

## Phase 5 — editing and creation

Implement non-destructive photo/video edit projects, validated exports, collage/GIF/slideshow/movie workflows, and secure temporary-file journals. Ship only tools with real processing, undo/redo, cancellation, recovery, and tests.

## Phase 6 — intelligence and cloud

Add opt-in, on-device OCR/labels/similarity/people/place features with deletable derived indexes. Add public cloud only against a documented provider API. Any secure backup must be ciphertext-only, recovery-key based, and separately consented.

## Cross-phase exit criteria

- No reachable placeholder actions, hardcoded credentials/keys, sensitive logs, or reference media in artifacts.
- Loading, empty, error, permission, cancellation, retry, rotation, process recreation, and accessibility behavior are covered where applicable.
- Documentation and threat model reflect the exact implementation, including omissions.
- Release validation waits for external signing configuration and a supported emulator/device matrix.
