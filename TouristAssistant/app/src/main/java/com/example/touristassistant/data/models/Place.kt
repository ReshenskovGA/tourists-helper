package com.example.touristassistant.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Класс, представляющий место в приложении.
 *
 * @property id уникальный идентификатор места
 * @property name название места
 * @property description описание места
 * @property type тип места (см. [PlaceType])
 * @property latitude географическая широта
 * @property longitude географическая долгота
 * @property address физический адрес места
 * @property phone контактный телефон (опционально)
 * @property website веб-сайт (опционально)
 * @property rating рейтинг от 0.0 до 5.0
 * @property priceLevel уровень цен от 0 (бесплатно) до 5 (очень дорого)
 * @property openingHours часы работы (опционально)
 * @property photos список URL фотографий
 * @property isFavorite флаг, указывающий, добавлено ли место в избранное
 */

@Entity(tableName = "places")
data class Place(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val type: PlaceType,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phone: String? = null,
    val website: String? = null,
    val rating: Float = 0.0f,
    val priceLevel: Int = 0,
    val openingHours: String? = null,
    val photos: List<String> = emptyList(),
    val isFavorite: Boolean = false
)

/**
 * Перечисление типов мест.
 */

enum class PlaceType {
    /** Достопримечательность */
    TOURIST_ATTRACTION,

    /** Ресторан */
    RESTAURANT,

    /** Кафе */
    CAFE,

    /** Отель */
    HOTEL,

    /** Магазин */
    SHOPPING,

    /** Транспорт */
    TRANSPORT,

    /** Больница */
    HOSPITAL,

    /** Аптека */
    PHARMACY,

    /** Банк */
    BANK,

    /** Туалет */
    TOILET,

    /** Другое */
    OTHER
}

/**
 * Класс, представляющий точку в маршруте.
 *
 * @property latitude географическая широта
 * @property longitude географическая долгота
 * @property name название точки
 * @property type тип точки маршрута (см. [RoutePointType])
 */

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val type: RoutePointType
)

/**
 * Перечисление типов точек маршрута.
 */

enum class RoutePointType {
    /** Начальная точка */
    START,

    /** Конечная точка (назначение) */
    DESTINATION,

    /** Промежуточная точка */
    WAYPOINT
}