// Used by ../../lili-app, which builds all four games into one app. The stand-alone game
// still builds from build.gradle.kts next to this file.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.foadsf.patternmachine"
    compileSdk = 36
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 19
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/lib/AndroidManifest.xml")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        error += "NewApi"
        abortOnError = true
    }
}
