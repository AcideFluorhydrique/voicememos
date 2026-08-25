package com.eva.utils

object NavDeepLinks {
	// a fixed host, so the links keep working whatever the application id or the
	// build type suffix happens to be
	private const val BASE_URI = "app://voicememos"

	const val RECORDER_DESTINATION_PATTERN = BASE_URI
	const val RECORDING_DESTINATION_PATTERN = "$BASE_URI/recordings"

	const val PLAYER_DESTINATION_PATTERN = "$BASE_URI/player/{audioId}"


	fun audioPlayerDestinationUri(id: Long) = (BASE_URI + "/player" + "/${id}")
}