# Known limitations — Vault Gallery Version 1

This document is intentionally direct. A gallery handles irreplaceable data; limitations must be
easier to find than marketing claims.

## Distribution and signing

- Version 1 is an ARM64-only flagship-phone evaluation build.
- The GitHub APK is signed with an Android debug certificate, not a protected production release
  key.
- Debug-signed builds are unsuitable for a store release or a long-lived production update chain.
- The source release does not contain signing credentials.
- Keep an independent backup of irreplaceable media.

## Android default-app boundary

Vault Gallery advertises Android’s standard gallery, image/video viewer, editor, camera-review,
collection and legacy picker contracts. Android intentionally keeps the final default-app decision
with the device owner, so an ordinary third-party app cannot silently become the default photo/video
handler on every phone. OEM camera and file-manager applications may use embedded/private viewers
that never consult the public resolver.

Android also has no device-wide media-sort preference. Vault Gallery follows real system
configuration—typeface, font scale, bold adjustment, display scale, locale, RTL and animation
scale—while remembering gallery sort and grid choices per relevant surface.

## Secure Gallery boundary

- Secure Gallery is application-level protection, not an Android operating-system container.
- Rooted or compromised devices, malicious privileged services, memory capture while unlocked and
  recipient retention of deliberately shared plaintext are outside its protection boundary.
- Locked-only mode is protected by app authentication, private navigation, `.nomedia` and controlled
  provider access; it is not encrypted at rest.
- Encrypted mode improves confidentiality but must create authenticated plaintext for approved
  viewing/sharing/export flows.
- No software can promise forensic erasure from flash storage after deletion.
- Uninstall is designed not to remove the persistent vault payload, but it does remove app-private
  preferences and can complicate recovery. Preserve recovery material and a separate backup.

## Neural and AI-assisted tools

- Big-LaMa content-aware fill is a fixed 512×512 local inpainting model, not a prompt-driven cloud
  generator.
- Large removals, repeated structures, text, faces at a mask edge and selections without enough
  surrounding context can produce visible artifacts.
- First use includes model initialization.
- Interactive selection depends on the quality of the source and may require Add/Subtract/Lasso
  refinement.
- Real-ESRGAN, NAFNet, MODNet, Whisper and face-restoration weights are not bundled or claimed as
  working.
- MediaPipe/ML Kit model terms require separate review from their runtime code licences; see model
  metadata and third-party notices.

## Real-time interaction boundary

Direct manipulation uses live GPU or bounded preview surfaces. Full-resolution encoding, large-model
inference and destructive output generation still happen after Apply/Save. A responsive preview does
not make full-resolution work instantaneous. When an algorithm cannot incrementally infer, the UI
keeps the latest valid preview rather than blocking the interaction surface.

## Media and hardware

- Very large libraries and very high-resolution edits still require broader stress testing.
- Uncommon image/video codecs remain dependent on Android vendor codecs.
- The editor avoids unnecessary full-resolution history, but a single 50 MP ARGB working frame can
  require roughly 200 MB before additional processing buffers.
- Some output codecs are available only when the device advertises them.
- Movie background audio must be acceptable to Android’s MP4 muxer; unsupported source audio is
  rejected rather than silently producing a corrupt movie.
- Tablet, foldable, desktop-mode, low-memory and broad OEM matrices are not yet certified.

## WhatsApp organization

Conversation-oriented organization requires a compatible local WhatsApp/WhatsApp Business backup
and the user-supplied 64-character key. Backups and schemas can change. When matching is unavailable,
the app preserves and displays WhatsApp’s normal folders rather than physically rearranging files.

## Architecture and editing

- Feature modules exist, but some mature implementations remain in the app module while extraction
  continues.
- The internal vector workspace is functional, while some legacy drawing/text/sticker paths still
  use the older compositor.
- Whole-vault backup/restore and recovery paths require continued device testing before being trusted
  as the only copy of real data.
- No cloud synchronization or shared cloud album service is included.
- Proprietary OEM account services, vendor AI models, private motion-photo internals and privileged
  framework integrations cannot be reproduced through public Android APIs.

## Accessibility and release engineering

- System font/display/motion configuration is integrated, but formal accessibility certification is
  not complete.
- Broad TalkBack, switch-access, magnification, RTL and extreme font-scale matrices require more
  device coverage.
- Reproducible production release signing, Play policy review, privacy-policy hosting, store listing,
  support operations and a complete security review remain release-engineering work.

## Data-safety rule

Do not rely on Version 1 as the only copy of irreplaceable media. Test copy, move, recovery, export
and restore workflows with disposable files on the exact target device before trusting them with the
only original.
