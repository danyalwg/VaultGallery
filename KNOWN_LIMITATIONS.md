# Known Limitations

This APK is an installable functional milestone, not the complete production scope described by the master brief.

## Implemented

- Two launcher activities labelled Gallery and Secure Gallery.
- Public MediaStore permission flow, timeline, four-column thumbnails, date groups, albums, search, long-press selection, sharing, Android 11+ approved deletion, image zoom, and Media3 video playback.
- Albums, Stories empty state, Menu, persisted settings, dark Compose layout, accessibility labels, and visible DEBUG marking.
- Secure PIN/passphrase setup, Argon2id, increasing persisted unlock delays, Android Keystore device wrapping, random master key, per-object derived keys, AES-256-GCM chunks, encrypted metadata index, Photo Picker imports, encrypted image viewing, seekable encrypted Media3 video playback, explicit decrypted sharing, permanent delete/reset confirmation, screenshot protection, disabled backup, and lock on pause/process restart.

## Not yet implemented

- Biometrics, recovery key, change-passphrase, configurable lock delay, launcher hiding, secure recycle bin, encrypted thumbnails, durable WorkManager import queues, pause/resume, folder-tree hierarchy imports, duplicate policy, transactional source deletion after secure copy, and encrypted backup.
- Public trash browser, favourite mutation, location map/editing, shared albums, clean-out analysis, generated stories, OCR, labels, people grouping, cloud sync, advanced search, creative tools, photo/video editing, slideshow/GIF/movie creation, and benchmarks.
- Full Gradle module split, Room/Hilt, complete screenshot/device matrix, tablet navigation rail, and the full security test matrix.

## Security boundary

Secure Gallery is application-level encrypted storage, not an operating-system container. Rooted or compromised devices, privileged memory inspection, malicious accessibility services, OS/vendor capture defects, and physical cameras are outside its reliable boundary. Shared plaintext can be retained by recipients. Flash wear leveling prevents guaranteed forensic overwrite.

The APK is debug-signed and visibly marked DEBUG. It is suitable for evaluation and personal testing, not store publication or irreplaceable private media until the remaining recovery, migration, and device-matrix work is complete.
