package com.eva.feature_ios.home.state

import androidx.compose.runtime.Immutable
import com.eva.player.domain.model.PlayerPlayBackSpeed
import kotlin.time.Duration

/**Everything the expanded player card needs to draw itself*/
@Immutable
data class MemoPlaybackState(
	val current: Duration = Duration.ZERO,
	val total: Duration = Duration.ZERO,
	val isPlaying: Boolean = false,
	val speed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.Normal,
) {
	val progress: Float
		get() {
			if (total.inWholeMilliseconds <= 0L) return 0f
			val ratio = current.inWholeMilliseconds.toFloat() / total.inWholeMilliseconds.toFloat()
			return ratio.coerceIn(0f, 1f)
		}

	val remaining: Duration
		get() = (total - current).coerceAtLeast(Duration.ZERO)
}
