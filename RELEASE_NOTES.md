# Vault Gallery — Version 1

Version 1 is the project’s only public release line. It consolidates the gallery, Secure Gallery,
viewers, editors, creation surfaces, on-device intelligence, durable transfer engine and Android
integration into one installable flagship-focused build.

## Gallery experience

- Date-grouped Pictures timeline.
- Continuously resizing 3–12-column pinch layout with anchored reflow.
- Album grid/list semantic zoom.
- Camera-first album navigation with sorting, essential albums, merged names, groups, smart albums
  and selectable whole albums.
- Press-and-slide range selection with reverse selection and edge scrolling.
- Remembered public tab, destination and grid density.
- Context-preserving viewer ordering: All pictures remains All pictures; an album remains that album.
- Finger-following paging between photos and videos.
- Pinch zoom, video zoom and swipe-up media information.
- Centre-anchored filmstrip with photo/video identity and duration.

## Video

- Inline Media3 playback and explicit external-player route.
- Play/pause and mute behavior shared across the video experience.
- Low-latency seeking while dragging with exact release seek.
- Playback state preserved across scrub gestures.
- Display-frame transport updates.
- Snapshot, rotation, metadata, sharing and editor handoff.
- Matching public and Secure Gallery playback interaction.

## Photo and document editing

- Crop, ratios, rotate, flip, straighten and two-axis perspective.
- Tones, filters, curves, selective color, LUTs and local adjustments.
- Drawing, highlighter, eraser, text, stickers, mosaic and vector layers.
- Lens correction, RAW preview and color-managed editing paths.
- Modern image export with truthful codec/extension handling.
- Ultra HDR preservation where the Android codec path supports it.
- OCR overlay, copyable text, document detection, corner adjustment, inspection loupe,
  perspective flattening and document cleanup.

## Video editing and creation

- Trim, live playhead, crop, rotation, speed, mute, looks, tone and subtitles.
- Save copy/replace, cancellation and MediaStore refresh.
- Configurable collage layouts and overlays.
- GIF creation.
- Mixed-media movies with clip order, ratio, resolution, crossfades, titles and optional audio.
- Slideshow and album creation from relevant gallery contexts.

## On-device intelligence

- Editable object/person/background masks.
- MediaPipe and ML Kit selection backends with edge-aware refinement.
- Fully local Big-LaMa ONNX content-aware fill.
- Fast interactive preview and full-quality Apply.
- Portrait/background effects through shared refined masks.
- Smart albums, OCR/tag indexing, exact duplicates, burst recommendations and visual similarity.
- Replaceable model/runtime boundary with recorded identity, source, licence metadata and checksum.

## Secure Gallery

- Four-digit auto-submit PIN and native Android biometric unlock.
- Android Keystore-protected vault key and recovery boundaries.
- Encrypted and locked-only storage modes.
- Public/Secure parity for gallery, album, viewer, editing, creation, sorting, search and metadata
  concepts where security permits.
- Controlled per-application access for selected locked-only media.
- Copy and true move in both directions.
- Automatic source-folder naming, destination creation and same-name merging.
- Persistent user-owned vault payload designed to survive APK uninstall.
- Secure recycle bin, temporary-share cleanup and explicit lock controls.

## Durable transfers

- WorkManager-backed foreground copy, move, import and export.
- Percentage/item progress notifications.
- Pause, resume and cancel.
- Per-item checkpoints and retry-safe operation identity.
- Destination verification before move-source deletion.
- Transfer reports for partial failures.

## Android integration

- Standard gallery launcher category.
- Image/video viewing and editing.
- Modern and legacy camera review.
- Image/video collection routes.
- Single and multiple legacy media picker contracts.
- Clean picker-only UI with safe result delivery and URI grants.
- Fixed warm intent handoff so picker requests cannot inherit an existing viewer/editor state.
- System integration settings showing current photo/video handler.
- System typeface, font scale, bold adjustment, display scale, locale, RTL and animation-scale
  integration.

## Verification

- 73 JVM tests passed with no failures, errors or skips.
- Android lint completed with zero errors.
- Debug APK assembly passed.
- The expanded system-contract test passed on the physical Pixel 8 Pro.
- Physical gallery launch, photo viewing, viewer-to-picker handoff and multi-selection checks passed.
- The functionally identical pre-publication build installed in place without clearing data; the
  public Version 1 rebuild changes only the public version label.
- Public and Secure Gallery launch smoke tests passed with an empty Android crash buffer.

## Distribution boundary

The Version 1 APK is ARM64-only and debug-signed for evaluation. Android requires the owner to
confirm default viewer associations. Production signing, store policy review, broad OEM/device
coverage and formal security/accessibility certification are not claimed.
