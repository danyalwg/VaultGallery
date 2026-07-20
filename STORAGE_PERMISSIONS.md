# Storage and Permissions

## API policy

- API 33+: request `READ_MEDIA_IMAGES` and/or `READ_MEDIA_VIDEO` only after an explanation; use the system Photo Picker for user-chosen imports.
- API 34+: honor selected-photo access and detect changes without presenting inaccessible items.
- API 28–32: request the narrow legacy read permission required by the OS; scoped storage rules apply where available.
- Folder trees and removable storage use `ACTION_OPEN_DOCUMENT_TREE` with persisted URI grants.
- Modifications and deletion always use platform approval requests when the app does not own the item.

`MANAGE_EXTERNAL_STORAGE` is not planned. Microphone is requested only inside voice-over recording. Location is requested only for an explicit location feature that truly needs current location; reading existing media coordinates does not justify it.

## UX states

Before a system dialog, explain the benefit and the exact data category. The public gallery supports `Loading`, `FullAccess`, `PartialAccess`, `Empty`, `Denied`, `PermanentlyDenied`, `Revoked`, and `Error`. Partial access shows an unobtrusive scope banner and a system-picker action; denial leaves import/picker and settings guidance available.

## URI safety

All streams are opened with `ContentResolver`; absolute public filesystem paths are never assumed. Persisted SAF grants are audited at startup. Grant loss pauses affected jobs with a recoverable error. Secure exports use a private `FileProvider` allowlist, temporary read grants, expiry journal, and startup cleanup.

## Testing

Instrumented coverage includes permission grant/deny/revoke, API 34 partial access, Photo Picker cancellation, removable media removal, read-only documents, user-denied delete/write, process death during approval, and rotation while a system request is pending.
