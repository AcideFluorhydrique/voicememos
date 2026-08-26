plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)

	alias(libs.plugins.kotlinx.serialization)
}

android {
	namespace = "com.eva.feature_ios"

	buildFeatures {
		compose = true
	}
}

dependencies {
	// navigation
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.hilt.navigation.compose)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.kotlinx.collections.immutable)

	// lifecycle service for the recorder binder
	implementation(libs.androidx.lifecycle.service)
	implementation(libs.androidx.lifecycle.runtime.compose)

	implementation(project(":core:cupertino"))
	implementation(project(":core:ui"))
	implementation(project(":core:utils"))

	implementation(project(":data:recorder"))
	implementation(project(":data:recordings"))
	implementation(project(":data:player"))
	implementation(project(":data:editor"))
	implementation(project(":data:visualizer"))
	implementation(project(":data:datastore"))
	implementation(project(":data:interactions"))
	implementation(project(":data:use_case"))
}
