package com.example.touristassistant.data.dao

import androidx.room.*
import com.example.touristassistant.data.models.Place
import com.example.touristassistant.data.models.PlaceType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для работы с таблицей мест.
 *
 * Предоставляет методы для выполнения операций CRUD с объектами [Place].
 * Все методы, возвращающие Flow, автоматически обновляются при изменениях в базе данных.
 */

@Dao
interface PlaceDao {

    /**
     * Получает все места из базы данных.
     *
     * @return Flow, эмиттирующий список всех мест
     */

    @Query("SELECT * FROM places")
    fun getAllPlaces(): Flow<List<Place>>

    /**
     * Получает места определенного типа.
     *
     * @param type тип места для фильтрации
     * @return Flow, эмиттирующий список мест указанного типа
     */

    @Query("SELECT * FROM places WHERE type = :type")
    fun getPlacesByType(type: PlaceType): Flow<List<Place>>

    /**
     * Получает место по его идентификатору.
     *
     * @param id уникальный идентификатор места
     * @return Flow, эмиттирующий место или null если не найдено
     */

    @Query("SELECT * FROM places WHERE id = :id")
    fun getPlaceById(id: String): Flow<Place?>

    /**
     * Ищет места по текстовому запросу.
     *
     * Поиск выполняется по полям name и description.
     *
     * @param query текст для поиска
     * @return Flow, эмиттирующий список найденных мест
     */

    @Query("SELECT * FROM places WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchPlaces(query: String): Flow<List<Place>>

    /**
     * Получает места в заданной географической области.
     *
     * @param minLat минимальная широта
     * @param maxLat максимальная широта
     * @param minLon минимальная долгота
     * @param maxLon максимальная долгота
     * @return Flow, эмиттирующий список мест в указанных границах
     */

    @Query("""
        SELECT * FROM places 
        WHERE (latitude BETWEEN :minLat AND :maxLat) 
        AND (longitude BETWEEN :minLon AND :maxLon)
    """)
    fun getNearbyPlaces(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Flow<List<Place>>

    /**
     * Вставляет новое место в базу данных.
     *
     * При конфликте заменяет существующую запись.
     *
     * @param place объект места для вставки
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: Place)

    /**
     * Обновляет существующее место в базе данных.
     *
     * @param place объект места с обновленными данными
     */

    @Update
    suspend fun updatePlace(place: Place)

    /**
     * Обновляет статус "Избранное" для места.
     *
     * @param id идентификатор места
     * @param isFavorite новый статус избранного
     */

    @Query("UPDATE places SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    /**
     * Удаляет место из базы данных.
     *
     * @param place объект места для удаления
     */

    @Delete
    suspend fun deletePlace(place: Place)

    /**
     * Получает количество мест в базе данных.
     *
     * @return количество записей в таблице places
     */

    @Query("SELECT COUNT(*) FROM places")
    suspend fun getCount(): Int
}