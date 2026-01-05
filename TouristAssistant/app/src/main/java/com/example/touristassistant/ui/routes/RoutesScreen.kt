package com.example.touristassistant.ui.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.touristassistant.data.models.Route
import kotlinx.coroutines.launch

/**
 * Экран отображения сохраненных маршрутов с возможностью управления.
 *
 * @param onBackClick обработчик нажатия кнопки "Назад"
 * @param onRouteClick обработчик выбора маршрута из списка, принимает идентификатор маршрута и режим работы
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    onBackClick: () -> Unit,
    onRouteClick: (String, String) -> Unit = { _, _ -> }
) {
    val viewModel: RoutesViewModel = hiltViewModel()
    val savedRoutes by viewModel.savedRoutes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Маршруты") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(
                        onClick = {
                            onRouteClick("", "create")
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Создать маршрут")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            error?.let {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (savedRoutes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Нет сохраненных маршрутов",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Создайте маршрут на карте",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedRoutes) { route ->
                        RouteCard(
                            route = route,
                            onDelete = {
                                scope.launch {
                                    viewModel.deleteRoute(route.id)
                                }
                            },
                            onClick = {
                                onRouteClick(route.id, "view")
                            },
                            onEdit = {
                                onRouteClick(route.id, "edit")
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Карточка маршрута для отображения в списке.
 *
 * @param route маршрут для отображения
 * @param onDelete обработчик удаления
 * @param onClick обработчик клика по карточке
 * @param onEdit обработчик редактирования
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteCard(
    route: Route,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = route.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RouteInfoItem(
                    icon = Icons.Default.DirectionsWalk,
                    text = "${(route.distance / 1000).format(2)} км"
                )

                RouteInfoItem(
                    icon = Icons.Default.Schedule,
                    text = "${route.duration / 60} мин"
                )

                RouteInfoItem(
                    icon = Icons.Default.LocationOn,
                    text = "${route.points.size} точек"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            route.points.take(3).forEachIndexed { index, point ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = when (point.type) {
                            com.example.touristassistant.data.models.RoutePointType.START -> Icons.Default.PlayArrow
                            com.example.touristassistant.data.models.RoutePointType.DESTINATION -> Icons.Default.Flag
                            else -> Icons.Default.LocationOn
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when (point.type) {
                            com.example.touristassistant.data.models.RoutePointType.START -> MaterialTheme.colorScheme.primary
                            com.example.touristassistant.data.models.RoutePointType.DESTINATION -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = point.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }

            if (route.points.size > 3) {
                Text(
                    text = "... и ещё ${route.points.size - 3} точек",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Элемент информации о маршруте с иконкой.
 *
 * @param icon иконка элемента
 * @param text текст информации
 */

@Composable
fun RouteInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Форматирует число Double с заданным количеством знаков после запятой.
 *
 * @param digits количество знаков после запятой
 * @return отформатированная строка
 */

fun Double.format(digits: Int) = "%.${digits}f".format(this)