package com.eva.cupertino.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eva.cupertino.theme.CupertinoTheme

/**
 * The iOS navigation bar, 44dp tall with a leading and a trailing slot. The inline title
 * only shows up once the large title has been scrolled away, matching the iOS behaviour.
 */
@Composable
fun CupertinoNavBar(
	title: String,
	modifier: Modifier = Modifier,
	isCollapsed: Boolean = false,
	showBackgroundWhenExpanded: Boolean = false,
	leading: @Composable RowScope.() -> Unit = {},
	trailing: @Composable RowScope.() -> Unit = {},
) {
	val colors = CupertinoTheme.colors

	val titleAlpha by animateFloatAsState(
		targetValue = if (isCollapsed) 1f else 0f,
		label = "inline-title"
	)
	val barAlpha by animateFloatAsState(
		targetValue = if (isCollapsed || showBackgroundWhenExpanded) 1f else 0f,
		label = "bar-background"
	)

	Column(
		modifier = modifier
			.fillMaxWidth()
			.background(colors.barBackground.copy(alpha = colors.barBackground.alpha * barAlpha))
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.statusBarsPadding()
				.height(44.dp)
				.padding(horizontal = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Row(
				modifier = Modifier.weight(1f),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.Start,
				content = leading,
			)
			Box(
				modifier = Modifier.weight(2f),
				contentAlignment = Alignment.Center,
			) {
				CupertinoText(
					text = title,
					style = CupertinoTheme.typography.headline,
					maxLines = 1,
					textAlign = TextAlign.Center,
					modifier = Modifier.alpha(titleAlpha),
				)
			}
			Row(
				modifier = Modifier.weight(1f),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.End,
				content = trailing,
			)
		}
		if (barAlpha > 0.01f) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(0.5.dp)
					.background(colors.separator.copy(alpha = colors.separator.alpha * barAlpha))
			)
		}
	}
}

/**The oversized title that sits on top of a scrollable list*/
@Composable
fun CupertinoLargeTitle(
	text: String,
	modifier: Modifier = Modifier,
	color: Color = CupertinoTheme.colors.label,
) = CupertinoText(
	text = text,
	style = CupertinoTheme.typography.largeTitle,
	color = color,
	maxLines = 1,
	modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
)
