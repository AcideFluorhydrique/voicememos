package com.eva.player.data.service

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "PLAYER_SERVICE"

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MediaPlayerService : MediaSessionService() {

	@Inject
	lateinit var mediaSession: MediaSession

	@Inject
	lateinit var notification: MediaNotification.Provider

	private val listener = object : Listener {
		override fun onForegroundServiceStartNotAllowedException() {
			Log.e(TAG, "CANNOT START FOREGROUND SERVICE")
			stopSelf()
		}
	}

	override fun onCreate() {
		super.onCreate()
		setMediaNotificationProvider(notification)
		Log.d(TAG, "MEDIA SESSION SERVICE READY")
	}

	override fun onTaskRemoved(rootIntent: Intent?) {
		Log.d(TAG, "TASK REMOVED CALLED")
		pauseAllPlayersAndStopSelf()
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
		mediaSession.apply { setListener(listener) }

	private fun cleanUp() {
		Log.d(TAG, "CLEAN UP IN MEDIA SESSION")

		mediaSession.apply {
			player.stop()
			player.clearMediaItems()
			// release the player
			player.release()
			// release the session
			release()
		}
		clearListener()
	}

	override fun onDestroy() {
		cleanUp()
		Log.d(TAG, "PLAYER SERVICE DESTROYED")
		super.onDestroy()
	}
}