import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	// custom plugins
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.recorderapp"
	compileSdk = libs.versions.compileSdk.get().toInt()

	defaultConfig {
		applicationId = "io.github.acidefluorhydrique.voicememos"
		minSdk = libs.versions.minSdk.get().toInt()
		targetSdk = libs.versions.compileSdk.get().toInt()
		versionCode = 1
		versionName = "1.0.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	signingConfigs {
		// CI hands the key over through the environment, the workflow decodes it from a
		// repository secret, the same key every run so installed builds can be updated
		val ciStoreFile = System.getenv("SIGNING_STORE_FILE")?.let(::File)
		if (ciStoreFile != null && ciStoreFile.exists()) {
			create("release") {
				storeFile = ciStoreFile
				storePassword = System.getenv("SIGNING_STORE_PASSWORD")
				keyAlias = System.getenv("SIGNING_KEY_ALIAS")
				keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
			}
			return@signingConfigs
		}

		// find if there is a properties file
		val keySecretFile = rootProject.file("keystore.properties")
		if (!keySecretFile.exists()) return@signingConfigs

		// load the properties
		val properties = Properties()
		keySecretFile.inputStream().use { properties.load(it) }

		val userHome = System.getProperty("user.home")
		val storeFileName = properties.getProperty("STORE_FILE_NAME")

		val keyStoreFolder = File(userHome, "keystore")
		if (!keyStoreFolder.exists()) return@signingConfigs

		val keyStoreFile = File(keyStoreFolder, storeFileName)
		if (!keyStoreFile.exists()) return@signingConfigs

		create("release") {
			storeFile = keyStoreFile
			keyAlias = properties.getProperty("KEY_ALIAS")
			keyPassword = properties.getProperty("KEY_PASSWORD")
			storePassword = properties.getProperty("STORE_PASSWORD")
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			multiDexEnabled = true
			// the version control block records the checkout the build came from, leave it out so
			// the bytes depend on the sources alone and F-Droid can match the signed release
			vcsInfo.include = false
			// change the signing config if release is not found
			signingConfig = signingConfigs.findByName("release")
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
		debug {
			applicationIdSuffix = ".debug"
			resValue("string", "app_name", "Voice Memos (DEBUG)")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}
	// the dependency metadata block is encrypted with a Google public key, only Play can read it,
	// and F-Droid rejects APKs that carry it
	dependenciesInfo {
		includeInApk = false
		includeInBundle = false
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
		// the prebuilt androidx .so files only get stripped when a matching NDK happens to be
		// installed, which varies between machines, so they ship untouched everywhere
		jniLibs {
			keepDebugSymbols += "**/*.so"
		}
	}
}

// the compiled baseline profile is not byte for byte stable between build machines, which
// would keep F-Droid from matching the signed release, so it is left out
tasks.configureEach {
	if (name.contains("ArtProfile")) enabled = false
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.work.runtime.ktx)
	implementation(libs.androidx.hilt.work)

	implementation(project(":core:utils"))
	implementation(project(":core:ui"))
	implementation(project(":core:cupertino"))
	implementation(project(":data:worker"))
	implementation(project(":data:datastore"))
	implementation(project(":data:interactions"))
	implementation(project(":feature:widget"))
	implementation(project(":feature:ios"))

	// android testing
	androidTestImplementation(libs.androidx.runner)
}
