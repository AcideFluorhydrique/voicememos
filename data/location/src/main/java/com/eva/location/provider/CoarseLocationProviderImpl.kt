package com.eva.location.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.eva.location.domain.BaseLocationModel
import com.eva.location.domain.exceptions.CannotFoundLastLocationException
import com.eva.location.domain.exceptions.CurrentLocationTimeoutException
import com.eva.location.domain.exceptions.LocationNotEnabledException
import com.eva.location.domain.exceptions.LocationPermissionNotFoundException
import com.eva.location.domain.exceptions.LocationProviderNotFoundException
import com.eva.location.domain.repository.LocationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume

private const val CURRENT_LOCATION_TIMEOUT = 2_000L
private const val LAST_LOCATION_MAX_AGE_NANOS = 10_000L * 1_000_000L

internal class CoarseLocationProviderImpl(private val context: Context) : LocationProvider {

	private val locationManager by lazy { context.getSystemService<LocationManager>() }

	private val _hasLocationPermission: Boolean
		get() = ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.ACCESS_COARSE_LOCATION
		) == PermissionChecker.PERMISSION_GRANTED

	override val isLocationEnabled: Boolean
		get() = locationManager?.isLocationEnabled == true

	/**
	 * The providers this device can actually serve a coarse fix from, cheapest first. The platform
	 * fused provider only exists from android 12, and roms without google play services frequently
	 * ship no network provider at all, so a single usable provider is enough.
	 */
	private val availableProviders: List<String>
		get() {
			val manager = locationManager ?: return emptyList()
			val candidates = buildList {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
					add(LocationManager.FUSED_PROVIDER)
				add(LocationManager.NETWORK_PROVIDER)
				add(LocationManager.GPS_PROVIDER)
			}
			// a provider the rom does not ship can throw instead of reporting itself disabled
			return candidates.filter { provider ->
				runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
			}
		}

	@SuppressLint("MissingPermission")
	private fun getLastKnownLocation(): Result<BaseLocationModel> {
		val manager = locationManager ?: return Result.failure(LocationProviderNotFoundException())
		// age is measured against the monotonic clock, the wall clock can jump under us
		val oldestAccepted = SystemClock.elapsedRealtimeNanos() - LAST_LOCATION_MAX_AGE_NANOS

		// each provider keeps its own last fix, take the freshest one that is still recent enough
		val location = availableProviders
			.mapNotNull { provider ->
				runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
			}
			.filter { it.elapsedRealtimeNanos >= oldestAccepted }
			.maxByOrNull { it.elapsedRealtimeNanos }
			?: return Result.failure(CannotFoundLastLocationException())

		return Result.success(location.toDomainModel())
	}

	@SuppressLint("MissingPermission")
	private suspend fun getCurrentLocation(): Result<BaseLocationModel> {
		val manager = locationManager ?: return Result.failure(LocationProviderNotFoundException())
		val provider = availableProviders.firstOrNull()
			?: return Result.failure(LocationProviderNotFoundException())

		return try {
			val location = manager.awaitCurrentLocation(provider)
				?: return Result.failure(CurrentLocationTimeoutException())
			Result.success(location.toDomainModel())
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override suspend fun invoke(fetchCurrentIfNotFound: Boolean): Result<BaseLocationModel> {
		return when {
			!_hasLocationPermission -> Result.failure(LocationPermissionNotFoundException())
			!isLocationEnabled -> Result.failure(LocationNotEnabledException())
			availableProviders.isEmpty() -> Result.failure(LocationProviderNotFoundException())

			else -> {
				val lastLocation = getLastKnownLocation()
				if (lastLocation.isSuccess) return lastLocation
				else {
					val isFetchCurrentAllowed =
						lastLocation.exceptionOrNull() is CannotFoundLastLocationException && fetchCurrentIfNotFound
					if (!isFetchCurrentAllowed) return lastLocation
					getCurrentLocation()
				}
			}
		}
	}

	private fun Location.toDomainModel() = BaseLocationModel(
		latitude = latitude,
		longitude = longitude,
		accuracy = if (hasAccuracy()) accuracy else .0f
	)
}

/**
 * Single shot fix from [provider], null if the provider gives up or nothing arrives in time. The
 * compat helper takes no duration, so the wait is capped here and the timeout cancels the request
 * through the signal. The consumer runs on the calling thread, it only completes a continuation.
 */
@SuppressLint("MissingPermission")
private suspend fun LocationManager.awaitCurrentLocation(provider: String): Location? =
	withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT) {
		suspendCancellableCoroutine { cont ->
			val signal = CancellationSignal()
			cont.invokeOnCancellation { signal.cancel() }

			LocationManagerCompat.getCurrentLocation(
				this@awaitCurrentLocation,
				provider,
				signal,
				Executor { command -> command.run() }
			) { location ->
				if (cont.isActive) cont.resume(location)
			}
		}
	}
