# Video Playback

AndroidX Media3 is the playback engine for public and secure video. A lifecycle-aware controller owns ExoPlayer; composables render state and controls only.

## Public playback

Content URIs feed the default Media3 data source. Controls include play/pause, scrub and preview, position/duration, double-tap seek, speed, volume/mute, fullscreen, rotation, picture-in-picture, loop, resume position, subtitle and audio-track selection. HDR/slow-motion behavior is enabled only when media metadata and device codecs support it. Decoder failures identify unsupported format without crashing the gallery.

## Secure playback

A custom read-only Media3 `DataSource` authenticates the secure header, maps requested positions to encrypted chunks, decrypts only required chunks, and exposes seekable reads. The cache is bounded, memory-only, cleared on stop/lock/memory pressure, and never shared with public playback. Authentication failure stops playback and marks integrity failure. Secure playback never writes a public plaintext file.

## Editing exception

If a codec/editor requires random-access plaintext, the user reauthenticates and a file is created only under private no-backup temporary storage, covered by `FLAG_SECURE` and a cleanup journal. It is deleted after export/cancel/failure/timeout, on startup, and after reboot where scheduling permits.

## Testing and performance

Tests cover seek across chunks, final partial chunk, pause/resume, rotation, PiP, subtitles, audio tracks, HDR capability gates, corrupt headers/tags, wrong keys, cache clearing, process death, unsupported codecs, and a 4K long-duration synthetic asset. Benchmarks record first frame, repeated seek latency, memory ceiling, and battery/thermal throttling behavior.
