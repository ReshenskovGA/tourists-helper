package com.example.touristassistant.data.repositories

import com.example.touristassistant.data.models.Place
import com.example.touristassistant.data.models.PlaceType
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с местами.
 *
 * Определяет интерфейс для операций с данными о местах.
 * Реализация абстрагирует доступ к данным и предоставляет
 * методы для работы с избранным, поиска и фильтрации.
 */

interface PlacesRepository {
    /**
     * Получает все места.
     *
     * @return Flow, эмиттирующий список всех мест
     */

    fun getAllPlaces(): Flow<List<Place>>

    /**
     * Получает места определенного типа.
     *
     * @param type тип места для фильтрации
     * @return Flow, эмиттирующий список мест указанного типа
     */

    fun getPlacesByType(type: PlaceType): Flow<List<Place>>

    /**
     * Получает место по идентификатору.
     *
     * @param id уникальный идентификатор места
     * @return Flow, эмиттирующий место или null если не найдено
     */

    fun getPlaceById(id: String): Flow<Place?>

    /**
     * Получает места вблизи указанных координат.
     *
     * @param lat широта центра поиска
     * @param lon долгота центра поиска
     * @param radius радиус поиска в метрах
     * @return Flow, эмиттирующий список близлежащих мест
     */

    fun getNearbyPlaces(lat: Double, lon: Double, radius: Double): Flow<List<Place>>

    /**
     * Переключает статус "Избранное" для места.
     *
     * @param placeId идентификатор места
     */

    suspend fun toggleFavorite(placeId: String)

    /**
     * Обновляет информацию о месте.
     *
     * @param place объект места с обновленными данными
     */

    suspend fun updatePlace(place: Place)

    /**
     * Добавляет новое место.
     *
     * @param place объект нового места
     */

    suspend fun addPlace(place: Place)

    /**
     * Удаляет место.
     *
     * @param placeId идентификатор места для удаления
     */

    suspend fun deletePlace(placeId: String)
}