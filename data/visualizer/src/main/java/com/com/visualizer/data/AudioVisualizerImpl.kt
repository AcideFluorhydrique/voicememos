package com.com.visualizer.data

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.ThreadController
import com.com.visualizer.domain.VisualizerState
import com.com.visualizer.domain.exception.DecoderExistsException
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "PLAIN_VISUALIZER"

@OptIn(UnstableApi::class)
internal class AudioVisualizerImpl(
	private val extractor: ExtractorsFactory,
	private val dataSource: DataSource.Factory,
	private val threadHandler: ThreadController
) : AudioVisualizer {

	constructor(context: Context, threadHandler: ThreadController) : this(
		extractor = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true),
		dataSource = DefaultDataSource.Factory(context),
		threadHandler = threadHandler
	)

	private var _decoder: MediaCodecPCMDataDecoder? = null
	private val _lock = Mutex()

	private val _isReady = MutableStateFlow(VisualizerState.NOT_STARTED)
	private val _visualization = MutableStateFlow(floatArrayOf())

	override val visualizerState: StateFlow<VisualizerState>
		get() = _isReady

	override val normalizedVisualization: Flow<FloatArray>
		get() = _visualization.map { array -> array.normalize().smoothen(.4f) }
			.flowOn(Dispatchers.Default)
			.catch { err -> Log.d(TAG, "SOME ERROR", err) }


	override suspend fun prepareVisualization(
		model: AudioFileModel,
		lifecycleOwner: LifecycleOwner,
		timePerPointInMs: Int
	): Result<Unit> = prepareVisualization(
		fileUri = model.fileUri,
		lifecycleOwner = lifecycleOwner,
		timePerPointInMs
	)

	override suspend fun prepareVisualization(
		fileUri: String,
		lifecycleOwner: LifecycleOwner,
		timePerPointInMs: Int
	): Result<Unit> = _lock.withLock {
		if (_decoder != null) {
			Log.d(TAG, "CLEAN DECODER TO PREPARE IT AGAIN")
			return Result.failure(DecoderExistsException())
		}

		val handler = threadHandler.bindToLifecycle(lifecycleOwner)

		return try {
			val decoder = MediaCodecPCMDataDecoder(
				handler = handler,
				seekDurationMillis = timePerPointInMs,
			).also { _decoder = it }

			Log.i(TAG, "MEDIA CODEC DECODER CREATED")

			// setup callbacks
			decoder.setOnBufferDecode(::updateVisuals)
			decoder.setOnComplete {
				Log.d(TAG, "DECODER JOB IS DONE RELEASING THE HANDLER")
				// release the objects
				releaseDecoder()
				// decoder work is done so we can kill it now
				if (handler != null) threadHandler.stopThread(handler)
			}
			decoder.initiateExtraction(extractor, dataSource, fileUri.toUri())
		} catch (e: Exception) {
			Log.e(TAG, "CANNOT DECODE THIS URI", e)
			Result.failure(e)
		}
	}

	private fun updateVisuals(array: FloatArray) {
		_isReady.update { VisualizerState.RUNNING }
		_visualization.update { it + array }
	}

	private fun releaseDecoder() {
		try {
			_decoder?.cleanUp()
		} finally {
			Log.d(TAG, "DECODER CLEANED!")
			_decoder = null
			_isReady.update { VisualizerState.FINISHED }
		}
	}

	override fun cleanUp() {
		Log.d(TAG, "CLEARING UP THE VISUALIZER")
		// release the objects
		releaseDecoder()
		// reset values
		_visualization.update { floatArrayOf() }
	}

}