pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "VaultGallery"
include(
    ":app",
    ":design-system",
    ":database",
    ":security",
    ":gallery",
    ":viewer",
    ":editor",
    ":creation",
    ":transfer",
    ":search",
    ":ai",
)
