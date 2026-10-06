import java.security.MessageDigest
import java.util.UUID

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.danyal.vaultgallery"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.danyal.vaultgallery"
        // AndroidX HeifWriter's supported HEIC/AVIF path requires Android 10. The app is
        // intentionally arm64/flagship-only, so keep the declared platform contract honest
        // instead of forcing the library onto Android 9 with a manifest override.
        minSdk = 29
        targetSdk = 35
        // Version 1 is the first and only public release. Keep the monotonically increasing
        // internal code so existing evaluation installs can upgrade without uninstalling.
        versionCode = 66
        versionName = "1.0.0"

        // Vault Gallery is deliberately distributed only to modern flagship phones. Shipping
        // x86, x86_64 and 32-bit ARM copies of OpenCV, MediaPipe, OCR and SQLCipher made the
        // sideload APK hundreds of megabytes larger without helping either supported device.
        ndk {
            abiFilters += "arm64-v8a"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // Keep the 198 MB LaMa graph page-aligned and memory-mappable. Compressing it into the APK
    // would require a second full copy in app storage before ONNX Runtime could open it.
    androidResources {
        noCompress += "onnx"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

composeCompiler {
    // Kotlin 2.3's optional Compose/R8 mapping producer is useful only for de-obfuscating
    // composable stack traces and pulls an additional build-time artifact. Runtime behavior and
    // ordinary R8 mapping are unchanged without it, while release builds remain reproducible in
    // the offline workstation used for device verification.
    includeComposeMappingFile.set(false)
}

dependencies {
    implementation(project(":design-system"))
    implementation(project(":database"))
    implementation(project(":security"))
    implementation(project(":gallery"))
    implementation(project(":viewer"))
    implementation(project(":editor"))
    implementation(project(":creation"))
    implementation(project(":transfer"))
    implementation(project(":search"))
    implementation(project(":ai"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-video:3.4.0")
    implementation("io.coil-kt.coil3:coil-gif:3.4.0")
    implementation("io.coil-kt.coil3:coil-svg:3.4.0")
    implementation("io.github.panpf.zoomimage:zoomimage-compose-coil3:1.4.0")
    implementation("androidx.media3:media3-exoplayer:1.9.2")
    implementation("androidx.media3:media3-ui:1.9.2")
    implementation("androidx.media3:media3-transformer:1.9.2")
    implementation("androidx.media3:media3-effect:1.9.2")
    implementation("androidx.transition:transition:1.5.1")
    implementation("com.burhanrashid52:photoeditor:3.1.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.print:print:1.0.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.lambdapioneer.argon2kt:argon2kt:1.6.0")
    implementation("com.google.crypto.tink:tink-android:1.23.0")
    implementation("androidx.heifwriter:heifwriter:1.2.0-beta01")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("net.zetetic:sqlcipher-android:4.17.0")
    implementation("org.opencv:opencv:4.12.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta6")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("org.maplibre.gl:android-sdk:13.6.1")
    // Google's official on-device Magic Touch model powers tap/paint object selection.
    implementation("com.google.mediapipe:tasks-vision:1.0.0")
    kapt("androidx.room:room-compiler:2.8.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}

private fun String.jsonEscaped(): String = replace("\\", "\\\\").replace("\"", "\\\"")

val generateSbom by tasks.registering {
    group = "verification"
    description = "Generate a CycloneDX inventory from the resolved release runtime"
    doLast {
        val artifacts = configurations.getByName("releaseRuntimeClasspath")
            .resolvedConfiguration.resolvedArtifacts
            .sortedBy { "${it.moduleVersion.id.group}:${it.name}:${it.moduleVersion.id.version}" }
        val output = rootProject.layout.projectDirectory.file("dist/vault-gallery-sbom.cdx.json").asFile
        output.parentFile.mkdirs()
        val components = artifacts.joinToString(",\n") { artifact ->
            val id = artifact.moduleVersion.id
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(artifact.file.readBytes()).joinToString("") { "%02x".format(it) }
            """    {"type":"library","group":"${id.group.jsonEscaped()}","name":"${artifact.name.jsonEscaped()}","version":"${id.version.jsonEscaped()}","purl":"pkg:maven/${id.group.jsonEscaped()}/${artifact.name.jsonEscaped()}@${id.version.jsonEscaped()}","hashes":[{"alg":"SHA-256","content":"$hash"}]}"""
        }
        output.writeText("""{
  "bomFormat": "CycloneDX",
  "specVersion": "1.5",
  "serialNumber": "urn:uuid:${UUID.randomUUID()}",
  "version": 1,
  "metadata": {"component":{"type":"application","name":"Vault Gallery","version":"${android.defaultConfig.versionName}"}},
  "components": [
$components
  ]
}
""")
        logger.lifecycle("Wrote ${output.absolutePath}")
    }
}

val generateResolvedDependencyNotices by tasks.registering {
    group = "verification"
    description = "Generate a resolved dependency inventory beside the curated legal notices"
    doLast {
        val coordinates = configurations.getByName("releaseRuntimeClasspath")
            .resolvedConfiguration.resolvedArtifacts
            .map { "${it.moduleVersion.id.group}:${it.name}:${it.moduleVersion.id.version}" }
            .distinct().sorted()
        val curated = file("src/main/assets/THIRD_PARTY_NOTICES.txt").takeIf { it.isFile }?.readText().orEmpty()
        val output = rootProject.layout.projectDirectory.file("dist/THIRD_PARTY_NOTICES_RESOLVED.txt").asFile
        output.parentFile.mkdirs()
        output.writeText(buildString {
            appendLine("Vault Gallery resolved release dependencies")
            appendLine("Generated from releaseRuntimeClasspath; verify every licence before distribution.")
            appendLine()
            coordinates.forEach(::appendLine)
            appendLine()
            appendLine("Curated notices")
            append(curated)
        })
        logger.lifecycle("Wrote ${output.absolutePath}")
    }
}

tasks.register("generateComplianceArtifacts") {
    group = "verification"
    dependsOn(generateSbom, generateResolvedDependencyNotices)
}
