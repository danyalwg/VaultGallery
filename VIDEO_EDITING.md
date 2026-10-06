# Video editing

Version 1 provides a working gallery-integrated video editor built on Android media components and
the shared design system.

## Operations

- Independent trim start/end handles
- Transport playhead and playback-preserving seeking
- Movable crop frame
- Rotation and output sizing
- Speed and mute
- Looks and tone adjustment
- Subtitle panel
- Undo/redo
- Save copy or replace
- Device-aware output estimate
- Cancellation and explicit failure reporting

## Interaction model

Scrubbing uses low-latency seeks while the finger moves and one exact seek when released. A playing
video remains playing after a scrub unless the user explicitly paused it. Crop and transform handles
retain one uninterrupted pointer gesture.

## Export safety

Exports render to an app-controlled destination, report progress, support cancellation, and validate
the output before MediaStore publication or source replacement. Replace does not discard the original
merely because rendering started.

Secure outputs remain inside Secure Gallery unless the user explicitly exports them through an
authenticated flow.
