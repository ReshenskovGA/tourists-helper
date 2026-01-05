package com.example.touristassistant

import android.app.Application
import android.util.Log
import com.example.touristassistant.data.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import javax.inject.Inject

/**
 * Главный класс приложения Tourist Assistant.
 *
 * Этот класс отвечает за инициализацию приложения, настройку зависимостей через Dagger Hilt,
 * конфигурацию OSMDroid и начальное заполнение базы данных.
 *
 * @property databaseSeeder Сеялка базы данных для инициализации начальных данных
 */

@HiltAndroidApp
class TouristAssistantApplication : Application() {

    /**
     * Внедренная зависимость сеялки базы данных через Dagger Hilt
     */

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    /**
     * Вызывается при создании приложения. Выполняет инициализацию:
     * 1. Настройка OSMDroid
     * 2. Установка User Agent
     * 3. Заполнение базы данных начальными данными
     *
     * @throws Exception в случае ошибок инициализации
     */

    override fun onCreate() {
        super.onCreate()
        Log.i("TouristAssistantApp", "Application onCreate started")

        try {
            // Initialize OSMDroid configuration
            org.osmdroid.config.Configuration.getInstance().load(
                this,
                getSharedPreferences("osmdroid", MODE_PRIVATE)
            )
            Log.d("TouristAssistantApp", "OSMDroid configuration loaded")

            // Set user agent
            Configuration.getInstance().userAgentValue = packageName
            Log.d("TouristAssistantApp", "User agent set: $packageName")

            // Seed initial data
            databaseSeeder.seedInitialData()
            Log.d("TouristAssistantApp", "Database seeding initiated")

            Log.i("TouristAssistantApp", "Application initialized successfully")
        } catch (e: Exception) {
            Log.e("TouristAssistantApp", "Error during application initialization", e)
        }
    }

    /**
     * Вызывается при завершении работы приложения
     */

    override fun onTerminate() {
        Log.i("TouristAssistantApp", "Application terminating")
        super.onTerminate()
    }

    /**
     * Вызывается при нехватке памяти в системе
     */

    override fun onLowMemory() {
        Log.w("TouristAssistantApp", "Low memory warning")
        super.onLowMemory()
    }

    /**
     * Вызывается, когда системе требуется освободить память
     *
     * @param level уровень обрезки памяти
     */

    override fun onTrimMemory(level: Int) {
        Log.d("TouristAssistantApp", "onTrimMemory called with level: $level")
        super.onTrimMemory(level)
    }
}