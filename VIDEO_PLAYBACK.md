# Video playback

AndroidX Media3 powers public and secure video playback. Compose renders the viewer, transport,
filmstrip and metadata surfaces.

## Behavior

- Tap-to-show/hide controls
- Explicit play/pause
- Consistent mute state
- Real-time scrubbing with exact release position
- Playback state preserved through seeking
- Elapsed/total display
- Centre-anchored media filmstrip
- Photo/video distinction and video duration in the filmstrip
- Viewer zoom and orientation control
- Snapshot, details, edit, share and external-player actions
- Next/previous media paging inside the launch collection

## Secure playback

Locked-only files use a private file-backed source. Encrypted files use an authenticated seekable
data source exposed through the secure provider only inside authorized flows. Playback does not add
the media to the public Android library.

An approved external app can access only supported locked-only media; encrypted content requires an
authenticated export/share path.
