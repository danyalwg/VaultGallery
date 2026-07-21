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
