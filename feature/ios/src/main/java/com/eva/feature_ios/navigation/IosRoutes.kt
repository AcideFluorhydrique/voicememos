package com.eva.feature_ios.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Destinations of the iOS styled app, the memos list doubles as the player so there is no
 * separate player route, only the trim editor and the two secondary screens.
 */
sealed interface IosRoutes {

	/**
	 * @param audioId set by the deep link that opens the last played recording, the list
	 * expands that recording when it is a real id
	 */
	@Serializable
	data class VoiceMemos(@SerialName("audioId") val audioId: Long = NO_AUDIO_ID) : IosRoutes

	@Serializable
	data class TrimEditor(@SerialName("audioId") val audioId: Long) : IosRoutes

	@Serializable
	data object RecentlyDeleted : IosRoutes

	@Serializable
	data object MemoSettings : IosRoutes

	companion object {
		const val NO_AUDIO_ID = -1L
	}
}
