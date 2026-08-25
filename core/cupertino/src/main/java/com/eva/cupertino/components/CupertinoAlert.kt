package com.eva.cupertino.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eva.cupertino.theme.CupertinoTheme

/**One button of a [CupertinoAlert] or a [CupertinoActionSheet]*/
data class CupertinoDialogAction(
	val text: String,
	val isDestructive: Boolean = false,
	val isPreferred: Boolean = false,
	val onClick: () -> Unit,
)

/**The centered iOS alert with hairline separated buttons*/
@Composable
fun CupertinoAlert(
	title: String,
	actions: List<CupertinoDialogAction>,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	message: String? = null,
) {
	val colors = CupertinoTheme.colors

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
					.padding(horizontal = 16.dp, vertical = 19.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(4.dp),
			) {
				CupertinoText(
					text = title,
					style = CupertinoTheme.typography.headline,
					textAlign = TextAlign.Center,
					maxLines = 3,
				)
				message?.let { text ->
					CupertinoText(
						text = text,
						style = CupertinoTheme.typography.footnote,
						color = colors.label,
						textAlign = TextAlign.Center,
					)
				}
			}
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(0.5.dp)
					.background(colors.separator)
			)
			if (actions.size == 2) {
				Row(modifier = Modifier.fillMaxWidth()) {
					AlertButton(
						action = actions[0],
						onDismissRequest = onDismissRequest,
						modifier = Modifier.weight(1f),
					)
					Box(
						modifier = Modifier
							.width(0.5.dp)
							.height(44.dp)
							.background(colors.separator)
					)
					AlertButton(
						action = actions[1],
						onDismissRequest = onDismissRequest,
						modifier = Modifier.weight(1f),
					)
				}
			} else {
				actions.forEachIndexed { index, action ->
					if (index != 0) {
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.height(0.5.dp)
								.background(colors.separator)
						)
					}
					AlertButton(
						action = action,
						onDismissRequest = onDismissRequest,
						modifier = Modifier.fillMaxWidth(),
					)
				}
			}
		}
	}
}

@Composable
private fun AlertButton(
	action: CupertinoDialogAction,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	Box(
		modifier = modifier
			.pressable(pressedAlpha = .5f) {
				onDismissRequest()
				action.onClick()
			}
			.heightIn(min = 44.dp)
			.padding(horizontal = 8.dp, vertical = 12.dp),
		contentAlignment = Alignment.Center,
	) {
		CupertinoText(
			text = action.text,
			style = CupertinoTheme.typography.body.copy(
				fontWeight = if (action.isPreferred) FontWeight.SemiBold else FontWeight.Normal
			),
			color = if (action.isDestructive) colors.destructive else colors.accent,
			maxLines = 1,
			textAlign = TextAlign.Center,
		)
	}
}

/**The bottom anchored action sheet, actions on top and a detached cancel button below*/
@Composable
fun CupertinoActionSheet(
	actions: List<CupertinoDialogAction>,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	title: String? = null,
	cancelText: String = "Cancel",
) {
	val colors = CupertinoTheme.colors
	val panelColor = if (colors.isDark) colors.tertiarySystemBackground else Color(0xFFF7F7F7)

	Dialog(
		onDismissRequest = onDismissRequest,
		properties = DialogProperties(usePlatformDefaultWidth = false),
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.pressable(pressedAlpha = 1f, onClick = onDismissRequest),
			contentAlignment = Alignment.BottomCenter,
		) {
			Column(
				modifier = modifier
					.fillMaxWidth()
					.navigationBarsPadding()
					.padding(horizontal = 8.dp, vertical = 8.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(14.dp))
						.background(panelColor),
				) {
					title?.let { text ->
						CupertinoText(
							text = text,
							style = CupertinoTheme.typography.footnote,
							color = colors.secondaryLabel,
							textAlign = TextAlign.Center,
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 16.dp, vertical = 14.dp),
						)
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.height(0.5.dp)
								.background(colors.separator)
						)
					}
					actions.forEachIndexed { index, action ->
						if (index != 0) {
							Box(
								modifier = Modifier
									.fillMaxWidth()
									.height(0.5.dp)
									.background(colors.separator)
							)
						}
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.pressable(pressedAlpha = .5f) {
									onDismissRequest()
									action.onClick()
								}
								.heightIn(min = 57.dp)
								.padding(horizontal = 16.dp, vertical = 16.dp),
							contentAlignment = Alignment.Center,
						) {
							CupertinoText(
								text = action.text,
								style = CupertinoTheme.typography.title3.copy(
									fontWeight = if (action.isPreferred) FontWeight.SemiBold
									else FontWeight.Normal
								),
								color = if (action.isDestructive) colors.destructive else colors.accent,
								textAlign = TextAlign.Center,
							)
						}
					}
				}
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(14.dp))
						.background(colors.secondaryGroupedBackground)
						.pressable(pressedAlpha = .5f, onClick = onDismissRequest)
						.heightIn(min = 57.dp)
						.padding(vertical = 16.dp),
					contentAlignment = Alignment.Center,
				) {
					CupertinoText(
						text = cancelText,
						style = CupertinoTheme.typography.title3.copy(fontWeight = FontWeight.SemiBold),
						color = colors.accent,
					)
				}
			}
		}
	}
}
