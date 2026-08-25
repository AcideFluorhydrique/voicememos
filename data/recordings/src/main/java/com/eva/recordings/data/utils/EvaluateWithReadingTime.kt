package com.eva.recordings.data.utils

import android.util.Log
import kotlin.time.measureTimedValue

internal inline fun <T> evaluateWithTimeRead(
	loggingTag: String = "EVALUATION_TIME",
	readTime: Boolean = true,
	caller: () -> T
): T {
	return if (readTime) {
		val (result, duration) = measureTimedValue(block = caller)
		Log.d(loggingTag, "EVALUATION TOOK :$duration $result")
		result
	} else caller()
}