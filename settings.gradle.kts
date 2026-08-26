pluginManagement {
	includeBuild("build-logic")
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
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
	}
}

rootProject.name = "RecorderApp"
include(":app")
include(":data:interactions")
include(":data:location")
include(":core:utils")
include(":data:datastore")
include(":data:database")
include(":data:player")
include(":data:recordings")
include(":data:recorder")
include(":data:worker")
include(":data:bookmarks")
include(":data:categories")
include(":data:use_case")
include(":core:ui")
include(":core:cupertino")
include(":feature:widget")
include(":data:editor")
include(":testing:runtime")
include(":data:visualizer")
include(":feature:ios")
