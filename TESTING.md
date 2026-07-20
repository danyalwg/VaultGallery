# Testing Strategy

## Layers

- Pure unit tests: date grouping, album sorting/merging, essential selection, search parser, selection reducer, trash expiry, duplicate/conflict policy, logical folder reconstruction, transaction state machines, file-format parser, key envelopes, lock timeout, and cleanup decisions.
- JVM integration tests: Room migrations, repository contracts with fakes, encrypted stream round trips, corruption vectors, paging invalidation, and worker restart logic where Android dependencies can be isolated.
- Instrumented tests: MediaStore permissions/mutations, Photo Picker/SAF grants, real Room/Keystore/Biometric flows, lifecycle locking, screenshot/recents flags, secure DataSource seeking, import/export cleanup, and reset.
- Compose UI/screenshot tests: the matrix in `SCREENSHOT_TEST_MATRIX.md`, semantics, keyboard focus, TalkBack labels, large font, rotation, adaptive layouts, and selection persistence.
- Benchmarks: cold launch, timeline scroll, album/search/viewer/player, indexing, secure thumbnail/import/seek, and photo/video export.

## Fixtures

Fixtures are generated geometric images, synthetic videos/audio/subtitles, fake metadata, and deliberately corrupt files. Include zero-byte, truncated JPEG, invalid EXIF, corrupt MP4, huge dimensions, wrong extension, non-seekable source, HDR/RAW capability gates, and duplicate sets. Never use supplied screenshots or personal media.

## Mandatory gates

Each change runs focused tests and compilation. Each phase runs `assembleDebug`, `testDebugUnitTest`, and `lintDebug`; connected tests run when a supported emulator/device is available. Release candidates additionally run release compilation, migration tests, screenshot matrix, macrobenchmarks, dependency/licence review, security checklist, and manual two-launcher flows.

## Failure policy

Compiler/lint/test failures are fixed or reported; they are not suppressed. Flaky tests are quarantined only with an owner, issue, reason, and deterministic replacement plan. Unsupported device capability results in an explicit skipped-capability assertion, never a false pass.
