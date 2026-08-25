package com.eva.cupertino.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**One of the coloured buttons revealed when a row is swiped to the left*/
data class CupertinoSwipeAction(
	val label: String,
	val glyph: CupertinoGlyph,
	val background: Color,
	val onClick: () -> Unit,
)

/**
 * Swipe a row towards the leading edge to uncover its actions, the same interaction iOS
 * uses on mail, notes and voice memos.
 *
 * @param isRevealed drives the row from the outside, useful to close every other row
 */
@Composable
fun CupertinoSwipeableRow(
	actions: List<CupertinoSwipeAction>,
	modifier: Modifier = Modifier,
	actionWidth: Dp = 74.dp,
	isRevealed: Boolean = false,
	onRevealChange: (Boolean) -> Unit = {},
	content: @Composable () -> Unit,
) {
	val density = LocalDensity.current
	val scope = rememberCoroutineScope()

	val maxOffset = with(density) { (actionWidth * actions.size).toPx() }
	val offsetX = remember { Animatable(0f) }
	val currentOnRevealChange by rememberUpdatedState(onRevealChange)

	LaunchedEffect(isRevealed, maxOffset) {
		val target = if (isRevealed) -maxOffset else 0f
		if (offsetX.value != target) offsetX.animateTo(target)
	}

	Box(modifier = modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier.matchParentSize(),
			horizontalArrangement = Arrangement.End,
		) {
			actions.forEach { action ->
				Column(
					modifier = Modifier
						.width(actionWidth)
						.fillMaxHeight()
						.background(action.background)
						.pressable(pressedAlpha = .7f) {
							scope.launch { offsetX.animateTo(0f) }
							currentOnRevealChange(false)
							action.onClick()
						},
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					CupertinoIcon(glyph = action.glyph, tint = Color.White, size = 22.dp)
					CupertinoText(
						text = action.label,
						style = CupertinoTheme.typography.caption2,
						color = Color.White,
						maxLines = 1,
					)
				}
			}
		}
		Box(
			modifier = Modifier
				.offset { IntOffset(offsetX.value.roundToInt(), 0) }
				.pointerInput(maxOffset) {
					detectHorizontalDragGestures(
						onDragEnd = {
							val shouldOpen = offsetX.value < -maxOffset / 2f
							scope.launch { offsetX.animateTo(if (shouldOpen) -maxOffset else 0f) }
							currentOnRevealChange(shouldOpen)
						},
						onDragCancel = {
							scope.launch { offsetX.animateTo(0f) }
							currentOnRevealChange(false)
						},
						onHorizontalDrag = { change, dragAmount ->
							change.consume()
							val next = (offsetX.value + dragAmount).coerceIn(-maxOffset, 0f)
							scope.launch { offsetX.snapTo(next) }
						},
					)
				},
		) {
			content()
		}
	}
}
