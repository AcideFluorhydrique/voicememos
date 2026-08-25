package com.eva.cupertino.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eva.cupertino.theme.CupertinoTheme

/**iOS segmented picker, the pill that highlights the active segment*/
@Composable
fun <T> CupertinoSegmentedControl(
	options: List<T>,
	selected: T,
	onSelect: (T) -> Unit,
	label: @Composable (T) -> String,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	Row(
		modifier = modifier
			.fillMaxWidth()
			.height(32.dp)
			.background(color = colors.tertiaryFill, shape = RoundedCornerShape(9.dp))
			.padding(2.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		options.forEach { option ->
			val isSelected = option == selected
			Box(
				modifier = Modifier
					.weight(1f)
					.height(28.dp)
					.pressable(pressedAlpha = .6f) { onSelect(option) }
					.background(
						color = if (isSelected) colors.secondaryGroupedBackground
						else androidx.compose.ui.graphics.Color.Transparent,
						shape = RoundedCornerShape(7.dp),
					),
				contentAlignment = Alignment.Center,
			) {
				CupertinoText(
					text = label(option),
					style = CupertinoTheme.typography.footnote.copy(
						fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
					),
					maxLines = 1,
				)
			}
		}
	}
}
