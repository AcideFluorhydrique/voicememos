package com.eva.cupertino.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws one of the [CupertinoGlyph] shapes, everything is drawn on a canvas which keeps
 * the icons crisp on every density and keeps the module free of drawable resources.
 *
 * @param glyph shape to draw
 * @param tint color of the glyph
 * @param size the square box the glyph is drawn inside
 * @param weight stroke weight of the outlined glyphs
 */
@Composable
fun CupertinoIcon(
	glyph: CupertinoGlyph,
	tint: Color,
	modifier: Modifier = Modifier,
	size: Dp = 22.dp,
	weight: Dp = 2.dp,
) {
	Canvas(modifier = modifier.size(size)) {
		drawGlyph(glyph = glyph, tint = tint, strokeWidth = weight.toPx())
	}
}

private fun DrawScope.drawGlyph(glyph: CupertinoGlyph, tint: Color, strokeWidth: Float) {
	val width = size.width
	val height = size.height
	val cx = width / 2f
	val cy = height / 2f
	val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

	when (glyph) {
		CupertinoGlyph.CHEVRON_LEFT -> {
			val path = Path().apply {
				moveTo(cx + width * .16f, cy - height * .26f)
				lineTo(cx - width * .16f, cy)
				lineTo(cx + width * .16f, cy + height * .26f)
			}
			drawPath(path, tint, style = stroke)
		}

		CupertinoGlyph.CHEVRON_RIGHT -> {
			val path = Path().apply {
				moveTo(cx - width * .16f, cy - height * .26f)
				lineTo(cx + width * .16f, cy)
				lineTo(cx - width * .16f, cy + height * .26f)
			}
			drawPath(path, tint, style = stroke)
		}

		CupertinoGlyph.CHEVRON_DOWN -> {
			val path = Path().apply {
				moveTo(cx - width * .26f, cy - height * .13f)
				lineTo(cx, cy + height * .13f)
				lineTo(cx + width * .26f, cy - height * .13f)
			}
			drawPath(path, tint, style = stroke)
		}

		CupertinoGlyph.SEARCH -> {
			val radius = width * .28f
			drawCircle(tint, radius = radius, center = Offset(cx - width * .06f, cy - height * .06f), style = stroke)
			val start = Offset(cx - width * .06f + radius * .72f, cy - height * .06f + radius * .72f)
			drawLine(tint, start, Offset(cx + width * .36f, cy + height * .36f), strokeWidth, StrokeCap.Round)
		}

		CupertinoGlyph.CLOSE_FILL -> {
			drawCircle(tint, radius = width * .5f, center = Offset(cx, cy))
		}

		CupertinoGlyph.XMARK -> {
			drawLine(
				tint,
				Offset(cx - width * .26f, cy - height * .26f),
				Offset(cx + width * .26f, cy + height * .26f),
				strokeWidth,
				StrokeCap.Round
			)
			drawLine(
				tint,
				Offset(cx + width * .26f, cy - height * .26f),
				Offset(cx - width * .26f, cy + height * .26f),
				strokeWidth,
				StrokeCap.Round
			)
		}

		CupertinoGlyph.ELLIPSIS -> {
			val radius = width * .075f
			drawCircle(tint, radius, Offset(cx - width * .26f, cy))
			drawCircle(tint, radius, Offset(cx, cy))
			drawCircle(tint, radius, Offset(cx + width * .26f, cy))
		}

		CupertinoGlyph.TRASH -> {
			// lid
			drawLine(
				tint,
				Offset(cx - width * .32f, cy - height * .26f),
				Offset(cx + width * .32f, cy - height * .26f),
				strokeWidth,
				StrokeCap.Round
			)
			// handle
			val handle = Path().apply {
				moveTo(cx - width * .12f, cy - height * .26f)
				lineTo(cx - width * .1f, cy - height * .36f)
				lineTo(cx + width * .1f, cy - height * .36f)
				lineTo(cx + width * .12f, cy - height * .26f)
			}
			drawPath(handle, tint, style = stroke)
			// body
			val body = Path().apply {
				moveTo(cx - width * .24f, cy - height * .18f)
				lineTo(cx - width * .19f, cy + height * .32f)
				lineTo(cx + width * .19f, cy + height * .32f)
				lineTo(cx + width * .24f, cy - height * .18f)
			}
			drawPath(body, tint, style = stroke)
		}

		CupertinoGlyph.SHARE -> {
			// box
			val box = Path().apply {
				moveTo(cx - width * .16f, cy - height * .12f)
				lineTo(cx - width * .28f, cy - height * .12f)
				lineTo(cx - width * .28f, cy + height * .38f)
				lineTo(cx + width * .28f, cy + height * .38f)
				lineTo(cx + width * .28f, cy - height * .12f)
				lineTo(cx + width * .16f, cy - height * .12f)
			}
			drawPath(box, tint, style = stroke)
			// arrow
			drawLine(
				tint,
				Offset(cx, cy + height * .16f),
				Offset(cx, cy - height * .38f),
				strokeWidth,
				StrokeCap.Round
			)
			val head = Path().apply {
				moveTo(cx - width * .14f, cy - height * .24f)
				lineTo(cx, cy - height * .38f)
				lineTo(cx + width * .14f, cy - height * .24f)
			}
			drawPath(head, tint, style = stroke)
		}

		CupertinoGlyph.CHECK -> {
			val path = Path().apply {
				moveTo(cx - width * .28f, cy + height * .02f)
				lineTo(cx - width * .08f, cy + height * .22f)
				lineTo(cx + width * .3f, cy - height * .24f)
			}
			drawPath(path, tint, style = stroke)
		}

		CupertinoGlyph.PLAY -> {
			val path = Path().apply {
				moveTo(cx - width * .24f, cy - height * .32f)
				lineTo(cx + width * .3f, cy)
				lineTo(cx - width * .24f, cy + height * .32f)
				close()
			}
			drawPath(path, tint)
		}

		CupertinoGlyph.PAUSE -> {
			val barWidth = width * .14f
			val barHeight = height * .64f
			val radius = androidx.compose.ui.geometry.CornerRadius(barWidth * .35f, barWidth * .35f)
			drawRoundRect(
				color = tint,
				topLeft = Offset(cx - width * .26f, cy - barHeight / 2f),
				size = Size(barWidth, barHeight),
				cornerRadius = radius,
			)
			drawRoundRect(
				color = tint,
				topLeft = Offset(cx + width * .12f, cy - barHeight / 2f),
				size = Size(barWidth, barHeight),
				cornerRadius = radius,
			)
		}

		CupertinoGlyph.STOP -> {
			val side = width * .58f
			drawRoundRect(
				color = tint,
				topLeft = Offset(cx - side / 2f, cy - side / 2f),
				size = Size(side, side),
				cornerRadius = androidx.compose.ui.geometry.CornerRadius(side * .16f, side * .16f),
			)
		}

		CupertinoGlyph.RECORD -> drawCircle(tint, radius = width * .34f, center = Offset(cx, cy))

		CupertinoGlyph.SKIP_BACK_15, CupertinoGlyph.SKIP_FORWARD_15 -> {
			val isBack = glyph == CupertinoGlyph.SKIP_BACK_15
			val radius = width * .36f
			// circular arc with a gap on the top
			drawArc(
				color = tint,
				startAngle = if (isBack) -60f else -120f,
				sweepAngle = 300f,
				useCenter = false,
				topLeft = Offset(cx - radius, cy - radius),
				size = Size(radius * 2, radius * 2),
				style = stroke,
			)
			// arrow head at the gap
			val headX = if (isBack) cx - radius * .5f else cx + radius * .5f
			val headY = cy - radius * .86f
			val dir = if (isBack) 1f else -1f
			val head = Path().apply {
				moveTo(headX + dir * width * .1f, headY - height * .1f)
				lineTo(headX, headY)
				lineTo(headX + dir * width * .1f, headY + height * .1f)
			}
			drawPath(head, tint, style = stroke)
		}

		CupertinoGlyph.TRIM -> {
			// two brackets facing each other, the way trimming is shown on iOS
			val left = Path().apply {
				moveTo(cx - width * .1f, cy - height * .34f)
				lineTo(cx - width * .3f, cy - height * .34f)
				lineTo(cx - width * .3f, cy + height * .34f)
				lineTo(cx - width * .1f, cy + height * .34f)
			}
			val right = Path().apply {
				moveTo(cx + width * .1f, cy - height * .34f)
				lineTo(cx + width * .3f, cy - height * .34f)
				lineTo(cx + width * .3f, cy + height * .34f)
				lineTo(cx + width * .1f, cy + height * .34f)
			}
			drawPath(left, tint, style = stroke)
			drawPath(right, tint, style = stroke)
			drawLine(
				tint,
				Offset(cx, cy - height * .18f),
				Offset(cx, cy + height * .18f),
				strokeWidth * .8f,
				StrokeCap.Round
			)
		}

		CupertinoGlyph.STAR, CupertinoGlyph.STAR_FILL -> {
			val path = starPath(cx, cy, width * .38f, width * .17f)
			if (glyph == CupertinoGlyph.STAR_FILL) drawPath(path, tint)
			else drawPath(path, tint, style = stroke)
		}

		CupertinoGlyph.GEAR -> {
			val outer = width * .38f
			drawCircle(tint, radius = outer * .82f, center = Offset(cx, cy), style = stroke)
			drawCircle(tint, radius = outer * .28f, center = Offset(cx, cy), style = stroke)
			repeat(8) { index ->
				rotate(degrees = index * 45f, pivot = Offset(cx, cy)) {
					drawLine(
						tint,
						Offset(cx, cy - outer * .82f),
						Offset(cx, cy - outer * 1.15f),
						strokeWidth,
						StrokeCap.Round
					)
				}
			}
		}

		CupertinoGlyph.FOLDER -> {
			val path = Path().apply {
				moveTo(cx - width * .34f, cy + height * .28f)
				lineTo(cx - width * .34f, cy - height * .24f)
				lineTo(cx - width * .06f, cy - height * .24f)
				lineTo(cx + width * .02f, cy - height * .12f)
				lineTo(cx + width * .34f, cy - height * .12f)
				lineTo(cx + width * .34f, cy + height * .28f)
				close()
			}
			drawPath(path, tint, style = stroke)
		}

		CupertinoGlyph.MIC -> {
			val capsuleWidth = width * .26f
			drawRoundRect(
				color = tint,
				topLeft = Offset(cx - capsuleWidth / 2f, cy - height * .38f),
				size = Size(capsuleWidth, height * .46f),
				cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleWidth / 2f, capsuleWidth / 2f),
			)
			drawArc(
				color = tint,
				startAngle = 0f,
				sweepAngle = 180f,
				useCenter = false,
				topLeft = Offset(cx - width * .26f, cy - height * .12f),
				size = Size(width * .52f, height * .4f),
				style = stroke,
			)
			drawLine(
				tint,
				Offset(cx, cy + height * .28f),
				Offset(cx, cy + height * .4f),
				strokeWidth,
				StrokeCap.Round
			)
		}

		CupertinoGlyph.RESTORE -> {
			val radius = width * .34f
			drawArc(
				color = tint,
				startAngle = 30f,
				sweepAngle = 300f,
				useCenter = false,
				topLeft = Offset(cx - radius, cy - radius),
				size = Size(radius * 2, radius * 2),
				style = stroke,
			)
			val head = Path().apply {
				moveTo(cx + radius * .5f, cy - radius * .95f)
				lineTo(cx + radius * .95f, cy - radius * .5f)
				lineTo(cx + radius * .35f, cy - radius * .35f)
			}
			drawPath(head, tint, style = stroke)
		}

		CupertinoGlyph.INFO -> {
			drawCircle(tint, radius = width * .38f, center = Offset(cx, cy), style = stroke)
			drawCircle(tint, radius = strokeWidth * .6f, center = Offset(cx, cy - height * .17f))
			drawLine(
				tint,
				Offset(cx, cy - height * .04f),
				Offset(cx, cy + height * .2f),
				strokeWidth,
				StrokeCap.Round
			)
		}

		CupertinoGlyph.PENCIL -> {
			val body = Path().apply {
				moveTo(cx - width * .3f, cy + height * .3f)
				lineTo(cx - width * .22f, cy + height * .08f)
				lineTo(cx + width * .18f, cy - height * .32f)
				lineTo(cx + width * .32f, cy - height * .18f)
				lineTo(cx - width * .08f, cy + height * .22f)
				close()
			}
			drawPath(body, tint, style = stroke)
		}

		CupertinoGlyph.WAVEFORM -> {
			val factors = floatArrayOf(.24f, .48f, .82f, .48f, .24f)
			factors.forEachIndexed { index, factor ->
				val x = cx + (index - 2) * width * .17f
				drawLine(
					tint,
					Offset(x, cy - height * factor / 2f),
					Offset(x, cy + height * factor / 2f),
					strokeWidth,
					StrokeCap.Round
				)
			}
		}
	}
}

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path {
	val path = Path()
	repeat(10) { index ->
		val radius = if (index % 2 == 0) outer else inner
		val angle = Math.toRadians((index * 36.0) - 90.0)
		val x = cx + (radius * kotlin.math.cos(angle)).toFloat()
		val y = cy + (radius * kotlin.math.sin(angle)).toFloat()
		if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
	}
	path.close()
	return path
}
