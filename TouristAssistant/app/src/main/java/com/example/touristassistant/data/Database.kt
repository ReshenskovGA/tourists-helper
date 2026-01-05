package com.example.touristassistant.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.touristassistant.data.dao.PlaceDao
import com.example.touristassistant.data.dao.RouteDao
import com.example.touristassistant.data.models.Place
import com.example.touristassistant.data.models.PlaceType
import com.example.touristassistant.data.models.Route
import com.example.touristassistant.data.models.RoutePoint
import com.example.touristassistant.data.models.RoutePointType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Главная база данных приложения Tourist Assistant.
 *
 * Использует Room для хранения данных о местах и маршрутах.
 * Версия базы данных: 1
 *
 * @property placeDao DAO для работы с местами
 * @property routeDao DAO для работы с маршрутами
 */

@Database(
    entities = [Place::class, Route::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Получает DAO для работы с местами
     *
     * @return экземпляр PlaceDao
     */

    abstract fun placeDao(): PlaceDao

    /**
     * Получает DAO для работы с маршрутами
     *
     * @return экземпляр RouteDao
     */

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Получает экземпляр базы данных (синглтон)
         *
         * @param context контекст приложения
         * @return экземпляр AppDatabase
         */

        fun getDatabase(context: Context): AppDatabase {
            Log.d("AppDatabase", "Getting database instance")
            return INSTANCE ?: synchronized(this) {
                Log.i("AppDatabase", "Creating new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tourist_assistant_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                Log.i("AppDatabase", "Database created successfully")
                instance
            }
        }
    }
}

/**
 * Конвертеры типов для Room Database.
 *
 * Обеспечивает преобразование сложных типов данных в примитивы для хранения в БД
 * и обратное преобразование при чтении.
 */

class Converters {
    private val gson = Gson()

    /**
     * Преобразует JSON-строку в список строк
     *
     * @param value JSON-строка
     * @return список строк или пустой список если строка null
     */

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    /**
     * Преобразует список строк в JSON-строку
     *
     * @param list список строк для преобразования
     * @return JSON-представление списка
     */

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    /**
     * Преобразует строку в перечисление PlaceType
     *
     * @param value строковое представление PlaceType
     * @return соответствующий элемент перечисления PlaceType
     */

    @TypeConverter
    fun fromPlaceType(value: String): PlaceType {
        return PlaceType.valueOf(value)
    }

    /**
     * Преобразует PlaceType в строку
     *
     * @param type элемент перечисления PlaceType
     * @return строковое представление типа
     */

    @TypeConverter
    fun toPlaceType(type: PlaceType): String {
        return type.name
    }

    /**
     * Преобразует строку в перечисление RoutePointType
     *
     * @param value строковое представление RoutePointType
     * @return соответствующий элемент перечисления RoutePointType
     */

    @TypeConverter
    fun fromRoutePointType(value: String): RoutePointType {
        return RoutePointType.valueOf(value)
    }

    /**
     * Преобразует RoutePointType в строку
     *
     * @param type элемент перечисления RoutePointType
     * @return строковое представление типа
     */

    @TypeConverter
    fun toRoutePointType(type: RoutePointType): String {
        return type.name
    }

    /**
     * Преобразует JSON-строку в список точек маршрута
     *
     * @param value JSON-строка с массивом RoutePoint
     * @return список точек маршрута или пустой список если строка null
     */

    @TypeConverter
    fun fromRoutePointList(value: String): List<RoutePoint> {
        val listType = object : TypeToken<List<RoutePoint>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    /**
     * Преобразует список точек маршрута в JSON-строку
     *
     * @param points список точек маршрута
     * @return JSON-представление списка точек
     */

    @TypeConverter
    fun toRoutePointList(points: List<RoutePoint>): String {
        return gson.toJson(points)
    }
}