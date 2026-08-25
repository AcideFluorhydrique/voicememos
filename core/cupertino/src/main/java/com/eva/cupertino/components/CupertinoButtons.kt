package com.eva.cupertino.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme

/**
 * iOS buttons do not ripple, they dim and shrink slightly, [pressable] reproduces that.
 */
@Composable
fun Modifier.pressable(
	enabled: Boolean = true,
	pressedAlpha: Float = 0.4f,
	pressedScale: Float = 1f,
	onClick: () -> Unit,
): Modifier {
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()

	val alpha by animateFloatAsState(
		targetValue = if (isPressed) pressedAlpha else 1f,
		label = "press-alpha"
	)
	val scale by animateFloatAsState(
		targetValue = if (isPressed) pressedScale else 1f,
		label = "press-scale"
	)

	return this
		.alpha(if (enabled) alpha else .3f)
		.scale(scale)
		.clickable(
			interactionSource = interactionSource,
			indication = null,
			enabled = enabled,
			onClick = onClick,
		)
}

/**A borderless text button, the bread and butter of iOS navigation bars*/
@Composable
fun CupertinoTextButton(
	text: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	color: Color = CupertinoTheme.colors.accent,
	style: TextStyle = CupertinoTheme.typography.body,
	enabled: Boolean = true,
	contentPadding: PaddingValues = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
) {
	Box(
		modifier = modifier
			.pressable(enabled = enabled, onClick = onClick)
			.padding(contentPadding),
		contentAlignment = Alignment.Center,
	) {
		CupertinoText(text = text, style = style, color = color, maxLines = 1)
	}
}

/**A circular icon button, used all over the player controls*/
@Composable
fun CupertinoIconButton(
	glyph: CupertinoGlyph,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	tint: Color = CupertinoTheme.colors.label,
	background: Color = Color.Transparent,
	buttonSize: Dp = 36.dp,
	iconSize: Dp = 22.dp,
	iconWeight: Dp = 2.dp,
	enabled: Boolean = true,
) {
	Box(
		modifier = modifier
			.pressable(enabled = enabled, onClick = onClick)
			.size(buttonSize)
			.background(color = background, shape = CircleShape),
		contentAlignment = Alignment.Center,
	) {
		CupertinoIcon(glyph = glyph, tint = tint, size = iconSize, weight = iconWeight)
	}
}

/**A filled capsule button, the iOS "prominent" style*/
@Composable
fun CupertinoFilledButton(
	text: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	containerColor: Color = CupertinoTheme.colors.accent,
	contentColor: Color = Color.White,
	shape: Shape = RoundedCornerShape(12.dp),
	enabled: Boolean = true,
	contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
) {
	Box(
		modifier = modifier
			.pressable(enabled = enabled, onClick = onClick)
			.background(color = containerColor, shape = shape)
			.padding(contentPadding),
		contentAlignment = Alignment.Center,
	) {
		CupertinoText(
			text = text,
			style = CupertinoTheme.typography.headline,
			color = contentColor,
			maxLines = 1,
		)
	}
}
