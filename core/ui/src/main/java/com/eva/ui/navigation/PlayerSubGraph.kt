package com.eva.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PlayerSubGraph {

	// we need audio id to mark to get the route data from saved state handle
	@Serializable
	data class NavGraph(@SerialName("audioId") val audioId: Long) : PlayerSubGraph

	// we need the audio id to let deep links work
	@Serializable
	data class AudioPlayerRoute(@SerialName("audioId") val audioId: Long) : PlayerSubGraph

	// we are using the audio id to set up the player again so audio id
	@Serializable
	data class AudioEditorRoute(@SerialName("audioId") val audioId: Long) : PlayerSubGraph
}