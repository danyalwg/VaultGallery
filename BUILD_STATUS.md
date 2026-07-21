# Build Status

## Vault Gallery 1.1 debug build

- Date: 2026-07-21 (Asia/Karachi)
- Project: `D:\personal projects\VaultGallery`
- Android Studio alias: `C:\Users\danya\Desktop\VaultGallery`
- Application ID: `com.danyal.vaultgallery.debug`
- Version: `1.1.0-debug` (version code 2)
- minSdk / targetSdk / compileSdk: 28 / 35 / 35
- Android Gradle Plugin / Gradle: 8.8.2 / 8.10.2
- Java bytecode: 17

## Automated checks

| Check | Result | Details |
|---|---|---|
| `assembleDebug` | PASS | Final APK assembled successfully |
| `testDebugUnitTest` | PASS | 6 GalleryLogic tests, including bidirectional range selection and range deselection |
| `lintDebug` | PASS | No lint errors; HTML report generated |
| `connectedDebugAndroidTest` | PASS | 1 launcher/manifest test on Pixel_6_API_30 Android 11 emulator |
| APK signature | PASS | APK Signature Scheme v2 verified; Android debug certificate |
| Reference asset exclusion | PASS | No supplied screenshot names or reference-image paths found in the APK archive |

The final combined Gradle verification ran 81 tasks and completed with `BUILD SUCCESSFUL` in 1m 8s.

## Runtime interaction checks

- Public Gallery launched and displayed MediaStore-owned test media after the permission flow.
- Long-press slide selection selected an inclusive seven-item range; reverse sliding removed a three-item range and left four selected.
- Android presented the native favourite/trash approval path; the public recycle bin displayed the trashed item and exposed restore/permanent-delete actions.
- Secure Gallery Argon2id setup and passphrase unlock completed on the emulator.
- A strong emulator fingerprint was enrolled. Biometric enrollment created the protected vault-key envelope, and a later biometric-only unlock returned to the encrypted gallery without entering the PIN.
- Public-to-secure import encrypted and displayed a test image.
- Secure viewer moved the item to the encrypted recycle bin, where it appeared with restore, empty, and permanent-delete controls.
- No `AndroidRuntime` crash was recorded during the interaction pass. `FLAG_SECURE` remains enabled for Secure Gallery.

## APK verification

- Artifact: `artifacts\VaultGallery-1.1-debug.apk`
- Size: 70,549,668 bytes
- SHA-256: `E221C302E9FD6B1A89778AB78B7BBBE2BC87D142FE5EED9F30740547B9F2CCB8`
- APK Signature Scheme v2: verified
- Signer: Android Debug certificate

This is a debug-signed personal evaluation build. Read `KNOWN_LIMITATIONS.md` before using irreplaceable private media.
