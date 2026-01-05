package com.example.touristassistant.data

import android.content.Context
import android.util.Log
import com.example.touristassistant.data.dao.PlaceDao
import com.example.touristassistant.data.dao.RouteDao
import com.example.touristassistant.data.models.Place
import com.example.touristassistant.data.models.PlaceType
import com.example.touristassistant.data.models.Route
import com.example.touristassistant.data.models.RoutePoint
import com.example.touristassistant.data.models.RoutePointType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Преобразует список точек маршрута в JSON-строку
 *
 * @param points список точек маршрута
 * @return JSON-представление списка точек
 */

@Singleton
class DatabaseSeeder @Inject constructor(
    private val placeDao: PlaceDao,
    private val routeDao: RouteDao,
    @ApplicationContext private val context: Context
) {

    /**
     * Заполняет базу данных начальными данными.
     *
     * Метод проверяет, пуста ли база данных, и если да - заполняет её
     * демонстрационными данными о местах и тестовым маршрутом.
     * Выполняется в фоновом потоке через CoroutineScope.
     */

    fun seedInitialData() {
        Log.i("DatabaseSeeder", "Starting database seeding process")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DatabaseSeeder", "Coroutine started for database seeding")

                // Проверка наличия данных перед заполнением
                val placesCount = placeDao.getCount()
                Log.i("DatabaseSeeder", "Current places count in DB: $placesCount")

                if (placesCount == 0) {
                    // Генерация и вставка демонстрационных данных
                    Log.i("DatabaseSeeder", "Database is empty, seeding initial data...")
                    val initialPlaces = getDefaultPlaces()
                    Log.d("DatabaseSeeder", "Generated ${initialPlaces.size} default places")

                    initialPlaces.forEachIndexed { index, place ->
                        placeDao.insertPlace(place)
                        Log.v("DatabaseSeeder", "Inserted place [$index]: ${place.name} (ID: ${place.id})")
                    }
                    Log.i("DatabaseSeeder", "Places seeding completed. Total places: ${initialPlaces.size}")
                } else {
                    Log.i("DatabaseSeeder", "Database already has $placesCount places. Skipping seeding.")
                }

                // Добавление тестового маршрута
                seedTestRoute()
                Log.i("DatabaseSeeder", "Database seeding process completed successfully")
            } catch (e: Exception) {
                Log.e("DatabaseSeeder", "Critical error during data seeding", e)
            }
        }
    }

    /**
     * Добавляет тестовый маршрут в базу данных.
     *
     * Создает маршрут от Москвы до Кремля для демонстрации функционала.
     *
     * @throws Exception если произошла ошибка при работе с базой данных
     */

    private suspend fun seedTestRoute() {
        Log.d("DatabaseSeeder", "Starting test route seeding")
        try {
            val routeDao = AppDatabase.getDatabase(context).routeDao()
            val routeCount = routeDao.getAllRoutes().firstOrNull()?.size ?: 0
            Log.d("DatabaseSeeder", "Current routes count in DB: $routeCount")

            if (routeCount == 0) {
                val testRoute = Route(
                    id = "test_route_1",
                    name = "Тестовый маршрут",
                    description = "Маршрут для тестирования",
                    points = listOf(
                        RoutePoint(55.7558, 37.6173, "Москва", RoutePointType.START),
                        RoutePoint(55.7517, 37.6178, "Кремль", RoutePointType.DESTINATION)
                    ),
                    distance = 500.0,
                    duration = 600,
                    polyline = "55.7558,37.6173;55.7517,37.6178",
                    isSaved = true
                )

                routeDao.insertRoute(testRoute)
                Log.i("DatabaseSeeder", "Test route added: ${testRoute.name} (ID: ${testRoute.id})")
            } else {
                Log.d("DatabaseSeeder", "Routes already exist. Skipping test route seeding.")
            }
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Error seeding test route", e)
        }
    }

    /**
     * Получает количество мест в базе данных.
     *
     * @return количество записей в таблице мест
     */

    private suspend fun getPlacesCount(): Int {
        Log.v("DatabaseSeeder", "Getting places count")
        var count = 0
        try {
            placeDao.getAllPlaces().collect { places ->
                count = places.size
            }
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Error getting places count", e)
        }
        Log.d("DatabaseSeeder", "Places count: $count")
        return count
    }

    /**
     * Создает список демонстрационных мест.
     *
     * Включает достопримечательности, рестораны, кафе, отели и другие
     * типы мест в Москве.
     *
     * @return список демонстрационных мест
     */

    private fun getDefaultPlaces(): List<Place> {
        Log.d("DatabaseSeeder", "Generating default places list")
        return listOf(
            Place(
                id = "1",
                name = "Красная площадь",
                description = "Главная площадь Москвы, исторический и культурный центр",
                type = PlaceType.TOURIST_ATTRACTION,
                latitude = 55.7539,
                longitude = 37.6208,
                address = "Красная площадь, Москва, Россия",
                rating = 4.9f,
                priceLevel = 0,
                openingHours = "Круглосуточно",
                photos = emptyList(),
                isFavorite = false
            ),
            Place(
                id = "2",
                name = "Московский Кремль",
                description = "Исторический укрепленный комплекс в центре Москвы",
                type = PlaceType.TOURIST_ATTRACTION,
                latitude = 55.7517,
                longitude = 37.6178,
                address = "Москва, Кремль",
                rating = 4.8f,
                priceLevel = 3,
                openingHours = "10:00-17:00",
                photos = emptyList(),
                isFavorite = false
            ),
            Place(
                id = "3",
                name = "Собор Василия Блаженного",
                description = "Православный храм на Красной площади",
                type = PlaceType.TOURIST_ATTRACTION,
                latitude = 55.7525,
                longitude = 37.6231,
                address = "Красная площадь, 7, Москва, Россия",
                rating = 4.7f,
                priceLevel = 2,
                openingHours = "11:00-18:00",
                photos = emptyList(),
                isFavorite = false
            ),

            // Рестораны
            Place(
                id = "4",
                name = "Ресторан «Турандот»",
                description = "Роскошный ресторан с европейской и азиатской кухней",
                type = PlaceType.RESTAURANT,
                latitude = 55.7647,
                longitude = 37.6056,
                address = "Тверской бульвар, 26, Москва",
                rating = 4.8f,
                priceLevel = 4,
                openingHours = "12:00-00:00",
                phone = "+7 (495) 739-00-11",
                website = "http://www.turandot-palace.ru",
                photos = emptyList(),
                isFavorite = false
            ),
            Place(
                id = "5",
                name = "Café Pushkin",
                description = "Легендарный ресторан русской кухни",
                type = PlaceType.RESTAURANT,
                latitude = 55.7601,
                longitude = 37.6195,
                address = "Тверской бульвар, 26А, Москва",
                rating = 4.7f,
                priceLevel = 4,
                openingHours = "09:00-23:00",
                phone = "+7 (495) 739-00-33",
                website = "http://www.cafe-pushkin.ru",
                photos = emptyList(),
                isFavorite = false
            ),

            Place(
                id = "6",
                name = "Кофемания",
                description = "Популярная сеть кофеен",
                type = PlaceType.CAFE,
                latitude = 55.7558,
                longitude = 37.6173,
                address = "Тверская ул., 10, Москва",
                rating = 4.3f,
                priceLevel = 2,
                openingHours = "08:00-23:00",
                phone = "+7 (495) 228-11-11",
                photos = emptyList(),
                isFavorite = false
            ),
            Place(
                id = "7",
                name = "Ritz-Carlton Moscow",
                description = "Пятизвездочный отель в центре Москвы",
                type = PlaceType.HOTEL,
                latitude = 55.7575,
                longitude = 37.6219,
                address = "Тверская ул., 3, Москва",
                rating = 4.9f,
                priceLevel = 5,
                openingHours = "Круглосуточно",
                phone = "+7 (495) 225-88-88",
                website = "http://www.ritzcarlton.com/moscow",
                photos = emptyList(),
                isFavorite = false
            ),
            Place(
                id = "8",
                name = "ГУМ",
                description = "Крупный торговый комплекс на Красной площади",
                type = PlaceType.SHOPPING,
                latitude = 55.7547,
                longitude = 37.6219,
                address = "Красная площадь, 3, Москва",
                rating = 4.6f,
                priceLevel = 3,
                openingHours = "10:00-22:00",
                website = "http://www.gum.ru",
                photos = emptyList(),
                isFavorite = false
            ),

            Place(
                id = "9",
                name = "Станция метро Охотный Ряд",
                description = "Станция метро в центре Москвы",
                type = PlaceType.TRANSPORT,
                latitude = 55.7572,
                longitude = 37.6169,
                address = "Моховая ул., Москва",
                rating = 4.0f,
                priceLevel = 0,
                openingHours = "05:30-01:00",
                photos = emptyList(),
                isFavorite = false
            ),

            Place(
                id = "10",
                name = "Аптека 36.6",
                description = "Круглосуточная аптека",
                type = PlaceType.PHARMACY,
                latitude = 55.7580,
                longitude = 37.6180,
                address = "Тверская ул., 15, Москва",
                rating = 4.2f,
                priceLevel = 0,
                openingHours = "Круглосуточно",
                phone = "+7 (495) 797-63-36",
                photos = emptyList(),
                isFavorite = false
            ),

            Place(
                id = "11",
                name = "Сбербанк",
                description = "Отделение банка с банкоматом",
                type = PlaceType.BANK,
                latitude = 55.7590,
                longitude = 37.6190,
                address = "Тверская ул., 20, Москва",
                rating = 3.8f,
                priceLevel = 0,
                openingHours = "09:00-20:00",
                phone = "900",
                photos = emptyList(),
                isFavorite = false
            ),

            Place(
                id = "12",
                name = "Общественный туалет",
                description = "Общественный туалет в центре города",
                type = PlaceType.TOILET,
                latitude = 55.7565,
                longitude = 37.6225,
                address = "Рядом с ГУМом, Москва",
                rating = 3.5f,
                priceLevel = 1,
                openingHours = "08:00-22:00",
                photos = emptyList(),
                isFavorite = false
            ),

            Place(
                id = "13",
                name = "Городская клиническая больница №1",
                description = "Многопрофильная больница",
                type = PlaceType.HOSPITAL,
                latitude = 55.7737,
                longitude = 37.6331,
                address = "Ленинский проспект, 8, Москва",
                rating = 4.1f,
                priceLevel = 0,
                openingHours = "Круглосуточно",
                phone = "+7 (495) 536-91-16",
                photos = emptyList(),
                isFavorite = false
            ),

            Place(
                id = "14",
                name = "Парк Горького",
                description = "Центральный парк культуры и отдыха",
                type = PlaceType.OTHER,
                latitude = 55.7280,
                longitude = 37.6010,
                address = "Крымский Вал, 9, Москва",
                rating = 4.7f,
                priceLevel = 0,
                openingHours = "Круглосуточно",
                photos = emptyList(),
                isFavorite = false
            ),

            Place(
                id = "15",
                name = "ВДНХ",
                description = "Выставка достижений народного хозяйства",
                type = PlaceType.OTHER,
                latitude = 55.8300,
                longitude = 37.6300,
                address = "Проспект Мира, 119, Москва",
                rating = 4.6f,
                priceLevel = 0,
                openingHours = "Круглосуточно",
                photos = emptyList(),
                isFavorite = false
            )
        )
    }
}