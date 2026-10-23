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
    }
}

rootProject.name = "lili-app"
include(":app")

// The four games are reused in place as library modules. Each game folder keeps its own
// build.gradle.kts for building it as a stand-alone app; library.gradle.kts is used here.
for ((name, dir) in listOf(
    "robotpath" to "../robot-path/app",
    "patternmachine" to "../pattern-machine/app",
    "kangaroohops" to "../kangaroo-hops/app",
    "balancescale" to "../balance-scale/app",
)) {
    include(":$name")
    project(":$name").projectDir = file(dir)
    project(":$name").buildFileName = "library.gradle.kts"
}
