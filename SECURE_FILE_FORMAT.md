# Secure File Format

Status: design for Phase 3 implementation and interoperability tests. Version 1 uses independently authenticated chunks so large video can seek without plaintext files.

## Binary layout

All integers are unsigned big-endian. The fixed preamble contains:

| Field | Size | Decision |
|---|---:|---|
| Magic | 8 bytes | ASCII `VLTGAL01` |
| Format version | 2 | `1` |
| Header length | 2 | Bounds checked before allocation |
| Flags | 4 | Media class and optional sections; unknown critical flags reject |
| File object ID | 16 | Random UUID bytes, not a filename |
| Chunk size | 4 | Default 1 MiB; validated range 64 KiB–4 MiB |
| Plaintext length | 8 | Encrypted metadata is authoritative; used for bounds only |
| Chunk count | 4 | Must equal ceiling(length/chunk size) |
| Nonce prefix | 8 | Cryptographically random per file/key |
| Key reference length + value | variable | Identifier for an encrypted DEK envelope, never raw key bytes |
| Metadata envelope length + value | variable | AES-256-GCM ciphertext and tag |
| Header authenticator | 16 | Authenticates every preceding header byte |

Each data record contains `chunkIndex` (4), `plaintextLength` (4), ciphertext, and a 16-byte GCM tag. Nonce is the 8-byte random prefix concatenated with the 32-bit chunk index. A fresh random 256-bit DEK and prefix are generated for every object, so nonce reuse with a key is structurally prevented. Header bytes and chunk index/length are additional authenticated data.

## Metadata and keys

Metadata is canonical CBOR with a schema version and encrypted under a metadata subkey derived from the file DEK using HKDF-SHA-256 with domain separation. The file DEK is wrapped by a vault wrapping key; the key reference selects its authenticated envelope. Filenames and logical paths never appear in the physical filename or clear header.

## Reading and seeking

Readers validate magic/version/length limits, authenticate the header, unwrap the DEK, and verify each requested chunk before releasing bytes. Fixed chunk sizing gives direct offsets; the final chunk uses its declared length. A bounded in-memory cache contains only authenticated plaintext chunks and is cleared on stop/lock/memory pressure.

## Failure and migration

Any tag, index, length, or ordering failure marks the object `IntegrityFailed`; no partial plaintext is shown as valid media. Unsupported newer versions are preserved and reported, never rewritten. Migration writes a new temporary object, verifies all chunks and metadata, atomically commits the database pointer, then retires the old object. Test vectors cover empty, one-chunk, multi-chunk, large-index, truncation, bit flips, reordered chunks, wrong keys, and unknown flags.
