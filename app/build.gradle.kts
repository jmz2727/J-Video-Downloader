plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.jameel.jvideodownloader"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.jameel.jvideodownloader"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    splits { abi { isEnable = true; reset(); include("armeabi-v7a", "arm64-v8a", "x86_64"); isUniversalApk = false } }
    packaging { jniLibs { useLegacyPackaging = true } }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release { isMinifyEnabled = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
    }
    lint { abortOnError = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
kotlin { jvmToolchain(17) }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    // Explicitly requested extractor runtime; no FFmpeg/aria2 dependency or runtime auto-download.
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    constraints {
        implementation("commons-io:commons-io:2.18.0")
        implementation("org.apache.commons:commons-compress:1.27.1")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
        implementation("com.fasterxml.jackson.core:jackson-core:2.18.3")
        implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
    }
    val bom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(bom)
    androidTestImplementation(bom)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.room:room-testing:2.8.3")
    testImplementation("androidx.work:work-testing:2.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// A clone must fetch the reviewed asset explicitly; a missing engine must fail the build.
val verifyPinnedEngine by tasks.registering {
    doLast {
        val engine = file("src/main/assets/yt-dlp-2026.10.26.zip")
        check(engine.isFile) { "Missing pinned engine: run python tools/prepare-engine.py" }
        val hash = java.security.MessageDigest.getInstance("SHA-256")
        engine.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) { val n = input.read(buffer); if (n < 0) break; hash.update(buffer, 0, n) }
        }
        check(hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) } == "e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3") { "Pinned engine checksum mismatch" }
    }
}
tasks.named("preBuild") { dependsOn(verifyPinnedEngine) }
