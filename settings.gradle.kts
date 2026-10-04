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
        mavenLocal()  // ← AGREGADO: Para consumir AAR desde ~/.m2/repository
        google()
        mavenCentral()
    }
}

rootProject.name = "TwedMediaInfoTest"

include(":app")

// ELIMINADO: includeBuild de composite build
// Ahora TwedMediaInfo se consume desde Maven Local