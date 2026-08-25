package com.eva.feature_editor.viewmodel

import dagger.assisted.AssistedFactory

@AssistedFactory
internal interface EditorViewmodelFactory {

	fun create(audioId: Long): AudioEditorViewModel
}