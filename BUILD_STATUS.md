# Build Status

## Installable milestone

- Date: 2026-07-21 (Asia/Karachi)
- Project: `D:\personal projects\VaultGallery`
- Android Studio alias: `C:\Users\danya\Desktop\VaultGallery`
- Application ID: `com.danyal.vaultgallery.debug`
- Version: `1.0.0-debug` (version code 1)
- minSdk / targetSdk / compileSdk: 28 / 35 / 35
- Android Gradle Plugin / Gradle: 8.8.2 / 8.10.2
- Java bytecode: 17

## Required checks

| Command | Result | Details |
|---|---|---|
| `.\gradlew.bat assembleDebug` | PASS | Exit 0; `BUILD SUCCESSFUL in 10s`; 37 tasks; APK assembled |
| `.\gradlew.bat testDebugUnitTest` | PASS | Exit 0; `BUILD SUCCESSFUL in 3s`; 4 GalleryLogic tests passed |
| `.\gradlew.bat lintDebug` | PASS | Exit 0; `BUILD SUCCESSFUL in 38s`; no lint errors |
| `.\gradlew.bat connectedDebugAndroidTest` | PASS | Exit 0; `BUILD SUCCESSFUL in 52s`; 1 launcher-manifest test passed on Pixel_6_API_30 Android 11 emulator |

## Runtime smoke checks

- APK installed successfully on the API 30 emulator.
- Public Gallery launched cold without a crash, displayed the permission education screen, and displayed the real empty state after permission grant.
- Secure Gallery launched cold without a crash.
- Argon2id first-use vault creation completed on-device.
- Secure Gallery displayed its encrypted empty state after setup.
- Returning from the background required authentication after the lifecycle-lock correction.
- `FLAG_SECURE` prevented secure screenshots.
- Both launcher activities are present with distinct labels and icons.

## APK verification

- Artifact: `artifacts\VaultGallery-1.0-debug.apk`
- Size: 76,854,374 bytes
- SHA-256: `42A61A80F7FED10D661EA06DB4B5750A1917747BE7F4170603508494C68CC497`
- APK Signature Scheme v2: verified
- Signer: Android Debug certificate
- Private reference image entries found in APK: 0

This is a debug-signed evaluation build. Read `KNOWN_LIMITATIONS.md` before using real private media.
