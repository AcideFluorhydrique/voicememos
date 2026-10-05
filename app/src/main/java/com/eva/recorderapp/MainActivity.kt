package com.eva.recorderapp

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.eva.recorderapp.navigation.AppNavHost
import com.eva.ui.R
import com.eva.ui.activity.animateOnExit
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.datastore.domain.enums.AppThemeMode
import com.eva.feature_widget.recorder.refreshRecorderWidgets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private var navController: NavHostController? = null

	private val themeViewModel by viewModels<AppThemeViewModel>()

	// last appearance applied to the system bars, the splash exit has to restore it
	private var isDarkTheme: Boolean? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		// splash needs to be initiated here
		val splash = installSplashScreen()

		super.onCreate(savedInstanceState)

		// set enable edge to edge normally
		enableEdgeToEdge()

		// hold the splash until the saved appearance is known, otherwise a dark app opens
		// with a white frame on a light system and the other way round
		splash.setKeepOnScreenCondition { themeViewModel.themeMode.value == null }

		// the exit animation resets the bars, put the chosen appearance back afterwards
		splash.animateOnExit(
			onAnimationEnd = { isDarkTheme?.let(this::applySystemBars) ?: enableEdgeToEdge() }
		)
		// set activity transitions
		setTransitions()

		setContent {
			val themeMode by themeViewModel.themeMode.collectAsState()

			val isDark = when (themeMode) {
				AppThemeMode.LIGHT -> false
				AppThemeMode.DARK -> true
				else -> isSystemInDarkTheme()
			}

			LaunchedEffect(isDark) {
				isDarkTheme = isDark
				applySystemBars(isDark)
			}

			CupertinoTheme(darkTheme = isDark) {
				AppNavHost(
					onSetController = { controller ->
						if (navController == null) navController = controller
					},
				)
			}
		}
	}

	/**
	 * The default edge to edge style follows the device, a forced appearance has to set the
	 * bar icons itself or they end up light on a light background.
	 */
	private fun applySystemBars(isDark: Boolean) {
		val style = if (isDark) SystemBarStyle.dark(Color.TRANSPARENT)
		else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)

		enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
	}

	override fun onStop() {
		super.onStop()
		// the widget decides what its record button does from the microphone permission,
		// which may just have been granted in here
		lifecycleScope.launch { refreshRecorderWidgets(applicationContext) }
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		navController?.handleDeepLink(intent)
	}

	@Suppress("DEPRECATION")
	private fun setTransitions() {
		// allow activity transitions
		window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
		// set transitions
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			overrideActivityTransition(
				OVERRIDE_TRANSITION_OPEN,
				R.anim.activity_enter_transition,
				R.anim.activity_exit_transition
			)
		} else {
			overridePendingTransition(
				R.anim.activity_enter_transition,
				R.anim.activity_exit_transition
			)
		}
	}
}