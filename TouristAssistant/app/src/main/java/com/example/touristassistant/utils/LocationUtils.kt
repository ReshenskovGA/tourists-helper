package com.example.touristassistant.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Утилиты для работы с геолокацией.
 *
 * Предоставляет методы для проверки разрешений, получения последнего известного
 * местоположения и вычисления расстояний между точками.
 */

object LocationUtils {

    /**
     * Проверяет наличие разрешений на доступ к местоположению.
     *
     * @param context контекст приложения
     * @return true если есть хотя бы одно разрешение (FINE или COARSE)
     */

    fun hasLocationPermission(context: Context): Boolean {
        val finePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val result = finePermission || coarsePermission
        Log.d("LocationUtils", "Location permission check: FINE=$finePermission, COARSE=$coarsePermission, RESULT=$result")
        return result
    }

    /**
     * Получает последнее известное местоположение устройства.
     *
     * Пытается получить местоположение от GPS, сети и пассивного провайдера (в порядке приоритета).
     *
     * @param context контекст приложения
     * @return последнее известное местоположение или null если недоступно
     * @throws SecurityException если нет разрешений на доступ к местоположению
     */

    fun getLastKnownLocation(context: Context): Location? {
        Log.d("LocationUtils", "Getting last known location")

        if (!hasLocationPermission(context)) {
            Log.w("LocationUtils", "No location permission, returning null")
            return null
        }

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var location: Location? = null
            var providerUsed = "none"

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                providerUsed = "GPS"
                Log.d("LocationUtils", "Got location from GPS provider")
            }

            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                providerUsed = "NETWORK"
                Log.d("LocationUtils", "Got location from NETWORK provider")
            }

            if (location == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    providerUsed = "PASSIVE"
                    Log.d("LocationUtils", "Got location from PASSIVE provider")
                }
            }

            if (location != null) {
                Log.i("LocationUtils", "Location obtained from $providerUsed: (${location.latitude}, ${location.longitude})")
            } else {
                Log.w("LocationUtils", "No location available from any provider")
            }

            location
        } catch (e: SecurityException) {
            Log.e("LocationUtils", "Security exception when getting location", e)
            null
        } catch (e: Exception) {
            Log.e("LocationUtils", "Exception when getting location", e)
            null
        }
    }

    /**
     * Проверяет, включены ли службы геолокации на устройстве.
     *
     * @param context контекст приложения
     * @return true если службы геолокации включены
     */

    fun isLocationEnabled(context: Context): Boolean {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }

            Log.d("LocationUtils", "Location enabled check: $result")
            result
        } catch (e: Exception) {
            Log.e("LocationUtils", "Error checking location enabled status", e)
            false
        }
    }

    /**
     * Вычисляет расстояние между двумя географическими точками.
     *
     * Использует формулу гаверсинусов для точного расчета.
     *
     * @param lat1 широта первой точки
     * @param lon1 долгота первой точки
     * @param lat2 широта второй точки
     * @param lon2 долгота второй точки
     * @return расстояние между точками в метрах
     */

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        Log.v("LocationUtils", "Calculating distance between ($lat1,$lon1) and ($lat2,$lon2)")
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        val distance = results[0]
        Log.d("LocationUtils", "Distance calculated: ${distance}m")
        return distance
    }

    /**
     * Создает объект Location с заданными координатами.
     *
     * @param latitude широта
     * @param longitude долгота
     * @return объект Location с указанными координатами
     */

    fun createLocation(latitude: Double, longitude: Double): Location {
        Log.v("LocationUtils", "Creating manual location: ($latitude,$longitude)")
        return Location("manual").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
    }
}