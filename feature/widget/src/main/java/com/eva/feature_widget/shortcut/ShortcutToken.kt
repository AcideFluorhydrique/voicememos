package com.eva.feature_widget.shortcut

import android.content.Context
import androidx.core.content.edit
import java.security.MessageDigest
import java.util.UUID

/**
 * Secret that ties a shortcut to this installation. [ShortcutHandlerActivity] is exported, so
 * any app could fire it, only the shortcuts made in [ShortcutConfigActivity] carry the token.
 * Automation apps store and replay the whole shortcut intent, so they keep working, other apps
 * cannot read the app's private preferences to learn it.
 *
 * Clearing the app's data makes a new token, shortcuts made before that have to be created
 * again in the picker.
 */
internal object ShortcutToken {

	private const val PREFERENCES = "shortcut_token"
	private const val KEY_TOKEN = "token"

	/** The token, made on first use */
	@Synchronized
	fun getOrCreate(context: Context): String {
		val preferences = context.applicationContext
			.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

		preferences.getString(KEY_TOKEN, null)?.let { return it }

		val token = UUID.randomUUID().toString()
		preferences.edit(commit = true) { putString(KEY_TOKEN, token) }
		return token
	}

	/** True only for the stored token, a token never made counts as no match */
	@Synchronized
	fun isValid(context: Context, candidate: String?): Boolean {
		if (candidate.isNullOrEmpty()) return false

		val stored = context.applicationContext
			.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
			.getString(KEY_TOKEN, null) ?: return false

		return MessageDigest.isEqual(stored.toByteArray(), candidate.toByteArray())
	}
}
