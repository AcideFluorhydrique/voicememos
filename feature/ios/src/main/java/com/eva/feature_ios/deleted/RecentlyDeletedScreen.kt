package com.eva.feature_ios.deleted

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoActionSheet
import com.eva.cupertino.components.CupertinoAlert
import com.eva.cupertino.components.CupertinoDialogAction
import com.eva.cupertino.components.CupertinoNavBar
import com.eva.cupertino.components.CupertinoRow
import com.eva.cupertino.components.CupertinoSection
import com.eva.cupertino.components.CupertinoSeparator
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.components.CupertinoTextButton
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.feature_ios.R
import com.eva.feature_ios.util.asFullDateLabel
import com.eva.recordings.domain.models.TrashRecordingModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun RecentlyDeletedScreen(
	recordings: ImmutableList<TrashRecordingModel>,
	isLoaded: Boolean,
	onRestore: (TrashRecordingModel) -> Unit,
	onDeleteForever: (TrashRecordingModel) -> Unit,
	onDeleteAll: () -> Unit,
	onNavigateBack: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	var selectedRecording by remember { mutableStateOf<TrashRecordingModel?>(null) }
	var showDeleteAllAlert by remember { mutableStateOf(false) }

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(colors.groupedBackground)
	) {
		CupertinoNavBar(
			title = stringResource(R.string.deleted_title),
			isCollapsed = true,
			showBackgroundWhenExpanded = true,
			leading = {
				CupertinoTextButton(
					text = stringResource(R.string.action_back),
					onClick = onNavigateBack,
				)
			},
			trailing = {
				if (recordings.isNotEmpty()) {
					CupertinoTextButton(
						text = stringResource(R.string.deleted_delete_all),
						color = colors.destructive,
						onClick = { showDeleteAllAlert = true },
					)
				}
			},
		)
		LazyColumn(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
				.navigationBarsPadding(),
			contentPadding = PaddingValues(vertical = 16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			if (recordings.isEmpty()) {
				item(key = "empty") {
					EmptyDeletedBox(isLoaded = isLoaded)
				}
			} else {
				item(key = "recordings") {
					CupertinoSection(footer = stringResource(R.string.deleted_footer)) {
						recordings.forEachIndexed { index, recording ->
							if (index != 0) CupertinoSeparator()
							CupertinoRow(
								title = recording.title,
								subTitle = stringResource(
									R.string.deleted_expires,
									recording.expiresAt.asFullDateLabel(),
								),
								showChevron = true,
								onClick = { selectedRecording = recording },
							)
						}
					}
				}
			}
		}
	}

	val recording = selectedRecording
	if (recording != null) {
		CupertinoActionSheet(
			title = recording.title,
			onDismissRequest = { selectedRecording = null },
			cancelText = stringResource(R.string.action_cancel),
			actions = listOf(
				CupertinoDialogAction(
					text = stringResource(R.string.action_restore),
					onClick = { onRestore(recording) },
				),
				CupertinoDialogAction(
					text = stringResource(R.string.action_delete_forever),
					isDestructive = true,
					onClick = { onDeleteForever(recording) },
				),
			),
		)
	}

	if (showDeleteAllAlert) {
		CupertinoAlert(
			title = stringResource(R.string.deleted_delete_all_title),
			message = stringResource(R.string.deleted_delete_all_message),
			onDismissRequest = { showDeleteAllAlert = false },
			actions = listOf(
				CupertinoDialogAction(text = stringResource(R.string.action_cancel), onClick = {}),
				CupertinoDialogAction(
					text = stringResource(R.string.deleted_delete_all),
					isDestructive = true,
					onClick = onDeleteAll,
				),
			),
		)
	}
}

@Composable
private fun EmptyDeletedBox(
	isLoaded: Boolean,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	Column(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 32.dp, vertical = 80.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		CupertinoIcon(
			glyph = CupertinoGlyph.TRASH,
			tint = colors.tertiaryLabel,
			size = 44.dp,
			weight = 2.5.dp,
		)
		CupertinoText(
			text = stringResource(
				if (isLoaded) R.string.deleted_empty_title else R.string.memos_loading
			),
			style = CupertinoTheme.typography.title3,
			color = colors.secondaryLabel,
			textAlign = TextAlign.Center,
		)
		if (isLoaded) {
			CupertinoText(
				text = stringResource(R.string.deleted_empty_subtitle),
				style = CupertinoTheme.typography.subHeadline,
				color = colors.tertiaryLabel,
				textAlign = TextAlign.Center,
			)
		}
	}
}
