plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.ui"
	buildFeatures {
		compose = true
	}
}

dependencies {
	// the animated destination helper
	implementation(libs.androidx.navigation.compose)
	// the splash screen exit animation
	implementation(libs.androidx.core.splashscreen)

	// exposed, the feature modules hold their lists in immutable collections
	api(libs.kotlinx.collections.immutable)
}
