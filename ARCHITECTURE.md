# Architecture

## Decision summary

Vault Gallery is a Kotlin, Jetpack Compose Android application with two launcher activities and separated public and secure data paths. The build targets API 35, supports API 28+, and uses Java 17 bytecode.

## Module boundaries

The delivered APK currently uses one Gradle `app` module with package boundaries for UI, public data, shared logic, and security. This is a recorded compromise for the installable milestone. Splitting these packages into the planned Gradle modules remains required before a large-team production release.

Every feature uses presentation, domain, and data packages. Composables render immutable UI state and emit events. ViewModels invoke domain use cases. Repositories own storage and service coordination. File I/O, MediaStore queries, database work, media decoding, and cryptography never run in composables or on the main thread.

## Runtime composition

- `MainGalleryActivity` hosts public navigation and never requires authentication.
- `SecureGalleryActivity` has a separate task identity. Its root state machine permits only `Locked`, `Authenticating`, or `Unlocked`; secure destinations cannot be restored while locked.
- Android ViewModels own public and secure state independently. Public MediaStore access and secure encrypted storage have separate implementations.
- Compose state routes between screens. Secure content branches render only while the in-memory master key is available.
- Secure media, encrypted metadata, and temporary shares use app-private roots and never enter MediaStore.

## Dependency direction

Feature presentation depends on feature domain contracts and shared design/common modules. Feature data implementations depend on Android adapters in core modules. Core modules never depend on feature UI. Public repositories never accept a security-mode flag; secure repositories are separate types.

## Concurrency and recovery

Coroutines and StateFlow expose observable state. Public loading and secure imports run off the main thread. Durable WorkManager import manifests, pause/resume, and crash recovery remain planned and are listed as limitations.

## Adaptive UI

Phones use edge-to-edge bottom navigation; expanded widths use a navigation rail. Grids derive columns from available width and minimum cell width. Window insets, posture, orientation, font scale, keyboard focus, TalkBack, and reduced-motion settings are first-class inputs.

## Build variants

Debug uses the `.debug` application ID suffix and separate app-private storage. It will visibly identify itself in Phase 1 and can never open release vault files. Release signing configuration remains external and is not committed.

## Architectural decision records to add

Phase 1 will add ADRs for module granularity, screenshot tooling, and font choice. Phase 3 will add ADRs for password KDF, encrypted database strategy, secure file cipher suite, key invalidation, and biometric policy after device benchmarks and security tests validate the choices.
