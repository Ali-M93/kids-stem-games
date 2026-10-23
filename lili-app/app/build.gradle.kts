plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.lilifriends.games"
    compileSdk = 36
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.lilifriends.games"
        // Android 7.0+: Google Play's automatic protection needs minSdk 24. (The stand-alone games
        // in this repository still build for Android 4.4.)
        minSdk = 24
        targetSdk = 36
        // CI passes the run number, so every uploaded build has a higher version code.
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("VERSION_CODE") ?: "0")
    }

    // The upload key comes from GitHub secrets (same names as Madrasatuna); never from the repo.
    val ksPath = System.getenv("KS_PATH")
    signingConfigs {
        create("release") {
            if (ksPath != null) {
                storeFile = file(ksPath)
                storePassword = System.getenv("KS_PASS")
                keyAlias = System.getenv("KS_ALIAS")
                keyPassword = System.getenv("KS_KEY_PASS")
            }
        }
    }

    buildTypes {
        release {
            if (ksPath != null) signingConfig = signingConfigs.getByName("release")
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
        error += "NewApi"
        // The app's own screens are in English and Arabic; the games carry Dutch and Farsi too.
        disable += "MissingTranslation"
        abortOnError = true
        checkDependencies = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":robotpath"))
    implementation(project(":kangaroohops"))
    implementation(project(":patternmachine"))
    implementation(project(":balancescale"))
}
