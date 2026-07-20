# Privacy

Vault Gallery is local-first. Browsing public media requires no account. Secure content, thumbnails, filenames, logical folders, OCR, labels, face data, locations, and embeddings are not uploaded by default.

## Data use

Public access is limited to media categories the user grants. Secure imports are deliberate copy/move actions into app-private encrypted storage. OCR/labels/people/place analysis is on-device and separately controllable; derived data can be deleted and rebuilt. Location is not reverse-geocoded remotely without explicit consent.

## Sharing and export

Sharing secure content creates a temporary decrypted copy after recent authentication and a clear warning. Users can strip location, device EXIF, captions, and tags. Temporary files use private storage, narrow read grants, expiry, and cleanup journals. Recipient apps may retain intentionally shared plaintext, which is outside this app's control.

## Telemetry and diagnostics

Analytics and crash reporting are off until explicit consent and implementation. Production diagnostics use allowlisted non-sensitive event/error codes. They never contain filenames, folders, URIs, EXIF, locations, OCR, face data, credentials, keys/nonces, recipients, or cloud tokens.

## Retention and deletion

Public deletion follows Android system approval and MediaStore trash behavior. Secure trash defaults to 30 days and remains encrypted. Permanent secure deletion removes encrypted objects, metadata, thumbnails, edit/derived data, and key references; flash-storage forensic erasure cannot be guaranteed. Vault reset destroys keys and encrypted records after authentication and escalating confirmation.

## User controls

The app will expose permission scope, analysis toggles, derived-data deletion, location removal, crash/analytics consent, trash retention, temporary cleanup, launcher visibility, encrypted backup status, and complete vault reset. A published privacy policy must match the shipped build and identify any future third-party processors before activation.
