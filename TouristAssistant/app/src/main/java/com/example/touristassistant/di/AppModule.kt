package com.example.touristassistant.di

import android.content.Context
import com.example.touristassistant.data.AppDatabase
import com.example.touristassistant.data.DatabaseSeeder
import com.example.touristassistant.data.dao.PlaceDao
import com.example.touristassistant.data.dao.RouteDao
import com.example.touristassistant.data.repositories.PlacesRepository
import com.example.touristassistant.data.repositories.PlacesRepositoryImpl
import com.example.touristassistant.data.repositories.RouteRepository
import com.example.touristassistant.data.repositories.RouteRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль Dagger Hilt для предоставления зависимостей приложения.
 *
 * Определяет зависимости, которые будут доступны во всем приложении.
 * Установлен в [SingletonComponent] для обеспечения единственного экземпляра.
 */

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Предоставляет экземпляр базы данных.
     *
     * @param context контекст приложения
     * @return экземпляр [AppDatabase]
     */

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * Предоставляет DAO для работы с местами.
     *
     * @param database экземпляр базы данных
     * @return экземпляр [PlaceDao]
     */

    @Provides
    fun providePlaceDao(database: AppDatabase): PlaceDao {
        return database.placeDao()
    }

    /**
     * Предоставляет DAO для работы с маршрутами.
     *
     * @param database экземпляр базы данных
     * @return экземпляр [RouteDao]
     */

    @Provides
    fun provideRouteDao(database: AppDatabase): RouteDao {
        return database.routeDao()
    }

    /**
     * Предоставляет репозиторий для работы с местами.
     *
     * @param placeDao DAO для работы с местами
     * @return экземпляр [PlacesRepository]
     */

    @Provides
    @Singleton
    fun providePlacesRepository(placeDao: PlaceDao): PlacesRepository {
        return PlacesRepositoryImpl(placeDao)
    }

    /**
     * Предоставляет репозиторий для работы с маршрутами.
     *
     * @param routeDao DAO для работы с маршрутами
     * @return экземпляр [RouteRepository]
     */

    @Provides
    @Singleton
    fun provideRouteRepository(routeDao: RouteDao): RouteRepository {
        return RouteRepositoryImpl(routeDao)
    }

    /**
     * Предоставляет сеялку базы данных.
     *
     * @param placeDao DAO для работы с местами
     * @param routeDao DAO для работы с маршрутами
     * @param context контекст приложения
     * @return экземпляр [DatabaseSeeder]
     */

    @Provides
    @Singleton
    fun provideDatabaseSeeder(
        placeDao: PlaceDao,
        routeDao: RouteDao,
        @ApplicationContext context: Context
    ): DatabaseSeeder {
        return DatabaseSeeder(placeDao, routeDao, context)
    }
}