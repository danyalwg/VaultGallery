# Data Model

## Public catalog

`PublicMedia` mirrors shared-storage facts: `id: Long`, `contentUri: String`, `displayName`, `mimeType`, `mediaType`, `width`, `height`, `durationMs`, `sizeBytes`, `dateTaken`, `dateAdded`, `dateModified`, `relativePath`, `bucketId`, `bucketName`, optional latitude/longitude, orientation, favourite/trash flags, trash expiry, optional checksum and perceptual hash. URI and mutable MediaStore facts are refreshed from the platform; missing rows become tombstones before local cleanup.

`PublicAlbum` is either a physical bucket, virtual definition, smart query, merged presentation, or system collection. Merged albums retain constituent bucket IDs and paths. `PublicEditProject`, `PublicTag`, `Story`, and cross-reference tables use stable app IDs and never duplicate original bytes.

## Secure catalog

`SecureMedia` uses a random `secureId` and random physical object ID. Sensitive values—original name/extension, logical path, MIME type, full metadata, thumbnail/edit IDs, checksum/perceptual hash, import source, location, OCR, labels, tags, and album relationships—are authenticated ciphertext. Operational fields kept clear only when required are version, chunk sizing/count, key reference, import state, logical sort date rounded to the minimum useful precision, trash state/expiry, and integrity state. The Phase 3 review may encrypt additional operational fields if query benchmarks allow.

`SecureAlbum`, `SecureFolderNode`, `SecureEditProject`, `SecureDerivedData`, and `SecureTrashRecord` live only in the encrypted secure database. Logical folders use parent IDs plus encrypted display names; physical storage never mirrors the logical tree.

## Transaction entities

`TransferJob` records job ID, type, source grant, destination, totals, processed counters, lifecycle state, policy choices, creation/update times, and a non-sensitive diagnostic code. `TransferItem` records source locator only as protected job data, expected size/hash, temp object ID, final object ID, state, retry count, and deletion-request status. Terminal item states are `Copied`, `Moved`, `CopiedSourceNotDeleted`, `Skipped`, `Failed`, or `Cancelled`.

## State and migrations

Room migrations are explicit and tested from every shipped schema. Secure database migrations require authentication, backup the encrypted database inside app-private no-backup storage, verify the new schema, then remove the backup. No downgrade silently destroys data. Public source URIs are removed from secure records after a verified move unless a pending deletion-recovery record requires them.
