package com.eva.cupertino.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalCupertinoColors = staticCompositionLocalOf { cupertinoLightColors }
val LocalCupertinoTypography = staticCompositionLocalOf { CupertinoTypography() }

object CupertinoTheme {

	val colors: CupertinoColors
		@Composable
		@ReadOnlyComposable
		get() = LocalCupertinoColors.current

	val typography: CupertinoTypography
		@Composable
		@ReadOnlyComposable
		get() = LocalCupertinoTypography.current
}

/**
 * Applies the iOS look and feel, a material theme is kept underneath as few material
 * components (sheets, ripples) are still used, its colors are mapped onto the iOS palette
 * so nothing leaks the material identity.
 */
@Composable
fun CupertinoTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit,
) {
	val colors = if (darkTheme) cupertinoDarkColors else cupertinoLightColors
	val typography = CupertinoTypography()

	val materialColors = if (darkTheme) {
		darkColorScheme(
			primary = colors.accent,
			onPrimary = Color.White,
			background = colors.systemBackground,
			onBackground = colors.label,
			surface = colors.secondaryGroupedBackground,
			onSurface = colors.label,
			surfaceVariant = colors.tertiarySystemBackground,
			onSurfaceVariant = colors.secondaryLabel,
			error = colors.destructive,
			outline = colors.separator,
			outlineVariant = colors.separator,
			scrim = Color.Black,
		)
	} else {
		lightColorScheme(
			primary = colors.accent,
			onPrimary = Color.White,
			background = colors.systemBackground,
			onBackground = colors.label,
			surface = colors.secondaryGroupedBackground,
			onSurface = colors.label,
			surfaceVariant = colors.secondarySystemBackground,
			onSurfaceVariant = colors.secondaryLabel,
			error = colors.destructive,
			outline = colors.separator,
			outlineVariant = colors.separator,
			scrim = Color.Black,
		)
	}

	val materialTypography = Typography(
		headlineLarge = typography.largeTitle,
		headlineMedium = typography.title1,
		titleLarge = typography.title2,
		titleMedium = typography.headline,
		bodyLarge = typography.body,
		bodyMedium = typography.callout,
		bodySmall = typography.footnote,
		labelLarge = typography.headline,
		labelMedium = typography.subHeadline,
		labelSmall = typography.caption,
	)

	CompositionLocalProvider(
		LocalCupertinoColors provides colors,
		LocalCupertinoTypography provides typography,
	) {
		MaterialTheme(
			colorScheme = materialColors,
			typography = materialTypography,
			content = content,
		)
	}
}
