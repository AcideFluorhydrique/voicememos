package com.eva.feature_ios.home.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.components.pressable
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.feature_ios.R

/**The iOS alert with a single text field, used to rename a recording*/
@Composable
internal fun RenameMemoDialog(
	initialName: String,
	onDismissRequest: () -> Unit,
	onConfirm: (String) -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors
	var name by remember(initialName) { mutableStateOf(initialName) }

	Dialog(
		onDismissRequest = onDismissRequest,
		properties = DialogProperties(usePlatformDefaultWidth = false),
	) {
		Column(
			modifier = modifier
				.width(270.dp)
				.clip(RoundedCornerShape(14.dp))
				.background(if (colors.isDark) colors.tertiarySystemBackground else Color(0xFFF2F2F2)),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 18.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				CupertinoText(
					text = stringResource(R.string.rename_title),
					style = CupertinoTheme.typography.headline,
					textAlign = TextAlign.Center,
				)
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.background(
							color = colors.secondaryGroupedBackground,
							shape = RoundedCornerShape(6.dp),
						)
						.border(
							width = 0.5.dp,
							color = colors.separator,
							shape = RoundedCornerShape(6.dp),
						)
						.padding(horizontal = 8.dp, vertical = 8.dp),
					contentAlignment = Alignment.CenterStart,
				) {
					BasicTextField(
						value = name,
						onValueChange = { value -> name = value },
						singleLine = true,
						textStyle = CupertinoTheme.typography.subHeadline.copy(color = colors.label),
						cursorBrush = SolidColor(colors.accent),
						modifier = Modifier.fillMaxWidth(),
					)
				}
			}
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(0.5.dp)
					.background(colors.separator)
			)
			Row(modifier = Modifier.fillMaxWidth()) {
				DialogButton(
					text = stringResource(R.string.action_cancel),
					onClick = onDismissRequest,
					modifier = Modifier.weight(1f),
				)
				Box(
					modifier = Modifier
						.width(0.5.dp)
						.height(44.dp)
						.background(colors.separator)
				)
				DialogButton(
					text = stringResource(R.string.action_save),
					isPreferred = true,
					onClick = {
						onConfirm(name.trim())
						onDismissRequest()
					},
					modifier = Modifier.weight(1f),
				)
			}
		}
	}
}

@Composable
private fun DialogButton(
	text: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	isPreferred: Boolean = false,
) {
	val colors = CupertinoTheme.colors

	Box(
		modifier = modifier
			.pressable(pressedAlpha = .5f, onClick = onClick)
			.heightIn(min = 44.dp)
			.padding(vertical = 12.dp),
		contentAlignment = Alignment.Center,
	) {
		CupertinoText(
			text = text,
			style = CupertinoTheme.typography.body.copy(
				fontWeight = if (isPreferred) FontWeight.SemiBold else FontWeight.Normal
			),
			color = colors.accent,
		)
	}
}
