# Architecture

## Decision summary

Vault Gallery will be a Kotlin, Jetpack Compose Android application with two launcher activities and strictly separated public and secure data paths. The build targets API 35, supports API 28+, uses Java 17 bytecode, and is developed with the Android Studio bundled runtime. Phase 0 contains only the build scaffold and planning material.

## Module boundaries

The planned modules are `app`, `core-common`, `core-design`, `core-database`, `core-storage`, `core-media`, `core-playback`, `core-image`, `core-video`, `core-security`, `core-search`, `core-background`, the feature modules named in the master brief, `benchmark`, and `screenshot-tests`.

Every feature uses presentation, domain, and data packages. Composables render immutable UI state and emit events. ViewModels invoke domain use cases. Repositories own storage and service coordination. File I/O, MediaStore queries, database work, media decoding, and cryptography never run in composables or on the main thread.

## Runtime composition

- `MainGalleryActivity` hosts public navigation and never requires authentication.
- `SecureGalleryActivity` has a separate task identity. Its root state machine permits only `Locked`, `Authenticating`, or `Unlocked`; secure destinations cannot be restored while locked.
- Hilt supplies repositories and process-scoped coordinators. Public and secure bindings use distinct interfaces and qualifiers only where shared low-level primitives are safe.
- Navigation Compose owns screen routing. Secure routes are in a separate graph constructed only after authentication.
- Room databases, cache roots, WorkManager queues, thumbnails, and search indexes are physically separate for public and secure features.

## Dependency direction

Feature presentation depends on feature domain contracts and shared design/common modules. Feature data implementations depend on Android adapters in core modules. Core modules never depend on feature UI. Public repositories never accept a security-mode flag; secure repositories are separate types.

## Concurrency and recovery

Coroutines and Flow expose observable state. Paging 3 backs large public collections. Long operations are resumable WorkManager jobs with persistent manifests and idempotent state transitions. Cancellation occurs at verified item or chunk boundaries. Process death recovery replays journals rather than guessing from filenames.

## Adaptive UI

Phones use edge-to-edge bottom navigation; expanded widths use a navigation rail. Grids derive columns from available width and minimum cell width. Window insets, posture, orientation, font scale, keyboard focus, TalkBack, and reduced-motion settings are first-class inputs.

## Build variants

Debug uses the `.debug` application ID suffix and separate app-private storage. It will visibly identify itself in Phase 1 and can never open release vault files. Release signing configuration remains external and is not committed.

## Architectural decision records to add

Phase 1 will add ADRs for module granularity, screenshot tooling, and font choice. Phase 3 will add ADRs for password KDF, encrypted database strategy, secure file cipher suite, key invalidation, and biometric policy after device benchmarks and security tests validate the choices.
