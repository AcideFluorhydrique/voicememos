package com.eva.cupertino.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors mirroring the iOS system palette, the values are picked from apple's
 * human interface guidelines so that the app feels native to someone coming from iOS.
 */
@Immutable
data class CupertinoColors(
	val isDark: Boolean,
	// backgrounds
	val systemBackground: Color,
	val secondarySystemBackground: Color,
	val tertiarySystemBackground: Color,
	val groupedBackground: Color,
	val secondaryGroupedBackground: Color,
	val barBackground: Color,
	// content
	val label: Color,
	val secondaryLabel: Color,
	val tertiaryLabel: Color,
	val quaternaryLabel: Color,
	// strokes and fills
	val separator: Color,
	val opaqueSeparator: Color,
	val fill: Color,
	val secondaryFill: Color,
	val tertiaryFill: Color,
	// accents
	val accent: Color,
	val destructive: Color,
	val record: Color,
	val trim: Color,
	val success: Color,
)

internal val cupertinoLightColors = CupertinoColors(
	isDark = false,
	systemBackground = Color(0xFFFFFFFF),
	secondarySystemBackground = Color(0xFFF2F2F7),
	tertiarySystemBackground = Color(0xFFFFFFFF),
	groupedBackground = Color(0xFFF2F2F7),
	secondaryGroupedBackground = Color(0xFFFFFFFF),
	barBackground = Color(0xF2F9F9F9),
	label = Color(0xFF000000),
	secondaryLabel = Color(0x993C3C43),
	tertiaryLabel = Color(0x4D3C3C43),
	quaternaryLabel = Color(0x2E3C3C43),
	separator = Color(0x5C3C3C43),
	opaqueSeparator = Color(0xFFC6C6C8),
	fill = Color(0x33787880),
	secondaryFill = Color(0x29787880),
	tertiaryFill = Color(0x1F767680),
	accent = Color(0xFF007AFF),
	destructive = Color(0xFFFF3B30),
	record = Color(0xFFFF3B30),
	trim = Color(0xFFFFCC00),
	success = Color(0xFF34C759),
)

internal val cupertinoDarkColors = CupertinoColors(
	isDark = true,
	systemBackground = Color(0xFF000000),
	secondarySystemBackground = Color(0xFF1C1C1E),
	tertiarySystemBackground = Color(0xFF2C2C2E),
	groupedBackground = Color(0xFF000000),
	secondaryGroupedBackground = Color(0xFF1C1C1E),
	barBackground = Color(0xF21D1D1D),
	label = Color(0xFFFFFFFF),
	secondaryLabel = Color(0x99EBEBF5),
	tertiaryLabel = Color(0x4DEBEBF5),
	quaternaryLabel = Color(0x2EEBEBF5),
	separator = Color(0xA6545458),
	opaqueSeparator = Color(0xFF38383A),
	fill = Color(0x5C787880),
	secondaryFill = Color(0x52787880),
	tertiaryFill = Color(0x3D767680),
	accent = Color(0xFF0A84FF),
	destructive = Color(0xFFFF453A),
	record = Color(0xFFFF453A),
	trim = Color(0xFFFFD60A),
	success = Color(0xFF30D158),
)
