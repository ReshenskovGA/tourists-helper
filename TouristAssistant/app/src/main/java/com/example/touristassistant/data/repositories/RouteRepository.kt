package com.example.touristassistant.data.repositories

import com.example.touristassistant.data.models.Route
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с маршрутами.
 *
 * Определяет интерфейс для операций с маршрутами, включая
 * расчет маршрутов и управление сохраненными маршрутами.
 */

interface RouteRepository {
    /**
     * Рассчитывает маршрут между точками.
     *
     * @param startLat широта начальной точки
     * @param startLon долгота начальной точки
     * @param endLat широта конечной точки
     * @param endLon долгота конечной точки
     * @param waypoints список промежуточных точек (широта, долгота)
     * @return Result с рассчитанным маршрутом или ошибкой
     */

    suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        waypoints: List<Pair<Double, Double>> = emptyList()
    ): Result<Route>

    /**
     * Рассчитывает маршрут от текущего местоположения до точки назначения.
     *
     * @param destinationLat широта точки назначения
     * @param destinationLon долгота точки назначения
     * @return Result с рассчитанным маршрутом или ошибкой
     */

    suspend fun calculateRouteFromCurrentLocation(
        destinationLat: Double,
        destinationLon: Double
    ): Result<Route>

    /**
     * Получает сохраненные маршруты.
     *
     * @return Flow, эмиттирующий список сохраненных маршрутов
     */

    fun getSavedRoutes(): Flow<List<Route>>

    /**
     * Получает все маршруты.
     *
     * @return Flow, эмиттирующий список всех маршрутов
     */

    suspend fun getAllRoutes(): Flow<List<Route>>

    /**
     * Сохраняет маршрут.
     *
     * @param route маршрут для сохранения
     */

    suspend fun saveRoute(route: Route)

    /**
     * Удаляет маршрут.
     *
     * @param routeId идентификатор маршрута для удаления
     */

    suspend fun deleteRoute(routeId: String)

    /**
     * Обновляет маршрут.
     *
     * @param route маршрут с обновленными данными
     */

    suspend fun updateRoute(route: Route)
}