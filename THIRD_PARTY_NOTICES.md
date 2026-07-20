# Third-Party Notices Plan

No third-party runtime library or font has been added in Phase 0 beyond the Android/Kotlin build plugins. This file will list every shipped dependency, asset, model, font, licence, copyright notice, source link, modification, and notice-delivery requirement before release.

Planned AndroidX/Google components include Jetpack Compose, Material 3 foundations, Navigation, Room, Coroutines/Flow, Hilt, Paging, Media3, WorkManager, Biometric, ExifInterface, DataStore, benchmark, and test libraries. Planned candidates requiring explicit Phase 3 review include Coil, an Argon2id implementation, an encrypted SQLite/Room integration, and screenshot tooling.

The rounded handwritten visual reference will be matched only with a redistributable font (preferably SIL Open Font License) after metric tests. Its font files and complete licence text will be packaged correctly. Vendor fonts, logos, icons, trademarks, and supplied screenshots will not be shipped.

Release automation will generate a dependency inventory and compare it with this notice file. Dependencies with incompatible, unclear, abandoned, or missing licences are rejected.
