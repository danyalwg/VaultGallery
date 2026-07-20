# MediaStore Plan

## Query strategy

The public repository queries `MediaStore.Images`, `MediaStore.Video`, or `MediaStore.Files` through `ContentResolver` with an API-specific projection. Results are ordered by normalized capture time then ID descending. Paging keys use `(sortTime, id)` rather than offsets so inserts do not destabilize pages. Columns are requested only when present on the running API.

## Change detection

A `ContentObserver` debounces changes and schedules incremental reconciliation. The index tracks generation/version where Android exposes it; otherwise it compares IDs and modification times within affected buckets. Boot, app upgrade, permission expansion/revocation, removable-volume changes, and failed observers trigger bounded reconciliation. Unchanged files are not rescanned or rehashed.

## Thumbnails and metadata

Grid requests use platform thumbnails or size-bounded Coil decoding keyed by URI, modification time, and target size. Full-resolution decoding is prohibited in grids. EXIF and media metadata are lazy, cancellable, off-main-thread, and guarded against malformed input/decompression bombs. Video duration/codec errors produce placeholders rather than aborting a page.

## Mutations

Writes, edits, trash, restore, and deletion use pending items and Android-approved `RecoverableSecurityException`, `createWriteRequest`, `createTrashRequest`, and `createDeleteRequest` flows as appropriate. The UI never reports success before the resolver state is re-read. Partial success is itemized. Delete-first moves are forbidden.

## Scope and scale

The engine supports primary/removable volumes, partial photo access, revoked grants, 100,000+ rows, cancellation, memory pressure, and restoration of list position by stable media ID. Secure media is never inserted into or observed through MediaStore.
