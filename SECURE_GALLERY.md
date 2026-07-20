# Secure Gallery Architecture

Secure Gallery is an application-level encrypted vault, not an operating-system container. It protects app-managed data at rest and obscures authenticated screens, but it cannot defend against a rooted/compromised OS, memory inspection by privileged malware, or a physical camera.

## Authentication state machine

First use creates a six-or-more digit PIN or alphanumeric passphrase and chooses no recovery or a one-time high-entropy recovery key. Biometrics/device credential may be convenience unlock methods only after the vault exists. Runtime state is `Uninitialized`, `Locked`, `Authenticating`, `Unlocked`, `KeyInvalidated`, or `ResetRequired`. Failed attempts are rate-limited with increasing delay; no insecure reset exists.

The session locks immediately on process restart, screen off, explicit lock, authentication invalidation, and background transition by default. Configurable grace periods are 30 seconds, 1, 5, or 15 minutes. A non-sensitive overlay is applied before secure activity content can be captured during lifecycle transitions.

## Isolation

Secure media, database, thumbnails, search indexes, caches, edit projects, journals, and temporary exports have separate app-private/no-backup roots. Random object IDs are used physically. Secure content, names, thumbnails, navigation destinations, and notification details are unavailable until authentication succeeds. Normal repository/cache types cannot accept secure objects.

## Screen protection

Every secure window uses `FLAG_SECURE`, disables task snapshots where supported, provides a neutral recent-task preview and label, and replaces content immediately on pause. No secure widgets, shortcuts, thumbnail notifications, or public cache entries are allowed.

## Analysis, backup, and reset

OCR, labels, people, and place grouping are off or on-device by explicit setting; their encrypted derived data can be deleted/rebuilt. Ordinary Android backup is disabled for keys and sensitive metadata. Cloud backup is absent until a separately reviewed ciphertext-only recovery design exists. Reset requires authentication, typed confirmation, final warning, and acknowledgement that key destruction makes vault data unrecoverable.
