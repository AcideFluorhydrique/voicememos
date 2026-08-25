package com.eva.cupertino.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme

/**A hairline separator, indented like the ones inside iOS lists*/
@Composable
fun CupertinoSeparator(
	modifier: Modifier = Modifier,
	startInset: Dp = 16.dp,
	endInset: Dp = 0.dp,
) {
	val colors = CupertinoTheme.colors
	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(start = startInset, end = endInset)
			.height(0.5.dp)
			.background(colors.separator)
	)
}

/**
 * An inset grouped section, the rounded white (or dark grey) card that holds rows,
 * with an optional caption above and below.
 */
@Composable
fun CupertinoSection(
	modifier: Modifier = Modifier,
	header: String? = null,
	footer: String? = null,
	content: @Composable ColumnScope.() -> Unit,
) {
	val colors = CupertinoTheme.colors

	Column(modifier = modifier.fillMaxWidth()) {
		header?.let { text ->
			CupertinoText(
				text = text.uppercase(),
				style = CupertinoTheme.typography.footnote,
				color = colors.secondaryLabel,
				modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 6.dp),
			)
		}
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp)
				.clip(RoundedCornerShape(10.dp))
				.background(colors.secondaryGroupedBackground),
			content = content,
		)
		footer?.let { text ->
			CupertinoText(
				text = text,
				style = CupertinoTheme.typography.footnote,
				color = colors.secondaryLabel,
				modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 6.dp),
			)
		}
	}
}

/**
 * A single row inside a [CupertinoSection].
 */
@Composable
fun CupertinoRow(
	title: String,
	modifier: Modifier = Modifier,
	subTitle: String? = null,
	titleColor: Color = CupertinoTheme.colors.label,
	detail: String? = null,
	showChevron: Boolean = false,
	onClick: (() -> Unit)? = null,
	leading: (@Composable () -> Unit)? = null,
	trailing: (@Composable RowScope.() -> Unit)? = null,
) {
	val colors = CupertinoTheme.colors

	val clickModifier = if (onClick != null) Modifier.pressable(
		pressedAlpha = .6f,
		onClick = onClick
	) else Modifier

	Row(
		modifier = modifier
			.fillMaxWidth()
			.then(clickModifier)
			.heightIn(min = 44.dp)
			.padding(horizontal = 16.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp),
	) {
		leading?.invoke()
		Column(modifier = Modifier.weight(1f)) {
			CupertinoText(text = title, color = titleColor, maxLines = 1)
			subTitle?.let { text ->
				CupertinoText(
					text = text,
					style = CupertinoTheme.typography.footnote,
					color = colors.secondaryLabel,
					maxLines = 1,
				)
			}
		}
		detail?.let { text ->
			CupertinoText(
				text = text,
				color = colors.secondaryLabel,
				maxLines = 1,
			)
		}
		trailing?.invoke(this)
		if (showChevron) {
			CupertinoIcon(
				glyph = CupertinoGlyph.CHEVRON_RIGHT,
				tint = colors.tertiaryLabel,
				size = 16.dp,
				weight = 2.dp,
			)
		}
	}
}
