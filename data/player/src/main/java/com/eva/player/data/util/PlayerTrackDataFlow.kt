package com.eva.player.data.util

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.eva.player.domain.model.PlayerTrackData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "PLAYER_TRACK_DATA_FLOW"

fun Player.computePlayerTrackData(
	delayDuration: Duration = 100.milliseconds
): Flow<PlayerTrackData> = callbackFlow {

	// first emission
	launch {
		val trackData = this@computePlayerTrackData.toTrackData()
		Log.d(TAG, "INITIAL DATA :$trackData")
		// send only if there is some duration info available
		// if invalid will be filter out later
		send(trackData)
	}

	var job: Job? = null
	fun startPeriodicUpdates() {
		job?.cancel()
		job = launch {
			try {
				while (isActive) {
					val trackData = toTrackData()
					if (trackData.allPositiveAndFinite) trySend(trackData)
					delay(delayDuration)
				}
			} catch (_: CancellationException) {
				Log.d(TAG, "ADVERTISE COROUTINE CANCELLED")
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	val listener = object : Player.Listener {

		override fun onPlaybackStateChanged(playbackState: Int) {
			if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
				launch {
					val trackData = this@computePlayerTrackData.toTrackData()
					send(trackData)
				}
			}
		}

		override fun onPositionDiscontinuity(
			oldPosition: Player.PositionInfo,
			newPosition: Player.PositionInfo,
			reason: Int
		) {
			if (reason != Player.DISCONTINUITY_REASON_SEEK) return
			// on seek update the current position to the final seek position
			launch {
				val newPosDuration = newPosition.positionMs.milliseconds
				val trackData = this@computePlayerTrackData.toTrackData()
					.copy(current = newPosDuration)
				send(trackData)
			}
		}

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			if (isPlaying) {
				Log.d(TAG, "STARTING PERIODIC UPDATES")
				startPeriodicUpdates()
			} else {
				Log.d(TAG, "PERIODIC UPDATES STOPPED")
				// cancel the old coroutine
				// and emit current track info
				job?.cancel()
				job = null
				launch {
					val track = this@computePlayerTrackData.toTrackData()
					send(track)
				}
			}
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			if (reason in arrayOf(
					Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
					Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
				)
			) {
				launch {
					val trackData = this@computePlayerTrackData.toTrackData()
					send(trackData)
				}
			}
			if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
				val itemDuration = mediaItem?.mediaMetadata?.durationMs?.milliseconds ?: return
				Log.d(TAG, "MEDIA ITEM TRANSITION NEW DURATION :$itemDuration")
				launch {
					val trackData = PlayerTrackData(0.seconds, itemDuration)
					send(trackData)
				}
			}
		}

		override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
			super.onMediaMetadataChanged(mediaMetadata)
			val itemDuration = mediaMetadata.durationMs?.milliseconds ?: return
			Log.d(TAG, "MEDIA ITEM METADATA CHANGED NEW DURATION :$itemDuration")
		}

		override fun onTimelineChanged(timeline: Timeline, reason: Int) {
			if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
				Log.d(TAG, "TIMELINE OF THE MEDIA CHANGED")
				launch(Dispatchers.Main) {
					val window = Timeline.Window()
					timeline.getWindow(0, window)
					val duration = window.durationMs.takeIf { it != C.TIME_UNSET } ?: return@launch
					val trackData = PlayerTrackData(0.seconds, duration.milliseconds)
					send(trackData)
				}
			}
		}
	}

	// add the listener
	Log.d(TAG, "LISTENER ADDED")
	addListener(listener)

	// if this already playing start periodic updates
	if (isPlaying) {
		Log.d(TAG, "PLAYER IS ALREADY PLAYING SO PERIODIC UPDATES")
		startPeriodicUpdates()
	}

	//remove the listener
	awaitClose {
		job?.cancel()
		Log.d(TAG, "REMOVING LISTENER")
		removeListener(listener)
	}
}
	.filter { it.allPositiveAndFinite }
	.distinctUntilChanged { old, new -> old.current == new.current }


