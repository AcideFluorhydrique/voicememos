package com.eva.recordings.data.wrapper

import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

internal object RecordingsConstants {

	val AUDIO_VOLUME_URI: Uri
		get() = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

	/**
	 * Every folder this app has ever recorded into, as LIKE patterns over the relative path.
	 * Media store forgets who made a file once that install is removed, the folder is the
	 * only thing left that says a recording is ours.
	 */
	val RECORDINGS_FOLDER_PATTERNS: List<String>
		get() = listOf(Environment.DIRECTORY_RECORDINGS, Environment.DIRECTORY_MUSIC)
			.map { directory -> directory + File.separator + "RecorderApp" + File.separator + "%" }

	// DON'T CHANGE
	val RECORDINGS_MUSIC_PATH: String
		get() {
			// keep the recordings in recordings directory on API 31
			// otherwise music directory
			val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				Environment.DIRECTORY_RECORDINGS
			else Environment.DIRECTORY_MUSIC

			return directory + File.separator + "RecorderApp"
		}
}