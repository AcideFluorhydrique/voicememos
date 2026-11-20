package com.eva.recordings.data.wrapper

import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel

private const val TAG = "INTENT_MEDIA_REQUESTER"

object RecordingsMediaRequester {

	/**
	 * Android 10 has no write or trash request, changing a recording another install made
	 * throws a [RecoverableSecurityException] carrying the system's own approval dialog.
	 * Opening the file for reading and writing, without truncating it, finds out whether this
	 * install may change it already, and hands that dialog over when it may not.
	 *
	 * @return the approval to ask for, null when the recording can be changed as it is or on
	 * any other version, which have requests of their own
	 */
	fun createLegacyWriteRequest(context: Context, recording: RecordedVoiceModel)
			: IntentSenderRequest? {
		if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) return null
		return try {
			context.contentResolver.openFileDescriptor(recording.fileUri.toUri(), "rw")?.close()
			null
		} catch (e: RecoverableSecurityException) {
			Log.d(TAG, "WRITE ACCESS NEEDS THE USER TO APPROVE")
			IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
		} catch (e: Exception) {
			Log.e(TAG, "CANNOT CHECK WRITE ACCESS", e)
			null
		}
	}

	@RequiresApi(Build.VERSION_CODES.R)
	fun createTrashRequest(context: Context, models: Collection<RecordedVoiceModel>)
			: IntentSenderRequest? {

		val uris = models.filterNot { it.owner == context.packageName }
			.map(RecordedVoiceModel::fileUri)
			.map(String::toUri)

		if (uris.isEmpty()) {
			Log.d(TAG, "NO URIS CANNOT BE DELETED")
			return null
		}

		Log.d(TAG, "NO. OF URI TO TRASH ${uris.size}")
		val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, true)

		return IntentSenderRequest.Builder(pendingIntent)
			.build()
	}

	/**
	 * Asks to take recordings this install does not own back out of the trash, the system
	 * restores them itself once the user agrees
	 */
	@RequiresApi(Build.VERSION_CODES.R)
	fun createRestoreRequest(context: Context, models: Collection<TrashRecordingModel>)
			: IntentSenderRequest? {

		val uris = models.filterNot { it.owner == context.packageName }
			.map(TrashRecordingModel::fileUri)
			.map(String::toUri)

		if (uris.isEmpty()) return null

		val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, false)

		return IntentSenderRequest.Builder(pendingIntent).build()
	}

	@JvmName("create_delete_requests_from_trash_models")
	@RequiresApi(Build.VERSION_CODES.R)
	fun createDeleteRequest(context: Context, models: Collection<TrashRecordingModel>)
			: IntentSenderRequest? {

		val uris = models.filterNot { it.owner == context.packageName }
			.map(TrashRecordingModel::fileUri)
			.map(String::toUri)

		if (uris.isEmpty()) {
			Log.d(TAG, "NO URIS CANNOT BE DELETED")
			return null
		}

		val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)

		return IntentSenderRequest.Builder(pendingIntent).build()
	}

	@RequiresApi(Build.VERSION_CODES.R)
	fun createWriteRequest(context: Context, recording: RecordedVoiceModel)
			: IntentSenderRequest {
		val uris = recording.fileUri.toUri()

		val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(uris))

		return IntentSenderRequest.Builder(pendingIntent).build()
	}

	@JvmName("create_delete_requests_from_recorded_models")
	@RequiresApi(Build.VERSION_CODES.R)
	fun createDeleteRequest(context: Context, models: List<RecordedVoiceModel>)
			: IntentSenderRequest? {

		val uris = models.map(RecordedVoiceModel::fileUri).map(String::toUri)

		if (uris.isEmpty()) {
			Log.d(TAG, "NO URIS PROVIDED")
			return null
		}

		val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)

		return IntentSenderRequest.Builder(pendingIntent).build()
	}
}