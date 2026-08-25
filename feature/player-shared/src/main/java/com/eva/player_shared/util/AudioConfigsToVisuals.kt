package com.eva.player_shared.util

import android.util.Log
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.editor.domain.model.AudioEditAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min

private const val TAG = "PLAYER_CONFIG_SETTER"

internal suspend fun FloatArray.updateArrayViaConfigs(
	configs: AudioConfigToActionList,
	timeInMillisPerBar: Int = 100,
	dispatcher: CoroutineContext = Dispatchers.Default
): FloatArray {
	require(timeInMillisPerBar > 0) { "timeInMillisPerBar must be positive" }
	return withContext(dispatcher) {
		if (configs.isEmpty()) return@withContext this@updateArrayViaConfigs
		var modifiedArray = copyOf()
		Log.d(TAG, "INITIAL SIZE :${size}")

		Log.d(TAG, "=========================================================")
		configs.forEachIndexed { iteration, (config, action) ->
			val startIndex = (config.start.inWholeMilliseconds / timeInMillisPerBar).toInt()
			val endIndex = (config.end.inWholeMilliseconds / timeInMillisPerBar).toInt()

			val validStart = max(0, min(startIndex, modifiedArray.size))
			val validEnd = max(0, min(endIndex, modifiedArray.size))

			if (validStart >= validEnd) {
				val error = "INVALID RANGE: [$validStart:$validEnd] ACT:$action I_TER:$iteration"
				Log.e(TAG, error)
				throw IllegalStateException(error)
			}

			Log.d(TAG, "ITERATION:$iteration")

			when (action) {
				AudioEditAction.CUT -> {
					Log.d(TAG, "ACTION:CUT [START :${validStart} END:$validEnd]")
					val before = modifiedArray.copyOfRange(0, validStart)
					val after = modifiedArray.copyOfRange(validEnd, modifiedArray.size)
					modifiedArray = before + after
				}

				AudioEditAction.CROP -> {
					Log.d(
						TAG,
						"ACTION:CROP [START :0 END:$validStart] ---xx--  [START:$validEnd END: ${modifiedArray.size}]"
					)
					modifiedArray = modifiedArray.copyOfRange(validStart, validEnd)
				}
			}
		}
		Log.d(TAG, "=========================================================")
		Log.d(TAG, "FINAL ARRAY SIZE AFTER PROCESSING: ${modifiedArray.size}")
		modifiedArray
	}
}