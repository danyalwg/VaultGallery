# Known Limitations

## Phase 0 status

This repository currently contains a build scaffold and planning documents only. It has no gallery UI, launchable activity, media indexing, editor, authentication, encryption, import/export, search, trash, cloud, or user data handling. Phase 0 must not be evaluated as a working gallery.

## Product security boundary

- Secure Gallery is app-level encrypted storage, not an operating-system secure container.
- Rooted devices, compromised firmware/OS, privileged memory inspection, malicious accessibility services, and physical cameras are outside the reliable protection boundary.
- `FLAG_SECURE` and recents controls depend on Android/OEM behavior.
- Flash wear leveling means deletion cannot promise forensic overwrite; cryptographic key destruction is the primary protection.
- Forgotten credentials are unrecoverable unless the user previously saved an optional recovery key.
- A recipient can retain plaintext intentionally exported or shared by the user.

## Platform/media limits

Codec, HDR, RAW, biometric, removable-storage, PiP, write/delete, and partial-access behavior varies by API/device/provider. Unsupported features will be hidden or explained, never claimed. Public operations remain subject to Android user approvals. Large/corrupt/non-seekable media may be rejected safely.

## Deferred decisions

The exact OFL font, screenshot library, Argon2id provider/parameters, encrypted Room integration, editing codec matrix, OCR/label engines, cloud provider, and encrypted-backup transport require implementation-phase evaluation and testing. No cloud or Studio row will appear before real functionality exists.
