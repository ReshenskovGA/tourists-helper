package com.example.touristassistant.ui.places

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touristassistant.data.models.Place
import com.example.touristassistant.data.models.PlaceType
import com.example.touristassistant.data.repositories.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<PlaceType?>(null)
    val selectedCategory: StateFlow<PlaceType?> = _selectedCategory

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        Log.i("PlacesViewModel", "ViewModel initialized")
        loadPlaces()
    }

    /**
     * Загрузка и фильтрация мест с использованием оператора combine.
     * Объединяет три потока: все места, поисковый запрос и выбранную категорию.
     * Выполняет фильтрацию в реальном времени при изменении любого из параметров.
     */
    private fun loadPlaces() {
        Log.i("PlacesViewModel", "Loading places with filters...")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                combine(
                    placesRepository.getAllPlaces(), // Поток всех мест
                    _searchQuery,                    // Поток поискового запроса
                    _selectedCategory                // Поток выбранной категории
                ) { allPlaces, query, category ->
                    // Фильтрация по категории и поисковому запросу
                    Log.d("PlacesViewModel", "Filtering places: query='$query', category=$category")
                    val filtered = allPlaces.filter { place ->
                        val matchesCategory = category == null || place.type == category
                        val matchesSearch = query.isEmpty() ||
                                place.name.contains(query, ignoreCase = true) ||
                                place.description.contains(query, ignoreCase = true) ||
                                place.address.contains(query, ignoreCase = true)
                        matchesCategory && matchesSearch
                    }.sortedByDescending { it.isFavorite } // Сортировка: избранное сверху

                    Log.d("PlacesViewModel", "Filter result: ${filtered.size} of ${allPlaces.size} places")
                    filtered
                }.collect { filteredPlaces ->
                    _places.value = filteredPlaces
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("PlacesViewModel", "Error loading places", e)
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        Log.d("PlacesViewModel", "Search query updated: '$query'")
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: PlaceType?) {
        Log.i("PlacesViewModel", "Category selected: ${category?.name ?: "ALL"}")
        _selectedCategory.value = category
    }

    fun toggleFavorite(placeId: String) {
        Log.i("PlacesViewModel", "Toggling favorite for place ID: $placeId")
        viewModelScope.launch {
            try {
                placesRepository.toggleFavorite(placeId)
                Log.d("PlacesViewModel", "Favorite toggled successfully")
            } catch (e: Exception) {
                Log.e("PlacesViewModel", "Error toggling favorite", e)
            }
        }
    }

    fun refreshPlaces() {
        Log.i("PlacesViewModel", "Refreshing places")
        loadPlaces()
    }

    override fun onCleared() {
        Log.d("PlacesViewModel", "ViewModel cleared")
        super.onCleared()
    }
}