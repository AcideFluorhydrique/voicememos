package com.eva.cupertino.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eva.cupertino.theme.CupertinoTheme

/**
 * iOS has no snackbars, messages arrive as a floating capsule. This keeps the existing
 * message plumbing but draws it the iOS way.
 */
@Composable
fun CupertinoBannerHost(
	hostState: SnackbarHostState,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	SnackbarHost(
		hostState = hostState,
		modifier = modifier,
	) { data ->
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 8.dp)
				.background(
					color = if (colors.isDark) colors.tertiarySystemBackground
					else colors.secondaryGroupedBackground,
					shape = RoundedCornerShape(14.dp),
				)
				.padding(horizontal = 16.dp, vertical = 12.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			CupertinoText(
				text = data.visuals.message,
				style = CupertinoTheme.typography.subHeadline,
				maxLines = 2,
				modifier = Modifier.padding(end = 8.dp),
			)
			data.visuals.actionLabel?.let { label ->
				CupertinoTextButton(
					text = label,
					onClick = { data.performAction() },
				)
			}
		}
	}
}
