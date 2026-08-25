package com.eva.feature_ios.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter
	get() = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

private val TIME_FORMAT: DateTimeFormatter
	get() = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

private val FULL_FORMAT: DateTimeFormatter
	get() = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

/**
 * Voice memos labels recordings relative to today, everything older falls back to a date.
 */
fun LocalDateTime.asMemoDateLabel(todayLabel: String, yesterdayLabel: String): String {
	val dateTime = toJavaLocalDateTime()
	val today = java.time.LocalDate.now()

	return when (dateTime.toLocalDate()) {
		today -> "$todayLabel, ${dateTime.format(TIME_FORMAT)}"
		today.minusDays(1) -> "$yesterdayLabel, ${dateTime.format(TIME_FORMAT)}"
		else -> dateTime.format(DATE_FORMAT)
	}
}

/**The long form used on the detail rows*/
fun LocalDateTime.asFullDateLabel(): String = toJavaLocalDateTime().format(FULL_FORMAT)
