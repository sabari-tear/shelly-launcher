plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.shalltear.shellylauncher.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The benchmark targets the :app module.
    targetProjectPath = ":app"

    buildTypes {
        create("benchmark") {
            isDebuggable = false
            isMinifyEnabled = false
            // Use debug signing so the test APK installs alongside the app.
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.junit)
}
