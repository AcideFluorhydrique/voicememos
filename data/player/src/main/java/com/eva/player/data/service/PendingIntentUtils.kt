package com.eva.player.data.service

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.eva.utils.IntentConstants
import com.eva.utils.IntentRequestCodes
import com.eva.utils.NavDeepLinks

internal fun Context.createBackStackIntent(audioId: Long): PendingIntent {
	val stackBuilder = TaskStackBuilder.create(applicationContext)
		.addNextIntent(
			Intent().apply {
				setClassName(applicationContext, IntentConstants.MAIN_ACTIVITY)
				data = NavDeepLinks.RECORDER_DESTINATION_PATTERN.toUri()
				action = Intent.ACTION_VIEW
			}
		)
		.addNextIntent(
			Intent().apply {
				setClassName(applicationContext, IntentConstants.MAIN_ACTIVITY)
				data = NavDeepLinks.RECORDING_DESTINATION_PATTERN.toUri()
				action = Intent.ACTION_VIEW
			},
		)
	if (audioId != -1L) {
		stackBuilder.addNextIntent(
			Intent().apply {
				setClassName(applicationContext, IntentConstants.MAIN_ACTIVITY)
				data = NavDeepLinks.audioPlayerDestinationUri(audioId).toUri()
				action = Intent.ACTION_VIEW
			}
		)
	}
	return stackBuilder.getPendingIntent(
		IntentRequestCodes.PLAYER_BACKSTACK_INTENT.code,
		PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
	)
}

internal fun Context.createPlayerIntent(audioId: Long): PendingIntent {
	return Intent().apply {
		setClassName(applicationContext, IntentConstants.MAIN_ACTIVITY)
		data = NavDeepLinks.audioPlayerDestinationUri(audioId).toUri()
		action = Intent.ACTION_VIEW
	}.let { intent ->
		PendingIntent.getActivity(
			applicationContext,
			IntentRequestCodes.PLAYER_NOTIFICATION_INTENT.code,
			intent,
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)
	}
}