# Storage and permissions

## Public media

- API 33+: `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` according to the requested scope.
- API 34+: selected visual-media access is honored.
- Older supported Android versions use the legacy read permission allowed by the OS.
- Public edits/deletes use Android approval requests when Vault Gallery does not own the item.
- Android Photo Picker is preferred for user-selected additions/shares where appropriate.

## Persistent Secure Gallery storage

The sideloaded flagship build requests broad file-management access because its vault payload is
stored in a user-owned Documents location designed to survive APK uninstall. This permission is
powerful and would require redesign/policy review before an ordinary Play Store listing. The UI
provides a dedicated system-settings path when persistent storage access is missing.

## Other permissions

- Contacts: optional display-name resolution for conversation-oriented WhatsApp organization.
- Notifications and foreground data sync: visible durable transfer progress and controls.
- Biometrics: native Secure Gallery unlock.
- Wallpaper: explicit set-as-wallpaper action.
- Record audio: user-triggered creation/editing paths that need audio input.

## URI safety

Public media uses `ContentResolver` and content URIs. Secure sharing uses narrow URI grants. Persisted
grants are treated as revocable, and loss of access becomes a recoverable error rather than a reason
to delete source data.
