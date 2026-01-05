package com.example.touristassistant.ui.routes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.touristassistant.data.models.Route
import com.example.touristassistant.data.repositories.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _savedRoutes = MutableStateFlow<List<Route>>(emptyList())
    val savedRoutes: StateFlow<List<Route>> = _savedRoutes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        Log.i("RoutesViewModel", "ViewModel initialized")
        loadSavedRoutes()
    }

    fun loadSavedRoutes() {
        Log.i("RoutesViewModel", "Loading saved routes...")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                routeRepository.getSavedRoutes().collect { routes ->
                    Log.i("RoutesViewModel", "Routes loaded successfully: ${routes.size} routes")
                    _savedRoutes.value = routes
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("RoutesViewModel", "Error loading routes", e)
                _error.value = "Не удалось загрузить маршруты: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun deleteRoute(routeId: String) {
        Log.i("RoutesViewModel", "Deleting route: $routeId")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                routeRepository.deleteRoute(routeId)
                Log.i("RoutesViewModel", "Route deleted successfully: $routeId")
                loadSavedRoutes()
            } catch (e: Exception) {
                Log.e("RoutesViewModel", "Error deleting route", e)
                _error.value = "Не удалось удалить маршрут: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        Log.d("RoutesViewModel", "Clearing error")
        _error.value = null
    }

    override fun onCleared() {
        Log.d("RoutesViewModel", "ViewModel cleared")
        super.onCleared()
    }
}