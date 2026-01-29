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
        maven(url = "https://jitpack.com")
        flatDir {
            dirs("libs")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.com")
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "StellAR"
include(":app")

include(":chemLab")
include(":unityLibrary:xrmanifest.androidlib")
// project(":chemLab").projectDir = File("L:\\Unity\\Chemistry Lab Android\\unityLibrary")
