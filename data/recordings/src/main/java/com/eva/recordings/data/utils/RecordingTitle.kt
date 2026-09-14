package com.eva.recordings.data.utils

import android.webkit.MimeTypeMap

private val AUDIO_EXTENSIONS = setOf(
	"m4a", "mp4", "aac", "ogg", "oga", "opus", "amr", "awb",
	"3gp", "3gpp", "mp3", "wav", "flac", "weba", "webm", "mka",
)

// media store prefixes the file of a trashed or pending item with its expiry
private val MEDIA_STORE_STATE_PREFIX = Regex("""^\.(trashed|pending)-\d+-""")

/**
 * The name a recording is shown with, taken from its display name.
 *
 * The title column cannot be used for this, media store derives it on its own and
 * silently ignores an app writing to it, so a rename only ever reaches the display name.
 * The extension is removed only when it really is one, a name like `take 1.2` keeps its
 * dot even on a file an earlier rename left without an extension.
 *
 * @param storedTitle media store's own title, used when there is no display name
 */
internal fun recordingTitleOf(displayName: String?, mimeType: String?, storedTitle: String?): String {
	val fallback = storedTitle.orEmpty()
	if (displayName.isNullOrBlank()) return fallback

	val name = displayName.replaceFirst(MEDIA_STORE_STATE_PREFIX, "")
	val suffix = name.substringAfterLast('.', "")
	if (suffix.isEmpty()) return name

	val mimeExtension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
	val isExtension = suffix.equals(mimeExtension, ignoreCase = true) ||
			suffix.lowercase() in AUDIO_EXTENSIONS

	if (!isExtension) return name
	return name.dropLast(suffix.length + 1).ifBlank { name }
}
