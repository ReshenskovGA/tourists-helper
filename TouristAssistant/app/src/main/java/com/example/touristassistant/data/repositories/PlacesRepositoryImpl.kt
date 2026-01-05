package com.example.touristassistant.data.repositories

import android.util.Log
import com.example.touristassistant.data.dao.PlaceDao
import com.example.touristassistant.data.models.Place
import com.example.touristassistant.data.models.PlaceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Реализация репозитория мест.
 *
 * Использует [PlaceDao] для доступа к данным и предоставляет
 * бизнес-логику для работы с местами.
 *
 * @property placeDao DAO для работы с местами
 */

class PlacesRepositoryImpl @Inject constructor(
    private val placeDao: PlaceDao
) : PlacesRepository {

    /**
     * @see PlacesRepository.getAllPlaces
     */

    override fun getAllPlaces(): Flow<List<Place>> {
        Log.d("PlacesRepository", "Getting all places")
        return placeDao.getAllPlaces()
    }

    /**
     * @see PlacesRepository.getPlacesByType
     */

    override fun getPlacesByType(type: PlaceType): Flow<List<Place>> {
        Log.d("PlacesRepository", "Getting places by type: $type")
        return placeDao.getPlacesByType(type)
    }

    /**
     * @see PlacesRepository.getPlaceById
     */

    override fun getPlaceById(id: String): Flow<Place?> {
        Log.d("PlacesRepository", "Getting place by ID: $id")
        return placeDao.getPlaceById(id)
    }

    /**
     * @see PlacesRepository.getNearbyPlaces
     */

    override fun getNearbyPlaces(lat: Double, lon: Double, radius: Double): Flow<List<Place>> {
        Log.i("PlacesRepository", "Getting nearby places: lat=$lat, lon=$lon, radius=$radius")
        val degreeRadius = radius / 111000.0
        val minLat = lat - degreeRadius
        val maxLat = lat + degreeRadius
        val minLon = lon - degreeRadius
        val maxLon = lon + degreeRadius

        Log.v("PlacesRepository", "Search bounds: lat[$minLat,$maxLat], lon[$minLon,$maxLon]")

        return placeDao.getNearbyPlaces(minLat, maxLat, minLon, maxLon)
    }

    /**
     * @see PlacesRepository.toggleFavorite
     * @throws Exception если место не найдено или произошла ошибка базы данных
     */

    override suspend fun toggleFavorite(placeId: String) {
        Log.i("PlacesRepository", "Toggling favorite for place: $placeId")
        try {
            val placeFlow = placeDao.getPlaceById(placeId)
            val place = placeFlow.first() ?: run {
                Log.w("PlacesRepository", "Place not found: $placeId")
                return
            }

            val newFavoriteStatus = !place.isFavorite
            placeDao.updateFavoriteStatus(placeId, newFavoriteStatus)
            Log.i("PlacesRepository", "Favorite status updated: $placeId -> $newFavoriteStatus")
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error toggling favorite for place: $placeId", e)
            throw e
        }
    }

    /**
     * @see PlacesRepository.updatePlace
     * @throws Exception если произошла ошибка при обновлении
     */

    override suspend fun updatePlace(place: Place) {
        Log.i("PlacesRepository", "Updating place: ${place.name} (ID: ${place.id})")
        try {
            placeDao.updatePlace(place)
            Log.d("PlacesRepository", "Place updated successfully")
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error updating place", e)
            throw e
        }
    }

    /**
     * @see PlacesRepository.addPlace
     * @throws Exception если произошла ошибка при добавлении
     */

    override suspend fun addPlace(place: Place) {
        Log.i("PlacesRepository", "Adding new place: ${place.name}")
        try {
            placeDao.insertPlace(place)
            Log.i("PlacesRepository", "Place added successfully: ${place.name}")
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error adding place", e)
            throw e
        }
    }

    /**
     * @see PlacesRepository.deletePlace
     * @throws Exception если произошла ошибка при удалении
     */

    override suspend fun deletePlace(placeId: String) {
        Log.i("PlacesRepository", "Deleting place: $placeId")
        try {
            val placeFlow = placeDao.getPlaceById(placeId)
            val place = placeFlow.first()
            place?.let {
                placeDao.deletePlace(it)
                Log.i("PlacesRepository", "Place deleted successfully: ${it.name}")
            } ?: run {
                Log.w("PlacesRepository", "Place not found for deletion: $placeId")
            }
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error deleting place", e)
            throw e
        }
    }
}