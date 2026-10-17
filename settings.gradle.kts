pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Scribble"
include(":app")

// Core modules
include(":core:common")
include(":core:model")
include(":core:ui")
include(":core:navigation")
include(":core:network")
include(":core:database")
include(":core:designsystem")
include(":core:datastore")

// Feature modules
include(":feature:auth")
include(":feature:home")
include(":feature:lobby")
include(":feature:game")
include(":feature:results")
include(":feature:profile")