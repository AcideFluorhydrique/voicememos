package com.eva.cupertino.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eva.cupertino.theme.CupertinoTheme

/**The iOS toggle, 51x31 with a green track when checked*/
@Composable
fun CupertinoSwitch(
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
) {
	val colors = CupertinoTheme.colors

	val trackColor by animateColorAsState(
		targetValue = if (checked) colors.success else colors.fill,
		label = "switch-track"
	)
	val knobOffset by animateDpAsState(
		targetValue = if (checked) 20.dp else 0.dp,
		label = "switch-knob"
	)

	Box(
		modifier = modifier
			.pressable(enabled = enabled, pressedAlpha = .8f) { onCheckedChange(!checked) }
			.width(51.dp)
			.height(31.dp)
			.background(color = trackColor, shape = CircleShape)
			.padding(2.dp),
		contentAlignment = Alignment.CenterStart,
	) {
		Box(
			modifier = Modifier
				.offset(x = knobOffset)
				.size(27.dp)
				.background(color = Color.White, shape = CircleShape)
		)
	}
}
