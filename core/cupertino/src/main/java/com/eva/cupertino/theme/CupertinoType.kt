package com.eva.cupertino.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The iOS text styles, sizes are taken from the default (large) dynamic type scale.
 * San-Francisco cannot be bundled so the platform sans family is used with the same metrics.
 */
@Immutable
data class CupertinoTypography(
	val largeTitle: TextStyle = TextStyle(
		fontFamily = FontFamily.Default,
		fontSize = 34.sp,
		lineHeight = 41.sp,
		fontWeight = FontWeight.Bold,
		letterSpacing = (-0.4).sp,
	),
	val title1: TextStyle = TextStyle(
		fontSize = 28.sp,
		lineHeight = 34.sp,
		fontWeight = FontWeight.Bold,
		letterSpacing = (-0.3).sp,
	),
	val title2: TextStyle = TextStyle(
		fontSize = 22.sp,
		lineHeight = 28.sp,
		fontWeight = FontWeight.Bold,
		letterSpacing = (-0.2).sp,
	),
	val title3: TextStyle = TextStyle(
		fontSize = 20.sp,
		lineHeight = 25.sp,
		fontWeight = FontWeight.SemiBold,
	),
	val headline: TextStyle = TextStyle(
		fontSize = 17.sp,
		lineHeight = 22.sp,
		fontWeight = FontWeight.SemiBold,
		letterSpacing = (-0.2).sp,
	),
	val body: TextStyle = TextStyle(
		fontSize = 17.sp,
		lineHeight = 22.sp,
		fontWeight = FontWeight.Normal,
		letterSpacing = (-0.2).sp,
	),
	val callout: TextStyle = TextStyle(
		fontSize = 16.sp,
		lineHeight = 21.sp,
		fontWeight = FontWeight.Normal,
	),
	val subHeadline: TextStyle = TextStyle(
		fontSize = 15.sp,
		lineHeight = 20.sp,
		fontWeight = FontWeight.Normal,
	),
	val footnote: TextStyle = TextStyle(
		fontSize = 13.sp,
		lineHeight = 18.sp,
		fontWeight = FontWeight.Normal,
	),
	val caption: TextStyle = TextStyle(
		fontSize = 12.sp,
		lineHeight = 16.sp,
		fontWeight = FontWeight.Normal,
	),
	val caption2: TextStyle = TextStyle(
		fontSize = 11.sp,
		lineHeight = 13.sp,
		fontWeight = FontWeight.Normal,
	),
	/**Used by the recorder timer, digits should not dance while counting */
	val timer: TextStyle = TextStyle(
		fontFamily = FontFamily.Monospace,
		fontSize = 44.sp,
		lineHeight = 52.sp,
		fontWeight = FontWeight.Light,
		letterSpacing = (-1).sp,
	),
	/**Monospaced digits for the player positions */
	val monoDigits: TextStyle = TextStyle(
		fontFamily = FontFamily.Monospace,
		fontSize = 12.sp,
		lineHeight = 16.sp,
		fontWeight = FontWeight.Normal,
	),
)
