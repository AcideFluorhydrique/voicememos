package com.eva.feature_editor.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
fun EditorBackHandlerDialog(
	showDialog: Boolean,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
	properties: DialogProperties = DialogProperties()
) {
	val lifeCycleOwner = LocalLifecycleOwner.current
	val lifecycleState by lifeCycleOwner.lifecycle.currentStateFlow.collectAsState()

	if (!showDialog) return

	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			Button(
				onClick = onConfirm,
				enabled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.errorContainer,
					contentColor = MaterialTheme.colorScheme.onErrorContainer
				)
			) {
				Text(
					text = stringResource(R.string.action_exit),
					fontWeight = FontWeight.SemiBold
				)
			}
		},
		dismissButton = {
			TextButton(
				onClick = onDismiss,
				enabled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
				colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
			) {
				Text(stringResource(R.string.action_cancel))
			}
		},
		title = { Text(text = stringResource(R.string.editor_screen_exiting_warning_dialog_title)) },
		text = { Text(text = stringResource(R.string.editor_screen_exiting_warning_dialog_desc)) },
		modifier = modifier,
		properties = properties,
	)
}

@PreviewLightDark
@Composable
private fun EditorBackHandlerDialogPreview() = RecorderAppTheme {
	EditorBackHandlerDialog(showDialog = true, onConfirm = {}, onDismiss = {})
}