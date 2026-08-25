package com.eva.cupertino.util

import kotlin.time.Duration

/**
 * Formats a duration the way voice memos does it, `1:23` for short clips and `1:02:03`
 * once the recording passes an hour.
 */
fun Duration.asClockString(): String {
	val totalSeconds = inWholeSeconds.coerceAtLeast(0L)
	val hours = totalSeconds / 3600
	val minutes = (totalSeconds % 3600) / 60
	val seconds = totalSeconds % 60

	return if (hours > 0) {
		"%d:%02d:%02d".format(hours, minutes, seconds)
	} else "%d:%02d".format(minutes, seconds)
}

/**The recorder timer, `00:12,34` with hundredths just like the iOS recorder*/
fun Duration.asRecorderTimerString(): String {
	val totalMillis = inWholeMilliseconds.coerceAtLeast(0L)
	val hours = totalMillis / 3_600_000
	val minutes = (totalMillis % 3_600_000) / 60_000
	val seconds = (totalMillis % 60_000) / 1000
	val hundredths = (totalMillis % 1000) / 10

	return if (hours > 0) {
		"%d:%02d:%02d,%02d".format(hours, minutes, seconds, hundredths)
	} else "%02d:%02d,%02d".format(minutes, seconds, hundredths)
}

/**Used for the remaining time, iOS shows it with a leading minus*/
fun Duration.asRemainingString(): String = "-" + asClockString()
