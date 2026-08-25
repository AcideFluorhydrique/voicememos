package com.eva.cupertino.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import com.eva.cupertino.theme.CupertinoTheme

/**A thin wrapper so that every text defaults to the iOS body style and label color*/
@Composable
fun CupertinoText(
	text: String,
	modifier: Modifier = Modifier,
	style: TextStyle = CupertinoTheme.typography.body,
	color: Color = CupertinoTheme.colors.label,
	maxLines: Int = Int.MAX_VALUE,
	textAlign: TextAlign? = null,
	overflow: TextOverflow = TextOverflow.Ellipsis,
) = Text(
	text = text,
	modifier = modifier,
	style = style,
	color = color,
	maxLines = maxLines,
	textAlign = textAlign,
	overflow = overflow,
)
