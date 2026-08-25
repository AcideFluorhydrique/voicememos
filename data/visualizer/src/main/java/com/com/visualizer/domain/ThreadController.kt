package com.com.visualizer.domain

import android.os.Handler
import androidx.lifecycle.LifecycleOwner

/**
 * An interface for controlling the lifecycle of a background thread, typically
 * managing a [Handler] that posts tasks to that thread.
 */
interface ThreadController {

	/**
	 * Binds the controlled thread's execution to the lifecycle of the given [lifecycleOwner].
	 *
	 * @param lifecycleOwner The [LifecycleOwner] whose lifecycle determines the thread's lifespan.
	 * @return The [Handler] associated with the newly started background thread,
	 * or `null` if the thread could not be started.
	 */
	fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler?

	/**
	 * Stops the background thread associated with the given [handler].
	 *
	 * @param handler The [Handler] associated with the background thread to be stopped.
	 * This is typically the object returned by [bindToLifecycle].
	 * @param maxWaitTime The maximum time (in milliseconds) to wait for the thread
	 * to complete its current tasks and stop before interrupting it.
	 */
	fun stopThread(handler: Handler?, maxWaitTime: Long = 600L)
}