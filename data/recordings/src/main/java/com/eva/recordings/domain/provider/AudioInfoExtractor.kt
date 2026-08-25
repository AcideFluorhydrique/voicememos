package com.eva.recordings.domain.provider

import com.eva.recordings.domain.models.MediaMetaDataInfo

fun interface AudioInfoExtractor {

	suspend fun extractMediaData(uri: String): MediaMetaDataInfo?
}