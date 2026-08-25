package com.eva.player.data.player

import com.eva.player.domain.AudioFilePlayer

internal sealed interface MediaControllerState {
	data object Disconnected : MediaControllerState
	data object Connecting : MediaControllerState
	data class Connected(val player: AudioFilePlayer) : MediaControllerState
}