# Architecture

Vault Gallery Version 1 is a Kotlin/Jetpack Compose Android application targeting API 35 and modern
ARM64 flagship phones. It has two launcher surfaces—public Gallery and Secure Gallery—sharing a
coherent design language and domain concepts while retaining separate storage and authentication
boundaries.

## Modules

| Module | Purpose |
|---|---|
| `app` | Android activities, application wiring, platform contracts and mature feature implementations |
| `design-system` | Shared colors, typography, spacing, shape and motion tokens |
| `database` | Persistent index/database boundary |
| `security` | Authentication, secure-storage and access-control boundary |
| `gallery` | Collection and album domain boundary |
| `viewer` | Viewer, zoom, paging and playback domain boundary |
| `editor` | Photo/video editing domain boundary |
| `creation` | Collage, GIF, movie and generated-media boundary |
| `transfer` | Durable import/export/copy/move boundary |
| `search` | Search, smart organization and index boundary |
| `ai` | Replaceable local model/runtime boundary |

The modules establish dependency direction, but extraction is intentionally incremental. Several
device-proven implementations remain in `app` until moving them can preserve behavior and test
coverage.

## Public Gallery

`MainGalleryActivity` integrates Android MediaStore, system intent contracts and public navigation.
MediaStore content URIs—not absolute filesystem paths—identify public media. Android owns dangerous
mutation approvals. The gallery remembers user presentation state but treats MediaStore as the
library source of truth.

The app accepts supported external `VIEW`, `EDIT`, camera review, collection and legacy picker
intents. Picker mode is bounded: it clears stale viewer/editor/navigation state, limits media kind to
the caller’s MIME request and returns URI grants without exposing ordinary destructive gallery
actions.

## Secure Gallery

`SecureGalleryActivity` is a separate task with locked/authenticating/unlocked states. Secure media
may be encrypted or locked-only. It never becomes public MediaStore content merely because it is
visible inside Secure Gallery.

- Encrypted files use authenticated storage and seekable Media3 data sources.
- Locked-only files remain ordinary bytes but stay outside public media scanning.
- A per-app provider/access list mediates approved locked-only external playback.
- The PIN, biometric policy and vault-key envelopes are separate from media payload identity.
- Screenshot protection is an optional setting and is off by default.

## State and concurrency

Compose renders observable state and emits user intent. ViewModels own public and secure UI state.
File I/O, hashing, database work, decoding, inference and encoding run away from the main thread.

Short interactive work uses replacement-cancelled coroutines and bounded previews. Durable media
operations use WorkManager with persisted operation identity, foreground progress and explicit
pause/resume/cancel behavior.

## Transfer invariant

A true move is a verified copy followed by deletion:

1. Discover and record source context.
2. Write a destination temporary object.
3. Flush and verify bytes/metadata.
4. Commit the destination.
5. Confirm the destination is readable.
6. Obtain Android approval if required.
7. Delete the source.
8. Re-query and report the final state.

Cancellation or failure before destination verification leaves the source intact.

## Media pipelines

### Viewer

The viewer receives an ordered media context from its launch surface. Media paging, zoom, information
swipe and centre-filmstrip state are coordinated so an album viewer does not silently become a global
library viewer.

### Editing

Editors separate interaction resolution from output resolution. Direct manipulation is rendered with
GPU transforms or bounded previews; full-resolution render/encode occurs for Apply/Save. Source
replacement retains the original until the output can be opened and indexed.

### AI-assisted processing

AI runtimes are explicit and replaceable. Model metadata records file, source, checksum, runtime and
distribution status. Interactive selection, OCR, segmentation, visual descriptors and neural
inpainting are local in Version 1.

## Android configuration

The Compose design system uses `sp` and the system font family, so Android font scale and weight
adjustment flow into typography. Window and Compose configuration inherit display scale, locale and
RTL direction. Motion tokens consult Android’s animator scale and collapse transitions when system
animations are disabled.

## Build variants

Debug adds `.debug` to the application ID and appends `-debug` to the visible version. Release builds
are minified and resource-shrunk but remain unsigned until a protected external signing configuration
is supplied. Keystores and signing properties are intentionally ignored.
