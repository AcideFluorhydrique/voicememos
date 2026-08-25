package com.eva.cupertino.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * iOS leans on haptics a lot, this gives the compose tree a light tap and a heavier
 * thud without every call site touching the platform view.
 */
class CupertinoHaptics internal constructor(private val view: View) {

	fun light() = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

	fun medium() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

	fun heavy() = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

@Composable
fun rememberCupertinoHaptics(): CupertinoHaptics {
	val view = LocalView.current
	return remember(view) { CupertinoHaptics(view) }
}
