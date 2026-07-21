# Vault Gallery 2.1 Pixel Audit Debug

Version 2.1 is a physical-device repair and interaction audit for Pixel, with a Samsung-style workflow used as the behavioral reference.

## Fixed and completed in 2.1

- Reliable video thumbnails through Android `loadThumbnail`, with Coil video-frame fallback for codec/provider edge cases.
- A custom inline video player with autoplay, scrubbing, elapsed/total time, replay, mute, rotation, casting settings, and an explicit external-player action.
- The same custom playback experience for encrypted Secure Gallery videos, using an app-private temporary playback copy that is deleted when the viewer closes and purged on startup.
- Six-to-twelve digit Secure Gallery PIN setup, confirmation validation, credential changes, strong-biometric unlock, launcher visibility, and configurable auto-lock settings.
- Secure image and video imports through the Storage Access Framework on Android 9–15, plus direct Android share-target support.
- Transactional Move to Secure Gallery: encrypted import completes first; Android requests recycle-bin approval for originals only after every item succeeds.
- One-time dismissible Albums education, real whole-album long-press selection, persistent Essential Album membership, actual album-name merging, and working album Copy/Move/Share/Secure actions.
- Album creation by selecting media and a real destination folder, plus searchable All Albums.
- A working 3/4/5-column Pictures layout control and press-drag range selection with edge scrolling.
- Working tags in search, home-screen collection shortcuts, external editor/wallpaper fallbacks, automatic-story control, and removal of reachable placeholder-only actions.
- Pixel 8 Pro installation and runtime audit, Android 11 encrypted-video import/playback validation, and connected instrumentation coverage.

The APK is debug-signed. Back up irreplaceable media and read `KNOWN_LIMITATIONS.md` before relying on it as the only copy.

---

# Vault Gallery 2.0 Samsung-style Debug

Version 2.0 is rebuilt from a direct, read-only audit of Samsung Gallery 15.6.06.0 on One UI 7.

## New in 2.0

- Audited Pictures, Albums, Stories, Menu, Search, Settings, viewer, video viewer, and selection structures.
- Pictures overflow with Select, Create, Slideshow, and duplicate review.
- Samsung-style press-drag selection with live range updates and edge auto-scroll.
- Complete eight-action Menu hub plus a dedicated Secure Gallery entry.
- Search landing page, configurable search categories, local name/album/type results, and voice-ready layout.
- Videos, Recent, Favourites, Clean out, Locations, Shared albums, and Recycle bin surfaces.
- Expanded Gallery settings matching Samsung's grouping and defaults where the underlying capability exists.
- Viewer actions for favourites, external editing, Photo Assist information, sharing, trash, details, clipboard, wallpaper, external playback, and Secure Gallery import.
- Strong biometric or device-screen-lock authentication for the Keystore-wrapped vault key on Android 11+.
- No visible debug watermark in the packaged evaluation APK.

The build is debug-signed. Review `KNOWN_LIMITATIONS.md` before relying on it for irreplaceable private media.

---

# Vault Gallery 1.1 Debug

Version 1.1 turns the original functional milestone into a much more complete Pixel replacement for Samsung Gallery.

## Highlights

- Samsung-style long-press-and-slide selection with range deselection and edge scrolling.
- Strong-biometric Secure Gallery unlock backed by an authentication-bound Android Keystore key.
- Change PIN/passphrase, configurable auto-lock delays, lock-now, and optional Secure Gallery launcher hiding.
- Recoverable encrypted Secure Gallery recycle bin with automatic 30-day expiry.
- Android-native public favourites, trash, restore, empty-bin, and permanent-delete operations.
- Functional grid-density, essential-album, external-player, permissions, privacy, and launcher settings.
- Local monthly photo stories, album search, and improved confirmations and status feedback.
- MediaStore image/video queries separated for broader Android compatibility.

The build remains debug-signed and visibly marked DEBUG. Review `KNOWN_LIMITATIONS.md` before relying on it for irreplaceable private media.
