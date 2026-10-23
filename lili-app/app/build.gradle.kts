plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.lilifriends.games"
    compileSdk = 36
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.lilifriends.games"
        // Same floor as the games: a 2014 phone on KitKat 4.4 must run it. Do not raise.
        minSdk = 19
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
