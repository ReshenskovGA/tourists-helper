package com.example.touristassistant.data.dao

import androidx.room.*
import com.example.touristassistant.data.models.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Data Access Object (DAO) для работы с таблицей маршрутов.
 *
 * Предоставляет методы для выполнения операций CRUD с объектами [Route].
 * Все методы, возвращающие Flow, автоматически обновляются при изменениях в базе данных.
 */

@Dao
interface RouteDao {

    /**
     * Получает все маршруты из базы данных.
     *
     * @return Flow, эмиттирующий список всех маршрутов
     */

    @Query("SELECT * FROM routes")
    fun getAllRoutes(): Flow<List<Route>>

    /**
     * Получает сохраненные маршруты.
     *
     * @return Flow, эмиттирующий список маршрутов с флагом isSaved = true
     */

    @Query("SELECT * FROM routes WHERE isSaved = 1")
    fun getSavedRoutes(): Flow<List<Route>>

    /**
     * Получает маршрут по его идентификатору.
     *
     * @param id уникальный идентификатор маршрута
     * @return Flow, эмиттирующий маршрут или null если не найден
     */

    @Query("SELECT * FROM routes WHERE id = :id")
    fun getRouteById(id: String): Flow<Route?>

    /**
     * Вставляет новый маршрут в базу данных.
     *
     * При конфликте заменяет существующую запись.
     *
     * @param route объект маршрута для вставки
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route)

    /**
     * Обновляет существующий маршрут в базе данных.
     *
     * @param route объект маршрута с обновленными данными
     */

    @Update
    suspend fun updateRoute(route: Route)

    /**
     * Удаляет маршрут из базы данных.
     *
     * @param route объект маршрута для удаления
     */

    @Delete
    suspend fun deleteRoute(route: Route)
}