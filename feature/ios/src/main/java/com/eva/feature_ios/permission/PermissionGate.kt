package com.eva.feature_ios.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.eva.cupertino.components.CupertinoFilledButton
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.components.CupertinoTextButton
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.feature_ios.R

/**
 * Checks a runtime permission and keeps the answer in state, the recorder needs this for
 * the microphone and the list needs it for the media store.
 */
@Composable
internal fun rememberPermissionState(permission: String): PermissionState {
	val context = LocalContext.current

	var isGranted by remember(permission) {
		val granted = ContextCompat.checkSelfPermission(context, permission) ==
				android.content.pm.PackageManager.PERMISSION_GRANTED
		mutableStateOf(granted)
	}

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission(),
		onResult = { result -> isGranted = result },
	)

	return remember(isGranted, permission) {
		PermissionState(
			isGranted = isGranted,
			onRequest = { launcher.launch(permission) },
		)
	}
}

internal data class PermissionState(
	val isGranted: Boolean,
	val onRequest: () -> Unit,
)

/**The full screen explainer shown while a permission is missing*/
@Composable
internal fun PermissionRequestBox(
	title: String,
	message: String,
	onRequest: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	val colors = CupertinoTheme.colors

	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(horizontal = 32.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		CupertinoIcon(
			glyph = CupertinoGlyph.MIC,
			tint = colors.tertiaryLabel,
			size = 56.dp,
			weight = 2.5.dp,
		)
		CupertinoText(
			text = title,
			style = CupertinoTheme.typography.title3,
			textAlign = TextAlign.Center,
		)
		CupertinoText(
			text = message,
			style = CupertinoTheme.typography.subHeadline,
			color = colors.secondaryLabel,
			textAlign = TextAlign.Center,
		)
		CupertinoFilledButton(
			text = stringResource(R.string.action_allow_access),
			onClick = onRequest,
			modifier = Modifier.padding(top = 8.dp),
		)
		CupertinoTextButton(
			text = stringResource(R.string.action_open_settings),
			onClick = {
				val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
					data = Uri.fromParts("package", context.packageName, null)
					addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				}
				context.startActivity(intent)
			},
		)
	}
}
