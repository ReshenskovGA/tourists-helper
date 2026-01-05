package com.example.touristassistant.ui.places

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.touristassistant.data.models.Place
import com.example.touristassistant.data.models.PlaceType
import kotlinx.coroutines.launch

/**
 * Экран отображения списка мест с поиском и фильтрацией по категориям.
 *
 * @param onBackClick обработчик нажатия кнопки "Назад"
 * @param onPlaceClick обработчик выбора места из списка
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    onBackClick: () -> Unit,
    onPlaceClick: (Place) -> Unit = {}
) {
    val viewModel: PlacesViewModel = hiltViewModel()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val places by viewModel.places.collectAsState()

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Места") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Поиск мест") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                }
            )

            CategoryFilter(
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::updateSelectedCategory
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(places) { place ->
                    PlaceCard(
                        place = place,
                        onFavoriteClick = {
                            scope.launch {
                                viewModel.toggleFavorite(place.id)
                            }
                        },
                        onClick = { onPlaceClick(place) }
                    )
                }
            }
        }
    }
}

/**
 * Компонент для фильтрации мест по категориям.
 *
 * @param selectedCategory выбранная категория
 * @param onCategorySelected обработчик выбора категории
 */

@Composable
fun CategoryFilter(
    selectedCategory: PlaceType?,
    onCategorySelected: (PlaceType?) -> Unit
) {
    val categories = listOf(
        null to "Все",
        PlaceType.TOURIST_ATTRACTION to "Достопримечательности",
        PlaceType.RESTAURANT to "Рестораны",
        PlaceType.CAFE to "Кафе",
        PlaceType.HOTEL to "Отели",
        PlaceType.SHOPPING to "Магазины"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (type, label) ->
            CategoryChip(
                label = label,
                icon = when (type) {
                    PlaceType.TOURIST_ATTRACTION -> Icons.Default.LocationOn
                    PlaceType.RESTAURANT -> Icons.Default.Restaurant
                    PlaceType.CAFE -> Icons.Default.Coffee
                    PlaceType.HOTEL -> Icons.Default.Hotel
                    PlaceType.SHOPPING -> Icons.Default.ShoppingCart
                    else -> Icons.Default.AllInclusive
                },
                isSelected = selectedCategory == type,
                onClick = { onCategorySelected(type) }
            )
        }
    }
}

/**
 * Чип категории для фильтрации.
 *
 * @param label текст метки
 * @param icon иконка категории
 * @param isSelected флаг выбранности
 * @param onClick обработчик клика
 */

@Composable
fun CategoryChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    )
}

/**
 * Карточка места для отображения в списке.
 *
 * @param place место для отображения
 * @param onFavoriteClick обработчик клика по избранному
 * @param onClick обработчик клика по карточке
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceCard(
    place: Place,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (place.type) {
                        PlaceType.TOURIST_ATTRACTION -> Icons.Default.LocationOn
                        PlaceType.RESTAURANT -> Icons.Default.Restaurant
                        PlaceType.CAFE -> Icons.Default.Coffee
                        PlaceType.HOTEL -> Icons.Default.Hotel
                        PlaceType.SHOPPING -> Icons.Default.ShoppingCart
                        PlaceType.TRANSPORT -> Icons.Default.DirectionsBus
                        PlaceType.HOSPITAL -> Icons.Default.LocalHospital
                        PlaceType.PHARMACY -> Icons.Default.MedicalServices
                        PlaceType.BANK -> Icons.Default.AccountBalance
                        PlaceType.TOILET -> Icons.Default.Wc
                        else -> Icons.Default.Place
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (place.type != PlaceType.OTHER) {
                        Text(
                            text = place.type.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (place.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (place.isFavorite) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = place.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = place.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (place.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = place.rating.toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (place.priceLevel > 0) {
                    Text(
                        text = "$".repeat(place.priceLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                place.phone?.let {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                place.website?.let {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}