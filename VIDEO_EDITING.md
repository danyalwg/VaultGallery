# Video Editing

Video editing is Phase 5 and will ship as a non-destructive project graph. Source media is immutable; operations store parameters and timeline references until export.

## Project model

A project contains ordered source clips, trims/splits, transforms, speed segments, freeze frames, audio tracks/envelopes, overlays, filters/adjustments, transitions, canvas, and export preset. Every edit command is reversible for undo/redo. Public and secure projects/caches are isolated.

## Processing decision

Media3 Transformer is the preferred stable foundation for decoding/composition/export where supported. Capability checks hide unsupported codec/frame-rate/HDR operations. A feature is not shown until its real pipeline, cancellation, recovery, accessibility, and export validation exist. Reverse, noise reduction, and complex transitions may remain absent when reliable device support is unavailable.

## Export transaction

Estimate output and temporary space; render to an app-controlled temporary output; expose progress/cancel/retry; flush; probe duration, dimensions, tracks, and decodability; optionally checksum; then publish with MediaStore pending semantics or authenticated secure import. Replacing an original requires explicit confirmation and retains the original until the replacement verifies.

## Secure workflow

Secure editing decrypts only bounded data when possible. Any required plaintext intermediate uses no-backup private storage, a cleanup journal, reauthentication, and secure-screen protection. Outputs stay secure unless the user explicitly exports after a warning and metadata-removal choices.

## Test matrix

Cover trim/split boundaries, rearrangement, rotation/crop/aspect, speed, audio mix/fades, overlays, presets, estimated size tolerance, cancel/retry, full storage, codec rejection, corrupt input, process death, rotation, validated publishing, original preservation, and secure-temp cleanup.
