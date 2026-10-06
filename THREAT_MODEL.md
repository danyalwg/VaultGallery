# Threat Model

## Assets and trust boundary

Assets are secure media, thumbnails, metadata, derived indexes, edit projects, credentials, recovery material, keys, share exports, and public-gallery privacy metadata. The application boundary includes its process, app-private storage, Android Keystore, configured content providers, and authenticated UI. Android OS, device firmware, hardware, user-approved recipient apps, and cloud providers are external trust dependencies.

| Threat | Planned mitigation | Residual/out-of-bound risk |
|---|---|---|
| Lost unlocked device | Immediate/background lock by default, inactivity timeout, explicit lock, secure overlay | Media visible during a legitimately active unlocked session; physical observation |
| Lost locked device | Keystore-wrapped VMK, memory-hard credential KDF, rate limits, encrypted data/metadata | Weak user PIN, OS/hardware exploit, unlocked bootloader |
| Malicious application | App-private storage, scoped URI grants, non-exported components, no browse provider | Root/accessibility abuse, OS vulnerabilities, user-approved sharing |
| Clipboard leakage | No automatic secure clipboard writes; warn/auto-clear when a text feature explicitly copies | Clipboard managers and OS versions may retain data |
| Share URI leakage | Narrow FileProvider roots, random names, read-only temporary grants, expiry/startup cleanup | Recipient may persist plaintext after intentional share |
| Screenshot leakage | `FLAG_SECURE`, neutral recents preview, pre-pause obscuring | Root, vendor bugs, physical camera, external capture hardware |
| Recent-app leakage | Disable snapshots where supported; neutral preview and task label | OS/vendor defects |
| Notification leakage | Generic progress only; no names/thumbnails/paths | OS can reveal app identity and operation timing |
| Crash-report leakage | Redaction allowlist, no sensitive breadcrumbs, opt-in diagnostics | Third-party SDK defects; memory dumps on compromised devices |
| Log leakage | Structured non-sensitive codes; release logging allowlist | Rooted log access; developer mistakes guarded by tests/review |
| Backup leakage | `allowBackup=false`, no-backup key/temp roots, secure backup absent by default | OEM/root backup mechanisms outside Android contract |
| Rooted device | Integrity warning where feasible; keep at-rest encryption and minimize plaintext | Root can inspect process memory, input, screen, or Keystore use |
| Debug build | `.debug` ID/storage/aliases, visible DEBUG, no auth bypass/test credentials | Debugger can inspect debug process; debug build is not production-secure |
| Memory scraping | Short-lived keys/buffers, bounded caches, clear on lock, no full-video decrypt | Managed-runtime copies and privileged memory access cannot be eliminated |
| Temporary-file recovery | Private no-backup directory, encrypted-by-default, journaled deletion, startup/reboot cleanup | Flash wear leveling prevents guaranteed forensic erasure |
| Database theft | SQLCipher-style database encryption plus field AEAD for sensitive values, keys separate | Unlocked process/root can query decrypted state |
| Brute-force authentication | Argon2id, per-vault salt, increasing delay, biometric/platform throttling | Low-entropy PIN may fall to offline attack if all protection layers fail |
| Biometric enrollment changes | Keystore invalidation policy, lock and recovery flow | Device/vendor biometric compromise |
| Corrupt encrypted files | AEAD per header/chunk, bounds checks, quarantine, no partial-valid claim | Corruption may be unrecoverable without user backup |
| Key invalidation | Explicit state, recovery key when configured, no insecure bypass | Without recovery, secure data is permanently inaccessible |
| Cloud compromise | No secure cloud by default; future design is client-encrypted ciphertext with no server keys | Traffic/size/timing metadata and recovery-key loss |

## Abuse and failure cases

Imports treat media as hostile: MIME sniffing, allocation/decompression limits, parser isolation where available, and one-item failure containment. Export/share requires recent authentication and explicit disclosure that plaintext leaves the vault. Reset and permanent deletion require escalating confirmation; forensic erasure is never promised.

## Verification cadence

Security unit/instrumented tests and crypto-format review accompany format and key-management changes. Each release rechecks exported components, backup rules, logs, URI grants, dependency advisories, lock lifecycle, optional screenshot behavior, temporary cleanup, and debug/release isolation.
