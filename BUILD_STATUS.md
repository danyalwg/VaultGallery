# Build Status

## Vault Gallery 2.1 debug build

- Date: 2026-07-21 (Asia/Karachi)
- Project: `D:\personal projects\VaultGallery`
- Android Studio alias: `C:\Users\danya\Desktop\VaultGallery`
- Application ID: `com.danyal.vaultgallery.debug`
- Version: `2.1.0-debug` (version code 4)
- minSdk / targetSdk / compileSdk: 28 / 35 / 35
- Android Gradle Plugin / Gradle: 8.8.2 / 8.10.2
- Java bytecode: 17

## Automated checks

| Check | Result | Details |
|---|---|---|
| `assembleDebug` | PASS | Final APK assembled successfully |
| `testDebugUnitTest` | PASS | GalleryLogic selection, formatting, and grouping tests |
| `lintDebug` | PASS | No lint errors; dependency-update advisories only |
| `connectedDebugAndroidTest` | PASS | 1 launcher/manifest test on Pixel_6_API_30 Android 11 emulator |
| APK signature | PASS | APK Signature Scheme v2 verified; Android debug certificate |
| Reference asset exclusion | PASS | No supplied screenshot names or reference-image paths found in the APK archive |

The final combined Gradle verification ran 81 tasks and completed with `BUILD SUCCESSFUL` in 2m 34s.

## Runtime interaction checks

- Public Gallery launched and displayed MediaStore-owned test media after the permission flow.
- Long-press slide selection selected an inclusive seven-item range; reverse sliding removed a three-item range and left four selected.
- Android presented the native favourite/trash approval path; the public recycle bin displayed the trashed item and exposed restore/permanent-delete actions.
- Secure Gallery Argon2id six-digit PIN setup and unlock completed on the emulator.
- A strong emulator fingerprint was enrolled. Biometric enrollment created the protected vault-key envelope, and a later biometric-only unlock returned to the encrypted gallery without entering the PIN.
- Public video thumbnails and the Samsung-style public player were verified on the connected Pixel 8 Pro.
- A generated 3-second H.264 video was selected through Android's document picker, encrypted, shown as a secure video tile, decrypted only into app-private cache for viewing, and played with a correct `0:03 / 0:03` duration.
- Secure viewer moved the item to the encrypted recycle bin, where it appeared with restore, empty, and permanent-delete controls.
- No `AndroidRuntime` crash was recorded during the interaction pass. `FLAG_SECURE` remains enabled for Secure Gallery.

## APK verification

- Artifact: `artifacts\VaultGallery-2.1-Pixel-audited-debug.apk`
- Size: 71,577,264 bytes
- SHA-256: `7F2E9D95E0AE7E0C69DA1914BFE20E4562323A3454D5F50A46379F1B642F02CA`
- APK Signature Scheme v2: verified
- Signer: Android Debug certificate
- Private reference check: all 708 APK entries scanned; no supplied screenshot names or reference-image paths found

This is a debug-signed personal evaluation build. Read `KNOWN_LIMITATIONS.md` before using irreplaceable private media.
