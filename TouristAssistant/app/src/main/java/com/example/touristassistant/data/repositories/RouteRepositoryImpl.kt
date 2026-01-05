package com.example.touristassistant.data.repositories

import android.util.Log
import com.example.touristassistant.data.dao.RouteDao
import com.example.touristassistant.data.models.Route
import com.example.touristassistant.data.models.RoutePoint
import com.example.touristassistant.data.models.RoutePointType
import com.example.touristassistant.utils.LocationUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Реализация репозитория маршрутов.
 *
 * Использует [RouteDao] для доступа к данным и предоставляет
 * бизнес-логику для расчета и управления маршрутами.
 *
 * @property routeDao DAO для работы с маршрутами
 */

class RouteRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao
) : RouteRepository {

    /**
     * @see RouteRepository.calculateRoute
     * @throws Exception если произошла ошибка при расчете маршрута
     */

    override suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        waypoints: List<Pair<Double, Double>>
    ): Result<Route> {
        Log.i("RouteRepository", "Calculating route: start($startLat,$startLon) -> end($endLat,$endLon), waypoints: ${waypoints.size}")
        return try {
            val points = mutableListOf<RoutePoint>().apply {
                add(RoutePoint(startLat, startLon, "Старт", RoutePointType.START))
                Log.v("RouteRepository", "Added start point")

                waypoints.forEachIndexed { index, (lat, lon) ->
                    add(RoutePoint(lat, lon, "Точка ${index + 1}", RoutePointType.WAYPOINT))
                    Log.v("RouteRepository", "Added waypoint $index: ($lat,$lon)")
                }

                add(RoutePoint(endLat, endLon, "Финиш", RoutePointType.DESTINATION))
                Log.v("RouteRepository", "Added destination point")
            }

            val distance = calculateTotalDistance(points)
            val duration = calculateEstimatedDuration(distance)

            val polyline = generatePolyline(points)

            val routeName = if (points.size == 2) {
                "От старта до финиша"
            } else {
                "Маршрут через ${points.size - 2} точек"
            }

            val route = Route(
                id = System.currentTimeMillis().toString(),
                name = routeName,
                description = "Рассчитанный маршрут",
                points = points,
                distance = distance,
                duration = duration,
                polyline = polyline
            )
            Log.i("RouteRepository", "Route calculated successfully: $routeName, distance=${distance}m")

            Result.success(route)
        } catch (e: Exception) {
            Log.e("RouteRepository", "Error calculating route", e)
            Result.failure(e)
        }
    }

    /**
     * Вычисляет общее расстояние маршрута.
     *
     * @param points список точек маршрута
     * @return общее расстояние в метрах
     */

    private fun calculateTotalDistance(points: List<RoutePoint>): Double {
        var totalDistance = 0.0

        for (i in 0 until points.size - 1) {
            val distance = LocationUtils.calculateDistance(
                points[i].latitude,
                points[i].longitude,
                points[i + 1].latitude,
                points[i + 1].longitude
            )
            totalDistance += distance
        }

        return totalDistance
    }

    /**
     * Оценивает время прохождения маршрута.
     *
     * @param distance расстояние в метрах
     * @return примерное время в секундах
     */

    private fun calculateEstimatedDuration(distance: Double): Int {
        // 5 км/ч = 1.3889 м/с
        val averageSpeed = 1.3889
        return (distance / averageSpeed).toInt()
    }

    /**
     * Генерирует строковое представление полилинии маршрута.
     *
     * @param points список точек маршрута
     * @return строку с координатами точек, разделенных точкой с запятой
     */

    private fun generatePolyline(points: List<RoutePoint>): String {
        return points.joinToString(";") { "${it.latitude},${it.longitude}" }
    }

    /**
     * Получает маршрут по идентификатору.
     *
     * @param id идентификатор маршрута
     * @return Flow, эмиттирующий маршрут или null
     */

    private fun getRouteById(id: String): Flow<Route?> {
        return routeDao.getRouteById(id)
    }

    /**
     * @see RouteRepository.getAllRoutes
     */

    override suspend fun getAllRoutes(): Flow<List<Route>> {
        Log.d("RouteRepository", "Getting all routes")
        return routeDao.getAllRoutes()
    }

    /**
     * @see RouteRepository.getSavedRoutes
     */

    override fun getSavedRoutes(): Flow<List<Route>> {
        Log.d("RouteRepository", "Getting saved routes")
        return routeDao.getSavedRoutes()
    }

    /**
     * @see RouteRepository.saveRoute
     * @throws Exception если произошла ошибка при сохранении
     */

    override suspend fun saveRoute(route: Route) {
        Log.i("RouteRepository", "Saving route: ${route.name} (ID: ${route.id})")
        try {
            routeDao.insertRoute(route.copy(isSaved = true))
            Log.i("RouteRepository", "Route saved successfully")
        } catch (e: Exception) {
            Log.e("RouteRepository", "Error saving route", e)
            throw e
        }
    }

    /**
     * @see RouteRepository.deleteRoute
     * @throws Exception если произошла ошибка при удалении
     */

    override suspend fun deleteRoute(routeId: String) {
        Log.i("RouteRepository", "Deleting route: $routeId")
        try {
            val route = routeDao.getRouteById(routeId).firstOrNull()
            route?.let {
                routeDao.deleteRoute(it)
                Log.i("RouteRepository", "Route deleted successfully: ${it.name}")
            } ?: run {
                Log.w("RouteRepository", "Route not found: $routeId")
            }
        } catch (e: Exception) {
            Log.e("RouteRepository", "Error deleting route", e)
            throw e
        }
    }

    /**
     * @see RouteRepository.updateRoute
     * @throws Exception если произошла ошибка при обновлении
     */

    override suspend fun updateRoute(route: Route) {
        Log.i("RouteRepository", "Updating route: ${route.name} (ID: ${route.id})")
        try {
            routeDao.updateRoute(route)
            Log.i("RouteRepository", "Route updated successfully")
        } catch (e: Exception) {
            Log.e("RouteRepository", "Error updating route", e)
            throw e
        }
    }

    /**
     * @see RouteRepository.calculateRouteFromCurrentLocation
     * @throws Exception если произошла ошибка при расчете маршрута
     */

    override suspend fun calculateRouteFromCurrentLocation(
        destinationLat: Double,
        destinationLon: Double
    ): Result<Route> {
        Log.i("RouteRepository", "Calculating route from current location to ($destinationLat,$destinationLon)")
        return try {
            val startLat = 55.7558
            val startLon = 37.6173
            Log.d("RouteRepository", "Using default start location: ($startLat,$startLon)")

            return calculateRoute(startLat, startLon, destinationLat, destinationLon, emptyList())
        } catch (e: Exception) {
            Log.e("RouteRepository", "Error calculating route from current location", e)
            Result.failure(e)
        }
    }
}