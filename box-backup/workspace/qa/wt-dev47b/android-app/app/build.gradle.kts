plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.shanivar.nativeapp"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.shanivar.nativeapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_URL", "\"https://api.thehybridmonk.com/api\"")
        // Supabase sync flag: empty URL = sync off (default). Set -PsupabaseUrl / -PsupabaseAnonKey for local E2E only.
        buildConfigField("String", "SUPABASE_URL", "\"${(project.findProperty("supabaseUrl") as String?) ?: ""}\"")
        // DEV-47: single sync switch. Debug only until DEV-44 flavours; sync also stays off when the URL is empty.
        buildConfigField("boolean", "SYNC_ENABLED", "false")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${(project.findProperty("supabaseAnonKey") as String?) ?: ""}\"")
        // MVP streamline task (package 1, 2026-09-18): the one explicit switch between the
        // simplified single-Home launch surface (ShanivarApp's `HomeScreen`) and the earlier
        // four-tab shell (`LegacyTabShell`, kept intact rather than deleted - see CLAUDE.md
        // section 5, "keep tested code"). Reversible by flipping this one line; nothing about the
        // alarm/focus engines, backend contract, or persisted data differs between the two - only
        // which composable presents them, so toggling this never orphans an active session.
        buildConfigField("boolean", "STREAMLINED_LAUNCH", "true")
        // Founder direction 2026-09-29: a three-tab shell (Dashboard / Routine / Sadhana). Default
        // false because the visual contract for it does not exist yet - GPT's wireframe is pending -
        // and the shipped single-Home surface must not move while a scaffold is being fitted. The
        // tabs re-home existing composables rather than replacing them, so flipping this switches
        // presentation only: same alarm engine, same focus engine, same Steady player, same storage.
        buildConfigField("boolean", "THREE_TAB_NAV", "false")
        // Decision 0014 Routine V3 two-tab shell (Routine | Sadhana). Default false so V1Home
        // remains the launch path until the Routine timeline / multi-window UI is device-gated.
        // Flipping this only switches presentation: same engines, same Steady, same storage.
        buildConfigField("boolean", "ROUTINE_V3", "false")
        // Initiation V1 (decision 0016, DEV-26). Off for release so the current app is unchanged;
        // on for debug builds so the new flow can be reviewed.
        buildConfigField("boolean", "INITIATION_V1", "false")
        // Sadhana V1 (decision 0017, DEV-31..33). Debug only.
        buildConfigField("boolean", "SADHANA_V1", "false")
    }
    buildTypes {
        debug {
            buildConfigField("boolean", "INITIATION_V1", "true")
            buildConfigField("boolean", "SADHANA_V1", "true")
            buildConfigField("boolean", "SYNC_ENABLED", "true")
            applicationIdSuffix = ".dev"
            val debugApi = providers.gradleProperty("shanivarApiUrl").getOrElse("http://10.0.2.2:5000/api")
            require(debugApi.matches(Regex("https?://[a-zA-Z0-9.\\-:/]+"))) { "Invalid shanivarApiUrl" }
            buildConfigField("String", "API_URL", "\"$debugApi\"")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Optional Initiation review screenshots: ./gradlew testDebugUnitTest -PinitiationShots=/path
        unitTests.all { test -> providers.gradleProperty("initiationShots").orNull?.let { test.systemProperty("initiation.shots", it) }
            providers.gradleProperty("sadhanaShots").orNull?.let { test.systemProperty("sadhana.shots", it) } }
        managedDevices.devices.create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel2Api34") {
            device = "Pixel 2"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Discipline engine dismissal flows: QR scan (CameraX + ML Kit barcode scanning) and photo +
    // motion (CameraX capture + platform SensorManager, no extra library needed for the sensor side).
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.withType<Test>().configureEach {
    (project.findProperty("supabaseE2eUrl") as String?)?.let { systemProperty("supabase.e2e.url", it) }
    (project.findProperty("supabaseE2eAnonKey") as String?)?.let { systemProperty("supabase.e2e.anon", it) }
}
