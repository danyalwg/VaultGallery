# Security Design

Vault Gallery provides application-level encryption and authenticated access for its Secure Gallery. It is not a system container and cannot isolate data from a compromised/rooted operating system or guarantee protection while an authorized user is actively viewing content.

## Controls

- Per-vault random master key wrapped by Android Keystore; optional recovery envelope; fresh per-file keys.
- Memory-hard Argon2id credential derivation with unique salt, versioned calibrated parameters, constant-time verification, and attempt delays.
- AES-256-GCM authenticated, independently seekable chunks with authenticated headers/metadata and unique nonces.
- AES-GCM encrypted secure metadata index with physically isolated media and temporary share files. Dedicated encrypted thumbnail and derived-data stores are not yet present.
- Secure video playback uses a random-name plaintext file only inside the app-private cache while the authenticated viewer is open. It is deleted when playback closes; abandoned files are purged when the vault service starts or when the user clears temporary files.
- Authentication-gated navigation, lifecycle locking, optional `FLAG_SECURE` screen-capture protection (off by default), generic notifications, and configurable auto-lock.
- Scoped storage, non-exported components by default, least-privilege FileProvider grants, TLS for any future network traffic, and no cloud upload by default.
- Transactional writes: temp → stream/hash → flush → verify → metadata commit → optional Android-approved source deletion.

## Secure coding rules

Cryptographic primitives come from Android or audited maintained libraries. Parsers validate magic, versions, lengths, counts, allocation ceilings, MIME signatures, and authentication before use. No sensitive production logs or crash fields. No hardcoded passwords, keys, test credentials, broad storage access, plaintext secure filenames, public secure cache, or authentication bypass.

## Reporting a vulnerability

Do not publish vault bypasses, key-handling flaws, provider authorization defects or destructive
media-loss issues in a public issue before the maintainer can assess them. Use GitHub's private
vulnerability-reporting surface for this repository when available. Never include real vault keys,
PINs, private media, contact databases or unredacted device logs.

Include the affected commit/version, Android/device context, minimal synthetic reproduction,
security impact and whether exploitation requires an already-unlocked device.

## Incident handling

Security reports will receive a non-sensitive tracking ID, affected version range, severity, containment plan, disclosure timeline, and remediation verification. Users are notified when confidentiality or recoverability may be affected. Diagnostics must never request users to send vault keys, credentials, or decrypted media.

## Release checklist

Review manifest exports, backup configuration, dependency advisories/licences, ProGuard/R8 behavior, debug/release isolation, URI grants, logging, lock transitions, screenshot/recents behavior, cryptographic test vectors, metadata/file migrations, temp cleanup, and reset/key invalidation before a production release.
