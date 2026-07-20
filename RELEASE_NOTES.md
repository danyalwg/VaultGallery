# Vault Gallery 1.0 Debug Milestone

This milestone provides a signed, installable debug APK with public Gallery and encrypted Secure Gallery launchers.

Public Gallery reads user-granted MediaStore photos/videos, groups them by date and album, supports search/selection/sharing, requests platform deletion approval, and opens images or videos. Secure Gallery creates a device-bound Argon2id-protected vault, encrypts imported media in authenticated chunks, keeps metadata encrypted, locks when paused, blocks screenshots, plays secure videos through an on-demand decrypting Media3 data source, and requires confirmation before sharing plaintext or deleting.

Read `KNOWN_LIMITATIONS.md` before using real private media. This APK is debug-signed, not a production store release.
