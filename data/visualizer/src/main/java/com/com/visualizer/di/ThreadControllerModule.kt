package com.com.visualizer.di

import com.com.visualizer.data.ThreadLifecycleControllerImpl
import com.com.visualizer.domain.ThreadController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object ThreadControllerModule {

	@Provides
	@ActivityRetainedScoped
	fun providesThread(): ThreadController = ThreadLifecycleControllerImpl("ComputeThread")
}