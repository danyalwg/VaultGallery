plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.danyal.vaultgallery.ai"; compileSdk = 35; defaultConfig { minSdk = 28 }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    api("com.microsoft.onnxruntime:onnxruntime-android:1.30.0")
    implementation("com.google.crypto.tink:tink-android:1.23.0")
}
