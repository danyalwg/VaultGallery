# Testing strategy

## Version 1 release gate

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The published gate completed with 73 JVM tests, zero failures/errors/skips and zero lint errors.

## Test layers

- **Pure/JVM logic:** media sorting, album rules, transfer routing, crop math, tone/curve math, smart
  albums, subtitles, selection behavior, duplicates, safety limits and cryptographic helpers.
- **Connected instrumentation:** Android intent resolution, real encoding, Ultra HDR, lens correction,
  vector output, media composition, transfer behavior, secure export/recovery, model inference and
  persistent search indexes.
- **Physical interaction:** public/Secure launch, viewer ordering, media paging, precise seek,
  filmstrip behavior, picker handoff, biometric/PIN surface, transfer notifications and editor output.
- **Static analysis:** Android lint and release manifest review.
- **Artifact checks:** model checksums, APK hash, version identity, in-place install and crash buffer.

## Fixtures

Use synthetic geometric images, generated video/audio/subtitles, fake metadata, deliberate corruption
and disposable MediaStore files. Never commit a user’s private screenshots, contacts, locations,
device dumps, vault media or recovery material.

## Required change-level checks

1. Compile the touched source set.
2. Run focused tests.
3. Run the complete JVM suite.
4. Run lint.
5. Assemble the APK.
6. Run relevant connected tests when Android behavior is involved.
7. Verify public and Secure Gallery parity where the concept applies.
8. Verify cancellation, retry and partial failure for data-moving work.
9. Inspect the Android crash buffer after physical smoke checks.

Unsupported device capability must be reported explicitly; it is not a successful feature test.
