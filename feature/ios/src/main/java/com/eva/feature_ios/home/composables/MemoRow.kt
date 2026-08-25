package com.eva.feature_ios.home.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoSeparator
import com.eva.cupertino.components.CupertinoSwipeAction
import com.eva.cupertino.components.CupertinoSwipeableRow
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.components.pressable
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.cupertino.util.asClockString
import com.eva.feature_ios.R
import com.eva.feature_ios.util.asMemoDateLabel
import com.eva.recordings.domain.models.RecordedVoiceModel
import androidx.compose.ui.res.stringResource

/**
 * A row of the recordings list, tapping it slides the player open right below the title
 * the same way voice memos does.
 */
@Composable
internal fun MemoRow(
	memo: RecordedVoiceModel,
	isExpanded: Boolean,
	onClick: () -> Unit,
	onDelete: () -> Unit,
	onShare: () -> Unit,
	modifier: Modifier = Modifier,
	isRevealed: Boolean = false,
	onRevealChange: (Boolean) -> Unit = {},
	expandedContent: @Composable () -> Unit = {},
) {
	val colors = CupertinoTheme.colors

	val swipeActions = listOf(
		CupertinoSwipeAction(
			label = stringResource(R.string.action_share),
			glyph = CupertinoGlyph.SHARE,
			background = colors.accent,
			onClick = onShare,
		),
		CupertinoSwipeAction(
			label = stringResource(R.string.action_delete),
			glyph = CupertinoGlyph.TRASH,
			background = colors.destructive,
			onClick = onDelete,
		),
	)

	val dateLabel = memo.recordedAt.asMemoDateLabel(
		todayLabel = stringResource(R.string.label_today),
		yesterdayLabel = stringResource(R.string.label_yesterday),
	)

	Column(
		modifier = modifier
			.fillMaxWidth()
			.background(colors.systemBackground)
	) {
		CupertinoSwipeableRow(
			actions = swipeActions,
			isRevealed = isRevealed,
			onRevealChange = onRevealChange,
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.background(colors.systemBackground)
					.pressable(pressedAlpha = .55f, onClick = onClick)
					.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Column(modifier = Modifier.weight(1f)) {
					CupertinoText(
						text = memo.title,
						style = CupertinoTheme.typography.headline,
						maxLines = 1,
					)
					CupertinoText(
						text = dateLabel,
						style = CupertinoTheme.typography.footnote,
						color = colors.secondaryLabel,
						maxLines = 1,
					)
				}
				if (memo.isFavorite) {
					CupertinoIcon(
						glyph = CupertinoGlyph.STAR_FILL,
						tint = colors.trim,
						size = 14.dp,
					)
				}
				if (!isExpanded) {
					CupertinoText(
						text = memo.duration.asClockString(),
						style = CupertinoTheme.typography.footnote,
						color = colors.secondaryLabel,
						maxLines = 1,
					)
				}
			}
		}
		AnimatedVisibility(
			visible = isExpanded,
			enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(220)),
			exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120)),
		) {
			expandedContent()
		}
		CupertinoSeparator(startInset = 16.dp)
	}
}
