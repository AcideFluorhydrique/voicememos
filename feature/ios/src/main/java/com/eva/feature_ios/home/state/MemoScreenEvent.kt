package com.eva.feature_ios.home.state

import com.eva.player.domain.model.PlayerPlayBackSpeed
import com.eva.recordings.domain.models.RecordedVoiceModel
import kotlin.time.Duration

internal sealed interface MemoScreenEvent {

	data object LoadRecordings : MemoScreenEvent

	data class OnSearchQueryChange(val query: String) : MemoScreenEvent

	/**Tapping a row expands it and loads the player, tapping it again collapses it*/
	data class OnMemoSelected(val memoId: Long) : MemoScreenEvent

	data object OnCollapseMemo : MemoScreenEvent

	data object OnTogglePlayPause : MemoScreenEvent

	data class OnSeek(val ratio: Float) : MemoScreenEvent

	data object OnSeekFinished : MemoScreenEvent

	data class OnSkip(val amount: Duration, val rewind: Boolean = false) : MemoScreenEvent

	data class OnSpeedChange(val speed: PlayerPlayBackSpeed) : MemoScreenEvent

	data class OnRename(val memo: RecordedVoiceModel, val newName: String) : MemoScreenEvent

	data class OnDelete(val memo: RecordedVoiceModel) : MemoScreenEvent

	/**The system moved the recording to the trash itself after the user approved it*/
	data class OnTrashedBySystem(val memo: RecordedVoiceModel) : MemoScreenEvent

	data class OnShare(val memo: RecordedVoiceModel) : MemoScreenEvent

	data class OnToggleFavourite(val memo: RecordedVoiceModel) : MemoScreenEvent
}
