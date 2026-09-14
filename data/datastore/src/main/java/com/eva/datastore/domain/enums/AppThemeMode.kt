package com.eva.datastore.domain.enums

/**
 * How the app picks between its light and dark palette, [SYSTEM] follows the device the
 * way iOS does with its automatic appearance.
 */
enum class AppThemeMode {
	SYSTEM,
	LIGHT,
	DARK,
}
