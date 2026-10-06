# Key Management

## Hierarchy

1. Generate a random 256-bit vault master key (VMK) with `SecureRandom`.
2. Generate a non-exportable Android Keystore AES-256 key to wrap the VMK. User-authentication validity matches the chosen lock policy; biometric enrollment invalidation behavior is explicit and tested.
3. If recovery is enabled, create an independent high-entropy recovery key displayed once. A recovery envelope wraps the VMK; it is never uploaded by default.
4. For PIN verification/recovery wrapping, derive a key with Argon2id using a random 16-byte-or-longer salt, versioned parameters, constant-time verification and the parameters encoded by the envelope. Parameter migrations must retain read compatibility until recovery has been proven.
5. Generate a fresh random 256-bit data-encryption key per media object. Wrap it under a VMK-derived wrapping key using authenticated encryption and domain-separated HKDF keys.

PIN/passphrase bytes are held only as long as derivation requires and cleared where the runtime allows. They are never media keys, logs, analytics, preferences, or backups.

## Rotation and invalidation

Changing PIN/passphrase rewrites only the authentication/recovery envelope, not every media file. Rotating VMK rewraps per-file keys transactionally. Keystore invalidation moves to `KeyInvalidated`; recovery is offered only when previously configured. Without recovery, reset destroys ciphertext and metadata keys after explicit confirmation.

## Storage and access

Wrapped keys and versioned parameters live in app-private no-backup storage. Raw VMK/DEKs exist only in bounded memory while unlocked and are cleared on lock. Debug and release use different application IDs and Keystore aliases. Keys never depend on device identifiers, filenames, or fixed nonces.

## Verification

Tests cover nonce uniqueness, envelope parsing, wrong PIN/passphrase, rate limiting, biometric cancel/lockout/enrollment change, key invalidation, recovery success/failure, interrupted rewrap, debug/release isolation, and corrupted key records. Cryptographic primitives are not custom implemented; format orchestration is app code over platform/audited libraries.
