# Vault Gallery Version 1 — build status

## Published artifact

| Field | Value |
|---|---|
| Product version | `1.0.0-debug` |
| Android version code | `66` (retained so existing evaluation installs can upgrade in place) |
| Application ID | `com.danyal.vaultgallery.debug` |
| Minimum Android | API 29 / Android 10 |
| Target Android SDK | API 35 |
| CPU architecture | ARM64 / `arm64-v8a` |
| Device verification | Pixel 8 Pro (`husky`) |
| Physical installation | Functionally identical pre-publication build installed in place; Version 1 differs only in its public version label |
| Data-preservation observation | In-place installation retained the original first-install timestamp |
| APK | `VaultGallery-v1.0.0-arm64.apk` in the GitHub Version 1 release |
| Signing | Android debug certificate; evaluation only |

APK size: `449,216,401` bytes. SHA-256:
`4217DF35B9200982E0ECAAB7EBE30104197CA22714F3A5AD659E4BBE765BC201`.
The same digest is attached to the GitHub release in `SHA256SUMS.txt`.

## Release gate

| Check | Result |
|---|---|
| Kotlin/Android compilation | PASS |
| JVM tests | PASS — 73 tests, 0 failures, 0 errors, 0 skipped |
| Android lint | PASS — 0 errors |
| Debug APK assembly | PASS |
| Expanded system-integration instrumentation | PASS |
| `CATEGORY_APP_GALLERY` resolution | PASS |
| Photo/video `VIEW` resolution | PASS |
| Photo/video `EDIT` resolution | PASS |
| Modern/legacy camera review resolution | PASS |
| Photo/video picker resolution | PASS |
| Media collection resolution | PASS |
| Public Gallery cold launch | PASS |
| Secure Gallery authentication-surface launch | PASS |
| Direct photo opening | PASS |
| Warm viewer → picker handoff | PASS |
| Multi-selection picker controls | PASS |
| Android crash buffer after final smoke checks | EMPTY |

## Model integrity

| Model | SHA-256 |
|---|---|
| Carve Big-LaMa FP32 ONNX | `1faef5301d78db7dda502fe59966957ec4b79dd64e16f03ed96913c7a4eb68d6` |
| MediaPipe Magic Touch interactive segmentation task | `38431bc66b883404e8397f74c3579404315b9b52b04a46c6346fe906a7309b03` |

CI verifies both digests before running the Version 1 test/lint/assembly gate.

## Interpretation

These results demonstrate that the published source compiles, its JVM contract suite passes, its
declared Android integration routes resolved on the physical Pixel, and the functionally identical
pre-publication build opened both gallery entry surfaces without a recorded Android runtime crash.
The exact relabeled Version 1 APK could not be reinstalled after the Pixel disconnected during the
publication pass. These results are not a claim of
formal security certification, accessibility certification, codec compatibility with every Android
vendor, or production release signing.
