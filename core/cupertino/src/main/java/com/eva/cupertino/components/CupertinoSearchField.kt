package com.eva.cupertino.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme

/**The rounded grey search bar seen on top of most iOS lists*/
@Composable
fun CupertinoSearchField(
	value: String,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	placeholder: String = "Search",
) {
	val colors = CupertinoTheme.colors

	val selectionColors = TextSelectionColors(
		handleColor = colors.accent,
		backgroundColor = colors.accent.copy(alpha = .3f),
	)

	CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
		Row(
			modifier = modifier
				.fillMaxWidth()
				.height(36.dp)
				.background(color = colors.tertiaryFill, shape = RoundedCornerShape(10.dp))
				.padding(horizontal = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			CupertinoIcon(
				glyph = CupertinoGlyph.SEARCH,
				tint = colors.secondaryLabel,
				size = 16.dp,
				weight = 1.6.dp,
			)
			Box(
				modifier = Modifier
					.weight(1f)
					.padding(horizontal = 6.dp),
				contentAlignment = Alignment.CenterStart,
			) {
				if (value.isEmpty()) {
					CupertinoText(
						text = placeholder,
						style = CupertinoTheme.typography.body,
						color = colors.secondaryLabel,
						maxLines = 1,
					)
				}
				BasicTextField(
					value = value,
					onValueChange = onValueChange,
					singleLine = true,
					textStyle = CupertinoTheme.typography.body.copy(color = colors.label),
					cursorBrush = SolidColor(colors.accent),
					keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
					modifier = Modifier.fillMaxWidth(),
				)
			}
			if (value.isNotEmpty()) {
				Box(
					modifier = Modifier
						.size(18.dp)
						.pressable { onValueChange("") },
					contentAlignment = Alignment.Center,
				) {
					CupertinoIcon(
						glyph = CupertinoGlyph.CLOSE_FILL,
						tint = colors.secondaryLabel,
						size = 16.dp,
					)
					CupertinoIcon(
						glyph = CupertinoGlyph.XMARK,
						tint = colors.secondarySystemBackground,
						size = 10.dp,
						weight = 1.6.dp,
					)
				}
			}
		}
	}
}
