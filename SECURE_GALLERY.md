# Secure Gallery

Secure Gallery is an application-level private gallery with the same core browsing, album, viewer,
editing, creation, sorting, search and metadata concepts as the public gallery. It is not an Android
system container and does not claim protection from a rooted or compromised operating system.

## Authentication

- Four-digit numeric PIN.
- Unlock submits automatically after the correct fourth digit.
- Native Android biometric prompt when enabled.
- PIN remains the recovery credential for biometric convenience unlock.
- Explicit lock and configurable background timeout.
- Unlock policy can permit biometric/PIN, prefer biometric, or require PIN.

## Screen capture

Screenshot/screen-recording protection is optional and off by default. Enabling it adds Android’s
secure-window flag while Secure Gallery is visible. Disabling it allows the user to capture the
screen and accepts the resulting privacy risk.

## Storage modes

### Encrypted

Media is authenticated and encrypted at rest. Playback uses a seekable authenticated Media3 data
source so normal video navigation does not require publishing the file to MediaStore.

### Locked only

Media remains ordinary bytes in the persistent vault area. The app gate, `.nomedia` and provider
policy hide it from normal gallery indexing, but the bytes are not cryptographically protected from
filesystem access. Approved external apps may receive narrow access.

## Persistent location

The vault payload lives under `Documents/VaultGallery/SecureVault`. This is intentionally outside the
APK’s automatically deleted private-data area so uninstall does not intentionally erase the media.
Uninstall still removes app-private preferences; recovery material and an independent backup remain
mandatory.

## Transfers

Imports and exports use durable background operations. Album names are retained automatically;
same-name destinations merge and missing destinations are created. Move deletes the source only after
the destination is committed and verified.

## External applications

Encrypted files are not directly usable by an ordinary external player. Locked-only mode supports a
user-controlled application allow-list through `SecureMediaProvider`. Grants are scoped to approved
flows; approved recipients may retain media they can read.

## Threat boundary

See [THREAT_MODEL.md](THREAT_MODEL.md), [KEY_MANAGEMENT.md](KEY_MANAGEMENT.md),
[SECURE_FILE_FORMAT.md](SECURE_FILE_FORMAT.md) and [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md).
