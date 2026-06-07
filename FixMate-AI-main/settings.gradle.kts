// Top-level settings file: defines repositories and the modules in this project.
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

dependencyResolutionManagement {
    // Fail fast if any module declares its own repositories.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack is occasionally required by transitive libraries; safe to keep.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FixMate AI"
include(":app")
