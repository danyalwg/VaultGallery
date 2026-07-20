# Secure File Format

Status: version 1 is implemented. It uses independently authenticated chunks so secure video can seek without a plaintext video file.

## Binary layout

All integers are big-endian. The implemented preamble contains:

| Field | Size | Decision |
|---|---:|---|
| Magic | 8 bytes | ASCII `VLTGAL01` |
| Format version | 4 bytes | `1` |
| Chunk size | 4 bytes | 1 MiB; reader validates 64 KiB to 4 MiB |
| Nonce prefix | 8 bytes | Cryptographically random per file/key |

Each data record contains `plaintextLength` (4 bytes), ciphertext, and a 16-byte GCM tag. Record order defines the zero-based chunk index. The nonce is the random 8-byte prefix concatenated with that 32-bit index. The per-object AES-256 key is derived from the random vault master key and random UUID object ID using HMAC-SHA-256 domain separation. Chunk index, length, and magic are authenticated as additional data.

## Metadata and keys

Metadata resides in a separate AES-256-GCM encrypted JSON index. Physical filenames contain only random UUID object IDs. Names and MIME types never appear in the media header or physical filename.

## Reading and seeking

Readers validate magic, version, and length limits and verify each requested chunk before releasing bytes. The Media3 data source scans record boundaries once, then seeks directly to the required authenticated chunk. It holds one decrypted chunk and clears it on close.

## Failure and migration

Authentication, truncation, or bounds failures stop the read; no unauthenticated plaintext is accepted. Unsupported versions are preserved and reported. A future migration must write a temporary object, verify it, commit the encrypted index, and only then retire the old object.
