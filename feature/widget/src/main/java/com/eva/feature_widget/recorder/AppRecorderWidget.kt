package com.eva.feature_widget.recorder

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import com.eva.feature_widget.R
import com.eva.feature_widget.proto.RecorderWidgetDataProto
import com.eva.feature_widget.recorder.composable.RecorderWidgetActions
import com.eva.feature_widget.recorder.composable.RecorderWidgetContent
import com.eva.feature_widget.recorder.models.toModel
import com.eva.feature_widget.recorder.repository.RecorderWidgetRepoImpl.Companion.FILE_LOCATION
import com.eva.feature_widget.recorder.repository.RecorderWidgetRepoImpl.Companion.recorderWidgetData
import com.eva.feature_widget.utils.RecorderAppWidgetTheme
import com.eva.recorder.domain.models.RecorderAction
import com.eva.utils.IntentConstants
import com.eva.utils.NavDeepLinks
import java.io.File


class AppRecorderWidget : GlanceAppWidget() {

	override val sizeMode: SizeMode
		get() = SizeMode.Exact

	override val stateDefinition: GlanceStateDefinition<RecorderWidgetDataProto>
		get() = object : GlanceStateDefinition<RecorderWidgetDataProto> {

			override suspend fun getDataStore(context: Context, fileKey: String)
					: DataStore<RecorderWidgetDataProto> = context.recorderWidgetData

			override fun getLocation(context: Context, fileKey: String)
					: File = context.dataStoreFile(FILE_LOCATION)

		}


	override suspend fun provideGlance(context: Context, id: GlanceId) {
		provideContent {

			val protoState = currentState<RecorderWidgetDataProto>()

			val openApp = actionStartActivity(
				Intent().apply {
					setClassName(context.applicationContext, IntentConstants.MAIN_ACTIVITY)
					action = Intent.ACTION_VIEW
					data = NavDeepLinks.RECORDER_DESTINATION_PATTERN.toUri()
					flags = Intent.FLAG_ACTIVITY_NEW_TASK
				}
			)

			// recording straight from the home screen needs the microphone permission to be
			// there already, a widget has no way to ask for it, so without it the button
			// opens the app where the question can be put
			val canRecord = ContextCompat.checkSelfPermission(
				context,
				Manifest.permission.RECORD_AUDIO
			) == PackageManager.PERMISSION_GRANTED

			val actions = RecorderWidgetActions(
				record = if (canRecord) context.recorderAction(
					action = RecorderAction.StartRecorderAction,
					asForegroundService = true,
				) else openApp,
				pause = context.recorderAction(RecorderAction.PauseRecorderAction),
				resume = context.recorderAction(RecorderAction.ResumeRecorderAction),
				stop = context.recorderAction(RecorderAction.StopRecorderAction),
				cancel = context.recorderAction(RecorderAction.CancelRecorderAction),
			)

			RecorderAppWidgetTheme {
				RecorderWidgetContent(
					model = protoState.toModel(),
					actions = actions,
					// anywhere outside the buttons still opens the app
					modifier = GlanceModifier.clickable(onClick = openApp),
				)
			}
		}
	}

	/**
	 * Sends a recorder action to the recording service. Only the start has to ask for a
	 * foreground service, nothing of the app is running at that point and a plain service
	 * start from the background is refused. The others reach a service that is already in
	 * the foreground, the same way the buttons of the recording notification do.
	 */
	private fun Context.recorderAction(
		action: RecorderAction,
		asForegroundService: Boolean = false,
	): Action = actionStartService(
		intent = Intent().apply {
			setClassName(applicationContext, IntentConstants.RECORDER_SERVICE)
			this.action = action.action
		},
		isForegroundService = asForegroundService,
	)

	override fun onCompositionError(
		context: Context,
		glanceId: GlanceId,
		appWidgetId: Int,
		throwable: Throwable,
	) {
		// print stacktrace
		throwable.printStackTrace()
		// update the layout
		val widgetManager = context.getSystemService<AppWidgetManager>() ?: return

		val remoteView = RemoteViews(context.packageName, R.layout.widget_loading_failed_layout)
			.apply {
				val message = throwable.message ?: context.getString(R.string.widget_error_text)
				setTextViewText(R.id.widget_error_description, message)
			}

		// show the error on the widget
		widgetManager.updateAppWidget(appWidgetId, remoteView)
	}

}

/**
 * Brings the recorder widgets up to date with the microphone permission, called when the
 * app leaves the screen, which is right after the permission can have been granted.
 *
 * Asking for an update is not enough on its own. Glance only draws a widget again when
 * its state has changed, and a permission is not part of any state, so the widget kept
 * opening the app after the permission was granted until something else, a resize for
 * one, forced it to draw. The permission is written into the state first.
 */
suspend fun refreshRecorderWidgets(context: Context) {
	try {
		val appContext = context.applicationContext

		val canRecord = ContextCompat.checkSelfPermission(
			appContext,
			Manifest.permission.RECORD_AUDIO
		) == PackageManager.PERMISSION_GRANTED

		appContext.recorderWidgetData.updateData { content ->
			if (content.canRecord == canRecord) content
			else content.toBuilder().setCanRecord(canRecord).build()
		}
		AppRecorderWidget().updateAll(appContext)
	} catch (e: Exception) {
		e.printStackTrace()
	}
}
