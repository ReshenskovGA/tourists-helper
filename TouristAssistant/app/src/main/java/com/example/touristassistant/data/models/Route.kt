package com.example.touristassistant.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Класс, представляющий маршрут в приложении.
 *
 * @property id уникальный идентификатор маршрута
 * @property name название маршрута
 * @property description описание маршрута
 * @property points список точек маршрута
 * @property distance общая дистанция в метрах
 * @property duration примерное время прохождения в секундах
 * @property polyline строковое представление полилинии маршрута
 * @property isSaved флаг, указывающий сохранен ли маршрут
 */

@Entity(tableName = "routes")
data class Route(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val points: List<RoutePoint>,
    val distance: Double,
    val duration: Int,
    val polyline: String,
    val isSaved: Boolean = false
)