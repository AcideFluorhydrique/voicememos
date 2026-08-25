package com.eva.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlin.reflect.KType

inline fun <reified T : Any> NavGraphBuilder.animatedComposable(
	typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
	deepLinks: List<NavDeepLink> = emptyList(),
	noinline sizeTransform: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards SizeTransform?)? = {
		SizeTransform(clip = false) { _, _ -> spring(stiffness = Spring.StiffnessMediumLow) }
	},
	noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T>(
	typeMap = typeMap,
	deepLinks = deepLinks,
	enterTransition = { slideInFromRightAndFadeIn },
	exitTransition = { slideOutToLeftAndFadeOut },
	popEnterTransition = { slideInFromLeftAndFadeIn },
	popExitTransition = { slideOutToRightAndFadeOut },
	sizeTransform = sizeTransform,
	content = content
)

val AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromRightAndFadeIn: EnterTransition
	get() = slideIntoContainer(
		AnimatedContentTransitionScope.SlideDirection.Up,
		animationSpec = tween(durationMillis = 300, easing = LinearEasing)
	) + fadeIn(animationSpec = tween(easing = LinearOutSlowInEasing, durationMillis = 300))

val AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToLeftAndFadeOut: ExitTransition
	get() = slideOutOfContainer(
		AnimatedContentTransitionScope.SlideDirection.Up,
		animationSpec = tween(durationMillis = 300, easing = LinearEasing)
	) + fadeOut(animationSpec = tween(easing = FastOutLinearInEasing, durationMillis = 300))

val AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromLeftAndFadeIn: EnterTransition
	get() = slideIntoContainer(
		AnimatedContentTransitionScope.SlideDirection.Down,
		animationSpec = tween(durationMillis = 300, easing = LinearEasing)
	) + fadeIn(animationSpec = tween(easing = LinearOutSlowInEasing, durationMillis = 300))

val AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToRightAndFadeOut: ExitTransition
	get() = slideOutOfContainer(
		AnimatedContentTransitionScope.SlideDirection.Down,
		animationSpec = tween(durationMillis = 300, easing = LinearEasing)
	) + fadeOut(animationSpec = tween(easing = FastOutLinearInEasing, durationMillis = 300))
