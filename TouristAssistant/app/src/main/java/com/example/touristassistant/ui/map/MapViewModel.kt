package com.example.touristassistant.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touristassistant.data.models.Place
import com.example.touristassistant.data.repositories.PlacesRepository
import com.example.touristassistant.data.repositories.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана карты.
 *
 * Отвечает за управление состоянием карты, местами и взаимодействием с пользователем.
 * Использует [PlacesRepository] для получения данных о местах и [RouteRepository]
 * для работы с маршрутами.
 *
 * @property placesRepository репозиторий для работы с местами
 * @property routeRepository репозиторий для работы с маршрутами
 */

@HiltViewModel
class MapViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val _routeRepository: RouteRepository
) : ViewModel() {

    private val _places = MutableStateFlow<List<Place>>(emptyList())

    /**
     * Поток, содержащий список всех мест.
     */

    val places: StateFlow<List<Place>> = _places

    private val _selectedPlace = MutableStateFlow<Place?>(null)

    /**
     * Поток, содержащий выбранное место или null.
     */

    val selectedPlace: StateFlow<Place?> = _selectedPlace

    private val _isLoading = MutableStateFlow(true)

    /**
     * Поток, указывающий на состояние загрузки.
     */

    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Репозиторий для работы с маршрутами.
     */

    val routeRepository: RouteRepository
        get() = _routeRepository

    init {
        Log.i("MapViewModel", "ViewModel initialized")
        loadPlaces()
    }

    /**
     * Загружает места из репозитория.
     */

    fun loadPlaces() {
        Log.i("MapViewModel", "Loading places...")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                placesRepository.getAllPlaces().collect { placesList ->
                    _places.value = placesList
                    _isLoading.value = false
                    Log.i("MapViewModel", "Places loaded successfully: ${placesList.size} items")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("MapViewModel", "Error loading places", e)
            }
        }
    }

    /**
     * Выбирает место для отображения на карте.
     *
     * @param place место для выбора или null для сброса выбора
     */

    fun selectPlace(place: Place?) {
        Log.d("MapViewModel", "Selecting place: ${place?.name ?: "null"}")
        _selectedPlace.value = place
    }

    /**
     * Переключает статус "Избранное" для места.
     *
     * @param placeId идентификатор места
     */

    fun toggleFavorite(placeId: String) {
        Log.i("MapViewModel", "Toggling favorite for place ID: $placeId")
        viewModelScope.launch {
            try {
                placesRepository.toggleFavorite(placeId)
                Log.d("MapViewModel", "Favorite toggled successfully for place ID: $placeId")
                loadPlaces()
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error toggling favorite for place ID: $placeId", e)
            }
        }
    }

    override fun onCleared() {
        Log.d("MapViewModel", "ViewModel cleared")
        super.onCleared()
    }
}