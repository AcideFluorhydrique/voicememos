package com.eva.feature_widget.shortcut

import androidx.annotation.StringRes
import com.eva.feature_widget.R
import com.eva.utils.IntentConstants

/**
 * What automation apps (Key Mapper, Tasker, MacroDroid...) can ask the recorder to do through
 * the "Launch app shortcut" action. [id] is what travels inside the shortcut, so it must stay
 * the same once released, the label can be reworded freely.
 */
internal enum class ShortcutAction(
	val id: String,
	@StringRes val label: Int,
	@StringRes val description: Int,
) {
	START("start", R.string.shortcut_action_start, R.string.shortcut_action_start_description),
	PAUSE("pause", R.string.shortcut_action_pause, R.string.shortcut_action_pause_description),
	RESUME("resume", R.string.shortcut_action_resume, R.string.shortcut_action_resume_description),
	STOP("stop", R.string.shortcut_action_stop, R.string.shortcut_action_stop_description),
	TOGGLE_RECORD_PAUSE(
		"toggle_record_pause",
		R.string.shortcut_action_toggle_record_pause,
		R.string.shortcut_action_toggle_record_pause_description
	),
	TOGGLE_RECORD_STOP(
		"toggle_record_stop",
		R.string.shortcut_action_toggle_record_stop,
		R.string.shortcut_action_toggle_record_stop_description
	);

	companion object {
		fun fromId(id: String?): ShortcutAction? = entries.firstOrNull { it.id == id }
	}
}

internal object ShortcutContract {

	/** Action of the intent a created shortcut launches, its target is always the handler */
	const val ACTION_RUN_SHORTCUT = "${IntentConstants.APPLICATION_NAME}.action.RUN_SHORTCUT"

	/** The [ShortcutAction.id] to perform */
	const val EXTRA_ACTION = "${IntentConstants.APPLICATION_NAME}.extra_shortcut_action"

	/** Proof that the shortcut was created in the picker, see [ShortcutToken] */
	const val EXTRA_TOKEN = "${IntentConstants.APPLICATION_NAME}.extra_shortcut_token"
}
