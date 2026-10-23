plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.foadsf.patternmachine"
    compileSdk = 36
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "io.github.foadsf.patternmachine"
        // Must run on a 2014 phone with stock KitKat 4.4 as well as a newer tablet. Do not raise.
        minSdk = 19
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables {
            generatedDensities?.clear()
            generatedDensities?.addAll(listOf("hdpi", "xhdpi", "xxhdpi"))
        }
    }

    // Optional shared debug key, so builds from several machines can update the same installed
    // copy: `debug.keystore` in this app's folder or in the repository root (both gitignored), or
    // the copy kept in an enclosing private checkout. Without one, AGP's per-machine key is used.
    val sharedDebugKey = listOf("debug.keystore", "../debug.keystore", "../../../secrets/kid-launcher-debug.keystore")
        .map { rootProject.file(it) }.firstOrNull { it.exists() }
    if (sharedDebugKey != null) {
        signingConfigs {
            getByName("debug") {
                storeFile = sharedDebugKey
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        // API 19 is the point: a too-new API call is a build failure, not a crash on the phone.
        error += "NewApi"
        abortOnError = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
