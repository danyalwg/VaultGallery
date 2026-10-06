# Contributing to Vault Gallery

Thank you for helping improve Vault Gallery Version 1. The project accepts focused bug fixes,
performance work, tests, documentation corrections and carefully justified feature proposals.

## Before opening an issue

1. Search existing issues.
2. Confirm the behavior on the latest Version 1 artifact or current `main` source.
3. Remove private photos, filenames, contacts, locations, vault material and credentials from every
   screenshot and log.
4. Record the device model, Android version, exact action sequence and expected result.
5. For data-loss risk, stop reproducing on irreplaceable media and use disposable test files.

Security vulnerabilities should follow [`SECURITY.md`](SECURITY.md) and should not be disclosed in a
public issue before remediation.

## Development setup

```bash
git lfs install
git clone https://github.com/danyalwg/VaultGallery.git
cd VaultGallery
git lfs pull
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Requirements are JDK 17, Android SDK 35 and an ARM64 Android 10+ device for meaningful runtime
testing.

## Engineering expectations

- Preserve user data and Android storage semantics.
- Never delete a move source before the destination is durably verified.
- Keep public and Secure Gallery behavior conceptually aligned.
- Do not weaken authentication, encryption or URI-grant boundaries to simplify a feature.
- Keep disk, database, decode, inference and encoding work off the main thread.
- Use bounded real-time previews and exact full-resolution output.
- Provide cancellation and honest progress for long-running work.
- Do not add inert controls, fabricated AI claims or success messages before completion.
- Keep user-facing terminology consistent across menus, viewers and editors.
- Add tests for logic and device-level contracts touched by the change.
- Update privacy, security, architecture and third-party notices when behavior changes.

## Pull-request checklist

- [ ] The change has one clear purpose.
- [ ] The app compiles with the Gradle wrapper.
- [ ] JVM tests pass.
- [ ] Android lint has no new errors.
- [ ] Relevant device/instrumentation checks pass.
- [ ] Public and Secure Gallery parity was considered.
- [ ] Media loss, duplication, partial transfer and cancellation were considered.
- [ ] Accessibility, font scale, RTL and reduced motion were considered.
- [ ] No private media, secrets, signing files or device dumps were added.
- [ ] Third-party code/model provenance and licence terms are documented.
- [ ] User-facing docs are updated.

## Commit style

Use concise imperative subjects, for example:

```text
fix: preserve playback state during precise seeking
feat: add cancellable secure export report
docs: explain Android default-viewer limitations
test: cover album-context viewer ordering
```

## Source terms

By submitting a contribution, you represent that you have the right to submit it and authorize the
repository owner to use, modify and distribute it as part of Vault Gallery. The public repository is
source-available under [`SOURCE_LICENSE.md`](SOURCE_LICENSE.md), not an automatic open-source grant.
