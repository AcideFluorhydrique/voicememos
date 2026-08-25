package com.eva.recordings.domain.provider

import com.eva.recordings.domain.models.AudioFileModel
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

typealias ResourcedDetailedRecordingModel = Resource<AudioFileModel, Exception>

interface PlayerFileProvider {

	suspend fun providesAudioFileUri(audioId: Long): Result<String>

	fun getAudioFileFromIdFlow(
		id: Long,
		readMetaData: Boolean = true
	): Flow<ResourcedDetailedRecordingModel>

	suspend fun getAudioFileFromId(
		id: Long,
		readMetaData: Boolean = false
	): Result<AudioFileModel>
}