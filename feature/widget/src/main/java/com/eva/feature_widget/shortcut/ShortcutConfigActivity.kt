package com.eva.feature_widget.shortcut

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.feature_widget.R
import com.eva.ui.theme.darkScheme
import com.eva.ui.theme.lightScheme

/**
 * Picker shown by automation apps that offer a "Launch app shortcut" action (Key Mapper,
 * Tasker, MacroDroid...) through the standard [Intent.ACTION_CREATE_SHORTCUT] protocol. The
 * chosen action is handed back as a shortcut that starts [ShortcutHandlerActivity], the app
 * itself never opens.
 */
class ShortcutConfigActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// backing out of the picker creates nothing
		setResult(RESULT_CANCELED)
		enableEdgeToEdge()

		setContent {
			ShortcutPickerTheme {
				Surface(modifier = Modifier.fillMaxSize()) {
					ShortcutPicker(
						actions = ShortcutAction.entries,
						onPick = ::returnShortcut,
						modifier = Modifier.systemBarsPadding()
					)
				}
			}
		}
	}

	private fun returnShortcut(action: ShortcutAction) {
		val shortcutIntent = Intent(applicationContext, ShortcutHandlerActivity::class.java).apply {
			this.action = ShortcutContract.ACTION_RUN_SHORTCUT
			putExtra(ShortcutContract.EXTRA_ACTION, action.id)
		}

		val result = Intent().apply {
			putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
			putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(action.label))
			putExtra(
				Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(this@ShortcutConfigActivity, applicationInfo.icon)
			)
		}
		setResult(RESULT_OK, result)
		finish()
	}
}

@Composable
private fun ShortcutPickerTheme(content: @Composable () -> Unit) {
	val isDark = isSystemInDarkTheme()
	val context = LocalContext.current

	val colorScheme = when {
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
			if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

		isDark -> darkScheme
		else -> lightScheme
	}
	MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
private fun ShortcutPicker(
	actions: List<ShortcutAction>,
	onPick: (ShortcutAction) -> Unit,
	modifier: Modifier = Modifier,
) {
	Column(modifier = modifier.fillMaxSize()) {
		Text(
			text = stringResource(R.string.shortcut_picker_title),
			style = MaterialTheme.typography.headlineSmall,
			modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
		)
		LazyColumn {
			items(actions, key = ShortcutAction::id) { action ->
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { onPick(action) }
						.padding(horizontal = 24.dp, vertical = 14.dp)
				) {
					Text(
						text = stringResource(action.label),
						style = MaterialTheme.typography.titleMedium
					)
					Text(
						text = stringResource(action.description),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				HorizontalDivider()
			}
		}
	}
}
