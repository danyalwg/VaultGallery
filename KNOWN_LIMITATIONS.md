# Known Limitations

Vault Gallery 2.0 is a substantial personal-use debug build, not a Play Store production release.

## Implemented

- Two distinct Gallery and Secure Gallery experiences, with an optional hideable Secure Gallery launcher icon.
- Public MediaStore permission flow, date timeline, configurable 3/4/5-column grids, albums, search, zoom, Media3 video playback, external-player routing, local monthly stories, and responsive dark UI.
- Samsung-style long-press slide selection across the timeline and album grids, including inclusive range add/remove and edge auto-scroll.
- Android system favourites and recycle-bin operations: add/remove favourites, move to trash, browse trash, restore, empty, and permanently delete through platform approval dialogs.
- Sharing, copy-to-Secure-Gallery, image/video viewing, and deliberate confirmation for destructive viewer actions.
- Secure PIN/passphrase setup, Argon2id, persisted increasing unlock delays, credential changes, configurable background auto-lock, and real Android biometric unlock.
- A random vault master key wrapped independently by passphrase and a biometric-authenticated Android Keystore key; biometric enrollment changes invalidate the biometric key.
- Per-object derived keys, AES-256-GCM chunk encryption, encrypted metadata, Photo Picker imports, encrypted image viewing, seekable encrypted Media3 video playback, and explicit decrypted sharing.
- Encrypted 30-day Secure Gallery recycle bin with restore, empty, and permanent-delete controls; screenshot protection and disabled Android backup.

## Not yet implemented

- Recovery keys, encrypted device-to-device backup/migration, durable WorkManager import queues, resumable large imports, folder-tree imports, duplicate detection, or transactional deletion of the public source after a secure copy.
- Encrypted thumbnail caches; secure image thumbnails are decrypted on demand while the vault is unlocked.
- Full location-map metadata, network-backed shared albums, people/face grouping, OCR, semantic labels, cloud sync, and Samsung's proprietary Galaxy AI services.
- Built-in pixel-level photo/video editing and final GIF/collage/movie rendering; the current build routes media to compatible Android editors and providers where possible.
- Samsung account/network features, vendor-specific motion-photo playback, RAW workflows, Chromecast, or cross-device continuity.
- Full tablet/foldable layouts, performance benchmarks for extremely large libraries, release signing, store packaging, accessibility certification, and a complete physical-device matrix.

## Security boundary

Secure Gallery is application-level encrypted storage, not an operating-system container. Rooted or compromised devices, privileged memory inspection, malicious accessibility services, OS/vendor capture defects, and physical cameras remain outside its reliable boundary. Shared plaintext can be retained by recipients. Flash wear leveling prevents guaranteed forensic overwrite.

The APK is debug-signed but has no visual debug watermark. It is suitable for evaluation and careful personal testing, but irreplaceable private media should remain backed up until migration, recovery, and physical-device testing are completed.
