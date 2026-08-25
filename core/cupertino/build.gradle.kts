plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.cupertino"

	buildFeatures {
		compose = true
	}
}

dependencies {
	implementation(project(":core:utils"))
}
