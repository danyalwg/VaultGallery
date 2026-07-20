# Import Transactions

## Persistent state machine

Jobs move through `Planned → Estimating → Ready → Running ↔ Paused → Cancelling → CompletedWithReport` or `NeedsAttention`. Items move through `Discovered → Reading → EncryptingTemp → Verifying → Committing → SecureReadable → RequestingSourceDelete → VerifyingSourceDelete → Terminal`. State changes and object renames are journaled and idempotent.

## Copy

Authenticate; enumerate selections/folder trees without loading bytes; estimate plaintext, encryption, thumbnail, database, and temporary overhead; persist the manifest; stream source to a random temporary encrypted object while hashing; flush; reopen and authenticate/decrypt samples or full content as policy requires; compare source checksum; commit encrypted metadata and final object atomically; confirm the secure repository can open it; leave the source unchanged; report each result.

## Move

Move performs the entire copy transaction first. Only after the secure object opens does it request Android deletion approval. Denial/failure produces `CopiedSourceNotDeleted`, keeps the secure copy, explains the duplicate, and offers retry. Source deletion is re-queried before `Moved`. No path deletes first.

## Folder preservation and conflicts

SAF traversal records encrypted logical parent IDs and names while physical objects remain random. Exact duplicates use SHA-256 and offer skip/keep both/metadata-only. Same logical name with different bytes offers adjusted name/replace/import elsewhere/skip. Replace retains the old secure item until the new one verifies and the database transaction commits.

## Pause, cancel, crash, and storage pressure

Workers stream one bounded buffer and checkpoint between chunks/items. Pause/cancel stops at an authenticated boundary, removes incomplete temporary objects, and preserves verified commits. Startup reconciles manifests, journals, temp objects, and database pointers. Storage is estimated up front and rechecked periodically; insufficient space stops safely with a partial report. Source grant loss pauses with a recoverable permission error.

## Diagnostics

Progress exposes item/byte totals when known, current item number without its private name in logs, pause/resume/cancel, retries, and non-sensitive codes. Reports list success/skipped/failure to the authenticated UI; production telemetry never contains names, paths, URIs, hashes, metadata, or keys.
