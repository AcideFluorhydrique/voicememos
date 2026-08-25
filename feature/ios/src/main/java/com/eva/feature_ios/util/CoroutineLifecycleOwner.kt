package com.eva.feature_ios.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * The audio visualizer decodes on a background thread bound to a lifecycle, a view model
 * has no lifecycle of its own so its scope is turned into one.
 */
internal class CoroutineLifecycleOwner(context: CoroutineContext) : LifecycleOwner {

	private val lifecycleRegistry = LifecycleRegistry(this)
		.apply { currentState = Lifecycle.State.INITIALIZED }

	override val lifecycle: Lifecycle
		get() = lifecycleRegistry

	init {
		val job = context[Job]
		if (job?.isActive == true) {
			lifecycleRegistry.currentState = Lifecycle.State.RESUMED
			job.invokeOnCompletion {
				lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
			}
		} else {
			lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
		}
	}
}
