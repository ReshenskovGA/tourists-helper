package com.example.touristassistant.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.touristassistant.data.models.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Основной экран карты приложения, отображающий места, маршруты и предоставляющий функционал навигации.
 *
 * @param onPlacesClick обработчик клика по кнопке "Места"
 * @param onRoutesClick обработчик клика по кнопке "Маршруты"
 * @param selectedPlaceId идентификатор выбранного места для отображения на карте
 * @param selectedRouteId идентификатор выбранного маршрута для отображения
 * @param routeMode режим работы с маршрутом: "view" - просмотр, "edit" - редактирование, "create" - создание
 * @param onRouteViewComplete обработчик завершения просмотра маршрута
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onPlacesClick: () -> Unit,
    onRoutesClick: () -> Unit,
    selectedPlaceId: String? = null,
    selectedRouteId: String? = null,
    routeMode: String? = null,
    onRouteViewComplete: () -> Unit = {}
) {
    // Инициализация состояний и зависимостей
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MapViewModel = hiltViewModel()
    val places by viewModel.places.collectAsState()
    val selectedPlace by viewModel.selectedPlace.collectAsState()

    Log.d("MapScreen", "Compose function called with params: selectedPlaceId=$selectedPlaceId, selectedRouteId=$selectedRouteId, routeMode=$routeMode")

    // Состояния для управления разрешениями
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    Log.d("MapScreen", "Location permission granted: $hasLocationPermission")

    // Launcher для запроса разрешений с обработкой результата
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            // Обработка результатов запроса разрешений
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            hasLocationPermission = fineLocationGranted || coarseLocationGranted
            Log.i("MapScreen", "Permission request result: FINE=$fineLocationGranted, COARSE=$coarseLocationGranted")
        }
    )

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var routeMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var isRouteMode by remember { mutableStateOf(false) }
    var showRouteDialog by remember { mutableStateOf(false) }
    var calculatedRoute by remember { mutableStateOf<Route?>(null) }
    var selectedPointForRoute by remember { mutableStateOf<GeoPoint?>(null) }

    // Состояния для управления маршрутами
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var isRouteBuildingFinished by remember { mutableStateOf(false) }
    var draggedMarkerIndex by remember { mutableStateOf<Int?>(null) }

    var showNameInputDialog by remember { mutableStateOf(false) }
    var routeNameInput by remember { mutableStateOf("") }

    val routeRepository = viewModel.routeRepository
    var loadedRoute by remember { mutableStateOf<Route?>(null) }
    var isRouteLoading by remember { mutableStateOf(false) }

    val (currentRouteMode, setCurrentRouteMode) = remember {
        mutableStateOf(routeMode)
    }

    LaunchedEffect(routeMode) {
        setCurrentRouteMode(routeMode)
    }

    /**
     * Эффект для загрузки маршрута при изменении selectedRouteId или routeMode.
     * Выполняет асинхронную загрузку маршрута из репозитория и обновляет состояние карты.
     */
    LaunchedEffect(selectedRouteId, routeMode) {
        selectedRouteId?.let { routeId ->
            isRouteLoading = true
            try {
                routeRepository.getAllRoutes().collect { routes ->
                    val route = routes.find { it.id == routeId }
                    route?.let {
                        loadedRoute = it
                        if (routeMode == "view" || routeMode == "edit") {
                            // Преобразование точек маршрута в GeoPoint для отображения на карте
                            val geoPoints = it.points.map { point ->
                                GeoPoint(point.latitude, point.longitude)
                            }
                            routePoints = geoPoints
                            if (routeMode == "edit") {
                                isRouteMode = true
                            }
                            // Центрирование карты на первой точке маршрута
                            mapView?.controller?.setCenter(geoPoints.first())
                            mapView?.controller?.zoomTo(14.0)
                        }
                    }
                    isRouteLoading = false
                }
            } catch (e: Exception) {
                isRouteLoading = false
                Log.e("MapScreen", "Ошибка загрузки маршрута", e)
            }
        }
    }

    /**
     * Функция обновления полилинии маршрута на карте.
     * Удаляет старую полилинию и создает новую на основе текущих точек маршрута.
     */
    fun updateRoutePolyline() {
        mapView?.let { view ->
            // Удаление старой полилинии
            routePolyline?.let { view.overlays.remove(it) }

            // Создание новой полилинии
            val polyline = Polyline().apply {
                color = Color.BLUE
                width = 10.0f
                isGeodesic = true // Использование геодезической линии (учитывает кривизну Земли)
                setPoints(ArrayList(routePoints))
            }

            view.overlays.add(polyline)
            routePolyline = polyline
            view.invalidate() // Принудительная перерисовка карты
        }
    }

    /**
     * Функция обновления маркеров точек маршрута.
     * Создает маркеры с номерами, цветовой индикацией и обработкой перетаскивания.
     */
    fun updateRouteMarkers() {
        mapView?.let { view ->
            // Удаление старых маркеров
            routeMarkers.forEach { marker ->
                view.overlays.remove(marker)
            }

            val markers = mutableListOf<Marker>()
            routePoints.forEachIndexed { index, point ->
                val marker = Marker(view).apply {
                    position = point
                    title = when (index) {
                        0 -> "Старт"
                        routePoints.size - 1 -> "Финиш"
                        else -> "Точка ${index + 1}"
                    }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    // Определение цвета маркера в зависимости от типа точки
                    var color = when (index) {
                        0 -> Color.GREEN
                        routePoints.size - 1 -> Color.RED
                        else -> Color.BLUE
                    }

                    // Создание кастомной иконки маркера с номером точки
                    val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val paint = Paint().apply {
                        this.color = color
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(24f, 24f, 24f, paint)

                    // Добавление номера точки на маркер
                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val text = (index + 1).toString()
                    canvas.drawText(text, 24f, 30f, textPaint)

                    icon = BitmapDrawable(context.resources, bitmap)

                    isDraggable = true // Включение возможности перетаскивания

                    // Обработчики событий перетаскивания маркера
                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDragStart(marker: Marker) {
                            // Запоминаем индекс перетаскиваемого маркера
                            draggedMarkerIndex = routePoints.indexOfFirst {
                                it.latitude == marker.position.latitude &&
                                        it.longitude == marker.position.longitude
                            }
                        }

                        override fun onMarkerDrag(marker: Marker) {
                            // Обновление полилинии при перетаскивании
                            updateRoutePolyline()
                        }

                        override fun onMarkerDragEnd(marker: Marker) {
                            // Обновление координат точки маршрута после перетаскивания
                            draggedMarkerIndex?.let { index ->
                                if (index in routePoints.indices) {
                                    routePoints = routePoints.toMutableList().apply {
                                        set(index, marker.position)
                                    }
                                    updateRoutePolyline()
                                }
                            }
                            draggedMarkerIndex = null
                        }
                    })
                }
                view.overlays.add(marker)
                markers.add(marker)
            }

            routeMarkers = markers
            view.invalidate()
        }
    }

    /**
     * Функция добавления новой точки в маршрут.
     * Вызывается при клике на карту в режиме построения маршрута.
     */
    fun addRoutePoint(point: GeoPoint) {
        Log.i("MapScreen", "Adding route point: (${point.latitude}, ${point.longitude})")
        routePoints = routePoints + point
        Log.d("MapScreen", "Route points count: ${routePoints.size}")

        updateRouteMarkers()

        // Если точек достаточно для построения линии, обновляем полилинию
        if (routePoints.size >= 2) {
            updateRoutePolyline()
        }
    }

    fun addNewRoutePoint() {
        Log.d("MapScreen", "Adding new route point from map center")
        mapView?.let { view ->
            val center = view.mapCenter
            addRoutePoint(center as GeoPoint)
        }
    }

    /**
     * Функция расчета общего расстояния маршрута.
     * Суммирует расстояния между последовательными точками маршрута.
     */
    fun calculateRouteDistance(points: List<GeoPoint>): Double {
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            // Использование метода distanceToAsDouble из OSMDroid для расчета расстояния
            totalDistance += points[i].distanceToAsDouble(points[i + 1])
        }
        return totalDistance
    }

    /**
     * Функция расчета примерного времени прохождения маршрута.
     * Использует среднюю скорость пешехода (5 км/ч ≈ 1.3889 м/с).
     */
    fun calculateEstimatedDuration(distance: Double): Int {
        val averageSpeed = 1.3889 // м/с (5 км/ч)
        return (distance / averageSpeed).toInt()
    }

    fun generateSmartRouteName(points: List<RoutePoint>): String {
        if (points.isEmpty()) return "Новый маршрут"

        return if (points.size == 2) {
            "От ${points[0].name} до ${points[1].name}"
        } else {
            "Маршрут через ${points.size} точек"
        }
    }

    /**
     * Функция завершения построения маршрута.
     * Создает объект Route на основе текущих точек и отображает диалог с информацией.
     */
    fun finishRouteBuilding(isEditing: Boolean = false) {
        Log.i("MapScreen", "Finishing route building. Points: ${routePoints.size}, isEditing=$isEditing")
        if (routePoints.size >= 2) {
            scope.launch {
                try {
                    // Преобразование GeoPoint в RoutePoint с определением типов точек
                    val points = routePoints.mapIndexed { index, geoPoint ->
                        com.example.touristassistant.data.models.RoutePoint(
                            latitude = geoPoint.latitude,
                            longitude = geoPoint.longitude,
                            name = when (index) {
                                0 -> "Старт"
                                routePoints.size - 1 -> "Финиш"
                                else -> "Точка ${index + 1}"
                            },
                            type = when (index) {
                                0 -> RoutePointType.START
                                routePoints.size - 1 -> RoutePointType.DESTINATION
                                else -> RoutePointType.WAYPOINT
                            }
                        )
                    }

                    val distance = calculateRouteDistance(routePoints)
                    val duration = calculateEstimatedDuration(distance)

                    // Создание объекта маршрута
                    val routeName = if (isEditing && loadedRoute != null) {
                        loadedRoute!!.name
                    } else {
                        generateSmartRouteName(points)
                    }

                    val route = Route(
                        id = if (isEditing && loadedRoute != null) loadedRoute!!.id else System.currentTimeMillis().toString(),
                        name = routeName,
                        description = if (isEditing && loadedRoute != null) loadedRoute!!.description else "Пользовательский маршрут",
                        points = points,
                        distance = distance,
                        duration = duration,
                        polyline = routePoints.joinToString(";") { "${it.latitude},${it.longitude}" },
                        isSaved = true
                    )

                    calculatedRoute = route
                    showRouteDialog = true
                    isRouteBuildingFinished = true
                    Log.i("MapScreen", "Route built successfully: $routeName, distance=${distance}m, duration=${duration}s")
                } catch (e: Exception) {
                    Log.e("MapScreen", "Error building route", e)
                }
            }
        } else {
            Log.w("MapScreen", "Cannot finish route: need at least 2 points, current: ${routePoints.size}")
        }
    }

    fun resetRoute() {
        Log.i("MapScreen", "Resetting route")
        routePoints = emptyList()
        isRouteBuildingFinished = false
        draggedMarkerIndex = null
        routePolyline?.let { mapView?.overlays?.remove(it) }
        routePolyline = null
        routeMarkers.forEach { mapView?.overlays?.remove(it) }
        routeMarkers = emptyList()
        mapView?.invalidate()
        Log.d("MapScreen", "Route reset complete")
    }


    fun removeLastRoutePoint() {
        if (routePoints.isNotEmpty()) {
            routePoints = routePoints.dropLast(1)

            updateRouteMarkers()
            if (routePoints.size >= 2) {
                updateRoutePolyline()
            } else {
                routePolyline?.let { mapView?.overlays?.remove(it) }
                routePolyline = null
            }
        }
    }

    LaunchedEffect(places, mapView) {
        mapView?.let { view ->
            val overlaysToRemove = view.overlays.filter {
                it is Marker || it is Polyline
            }
            overlaysToRemove.forEach { view.overlays.remove(it) }

            places.forEach { place ->
                val geoPoint = GeoPoint(place.latitude, place.longitude)
                val marker = Marker(view).apply {
                    position = geoPoint
                    title = place.name
                    snippet = place.description
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    icon = createCustomMarker(context, place)

                    setOnMarkerClickListener { marker, _ ->
                        if (isRouteMode && !isRouteBuildingFinished) {
                            addRoutePoint(geoPoint)
                            true
                        } else {
                            viewModel.selectPlace(place)
                            marker.showInfoWindow()
                            true
                        }
                    }
                }
                view.overlays.add(marker)
            }
            view.invalidate()
        }
    }

    /**
     * Эффект для отображения выбранного места на карте.
     * Центрирует карту на выбранном месте и показывает его маркер.
     */
    LaunchedEffect(selectedPlaceId, places, mapView) {
        Log.d("MapScreen", "LaunchedEffect: selectedPlaceId=$selectedPlaceId, places count=${places.size}")
        if (selectedPlaceId != null) {
            val place = places.find { it.id == selectedPlaceId }
            place?.let {
                Log.i("MapScreen", "Found selected place: ${it.name} (ID: ${it.id})")
                val geoPoint = GeoPoint(it.latitude, it.longitude)
                mapView?.controller?.animateTo(geoPoint) // Плавное перемещение к точке
                mapView?.controller?.setZoom(16.0) // Установка приближения
                viewModel.selectPlace(it)
                Log.d("MapScreen", "Map centered on place: ${it.name}")
            } ?: run {
                Log.w("MapScreen", "Selected place not found: $selectedPlaceId")
            }
        }
    }

    /**
     * Эффект для запроса разрешений при первом запуске.
     * Если разрешения нет, показывает системный диалог запроса.
     */
    LaunchedEffect(Unit) {
        Log.d("MapScreen", "Checking location permission on launch")
        if (!hasLocationPermission) {
            Log.i("MapScreen", "Requesting location permissions")
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        currentRouteMode == "view" && loadedRoute != null -> {
                            Log.d("MapScreen", "Displaying route: ${loadedRoute!!.name}")
                            Text(loadedRoute!!.name)
                        }
                        currentRouteMode == "edit" && loadedRoute != null -> {
                            Log.d("MapScreen", "Editing route: ${loadedRoute!!.name}")
                            Text("Редактирование: ${loadedRoute!!.name}")
                        }
                        else -> {
                            Log.v("MapScreen", "Default app title")
                            Text("Помощник туриста")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    if (routeMode == "view" || routeMode == "edit") {
                        IconButton(onClick = onRouteViewComplete) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (currentRouteMode == "view" && loadedRoute != null) {
                        IconButton(
                            onClick = {
                                Log.i("MapScreen", "Edit route button clicked")
                                setCurrentRouteMode("edit")
                                isRouteMode = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                        }
                    }

                    IconButton(onClick = {
                        Log.i("MapScreen", "Places button clicked")
                        onPlacesClick()
                    }) {
                        Icon(Icons.Default.Place, contentDescription = "Места")
                    }
                    IconButton(onClick = {
                        Log.i("MapScreen", "Routes button clicked")
                        onRoutesClick()
                    }) {
                        Icon(Icons.Default.Route, contentDescription = "Маршруты")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        Log.i("MapScreen", "My location button clicked")
                        if (hasLocationPermission) {
                            mapView?.controller?.animateTo(currentLocation ?: GeoPoint(55.7558, 37.6173))
                            Log.d("MapScreen", "Centering map on current location")
                        } else {
                            Log.w("MapScreen", "No location permission, requesting...")
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Мое местоположение")
                }

                FloatingActionButton(
                    onClick = {
                        Log.i("MapScreen", "Route building toggle clicked. Current state: $isRouteMode")
                        if (hasLocationPermission) {
                            isRouteMode = !isRouteMode
                            Log.d("MapScreen", "Route mode changed to: $isRouteMode")
                            if (!isRouteMode) {
                                resetRoute()
                            }
                        } else {
                            Log.w("MapScreen", "No location permission, requesting...")
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = if (isRouteMode) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (isRouteMode) Icons.Default.Done else Icons.Default.Route,
                        contentDescription = "Построить маршрут"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (currentRouteMode == "view") {
                LaunchedEffect(loadedRoute) {
                    loadedRoute?.let { route ->
                        updateRouteMarkers()
                        updateRoutePolyline()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)

                            Configuration.getInstance().userAgentValue = context.packageName
                            controller.setZoom(12.0)
                            controller.setCenter(GeoPoint(55.7558, 37.6173))

                            val compassOverlay = CompassOverlay(
                                context,
                                InternalCompassOrientationProvider(context),
                                this
                            )
                            compassOverlay.enableCompass()
                            overlays.add(compassOverlay)

                            mapView = this
                            setOnClickListener(null)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        if (hasLocationPermission) {
                            view.overlays.removeAll { it is MyLocationNewOverlay }

                            val locationOverlay = MyLocationNewOverlay(
                                GpsMyLocationProvider(context),
                                view
                            )
                            locationOverlay.enableMyLocation()
                            locationOverlay.runOnFirstFix {
                                currentLocation = locationOverlay.myLocation
                            }
                            view.overlays.add(locationOverlay)
                        }
                    }
                )
                loadedRoute?.let { route ->
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
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
                                Text(
                                    text = route.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = route.description,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text("Расстояние:", style = MaterialTheme.typography.bodySmall)
                                    Text("${(route.distance / 1000).format(2)} км")
                                }
                                Column {
                                    Text("Время:", style = MaterialTheme.typography.bodySmall)
                                    Text("${route.duration / 60} мин")
                                }
                                Column {
                                    Text("Точек:", style = MaterialTheme.typography.bodySmall)
                                    Text("${route.points.size}")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    setCurrentRouteMode("edit")
                                    isRouteMode = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Редактировать маршрут")
                            }
                        }
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)

                            Configuration.getInstance().userAgentValue = context.packageName
                            controller.setZoom(12.0)
                            controller.setCenter(GeoPoint(55.7558, 37.6173))

                            val compassOverlay = CompassOverlay(
                                context,
                                InternalCompassOrientationProvider(context),
                                this
                            )
                            compassOverlay.enableCompass()
                            overlays.add(compassOverlay)

                            mapView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.setOnClickListener { event ->
                            if (isRouteMode && !isRouteBuildingFinished) {
                                val projection = view.projection
                                val geoPoint = projection.fromPixels(
                                    event.x.toInt(),
                                    event.y.toInt()
                                ) as? GeoPoint

                                geoPoint?.let { point ->
                                    addRoutePoint(point)
                                }
                                true
                            } else {
                                false
                            }
                        }


                        if (hasLocationPermission) {
                            view.overlays.removeAll { it is MyLocationNewOverlay }

                            val locationOverlay = MyLocationNewOverlay(
                                GpsMyLocationProvider(context),
                                view
                            )
                            locationOverlay.enableMyLocation()
                            locationOverlay.runOnFirstFix {
                                currentLocation = locationOverlay.myLocation
                            }
                            view.overlays.add(locationOverlay)
                        }
                    }
                )
            }

            selectedPlace?.let { place ->
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
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
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.toggleFavorite(place.id)
                                    }
                                }
                            ) {
                                Icon(
                                    if (place.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Избранное",
                                    tint = if (place.isFavorite) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outline
                                )
                            }

                            if (isRouteMode && !isRouteBuildingFinished) {
                                IconButton(
                                    onClick = {
                                        val geoPoint = GeoPoint(place.latitude, place.longitude)
                                        addRoutePoint(geoPoint)
                                    }
                                ) {
                                    Icon(Icons.Default.AddLocation, contentDescription = "Добавить в маршрут")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = place.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = place.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.selectPlace(null)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Закрыть")
                        }
                    }
                }
            }

            if (isRouteMode && !isRouteBuildingFinished) {
                CompactRouteMenu(
                    routePoints = routePoints,
                    isRouteBuildingFinished = isRouteBuildingFinished,
                    onAddPoint = { addNewRoutePoint() },
                    onRemoveLastPoint = { removeLastRoutePoint() },
                    onFinish = { finishRouteBuilding() },
                    onReset = { resetRoute() },
                    onCancel = {
                        resetRoute()
                        isRouteMode = false
                    }
                )
            }

            if (isRouteMode && isRouteBuildingFinished) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Маршрут построен! Сохраните его в диалоге ниже.")
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                resetRoute()
                                isRouteMode = false
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                }
            }

            if (!hasLocationPermission) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Требуется разрешение на местоположение")
                            Text(
                                "Для использования функции \"Мое местоположение\" и построения маршрутов",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            ) {
                                Text("Запросить разрешение")
                            }
                        }
                    }
                }
            }

            if (showRouteDialog && calculatedRoute != null) {
                calculatedRoute?.let { route ->
                    RouteInfoDialog(
                        route = route,
                        onDismiss = {
                            showRouteDialog = false
                            resetRoute()
                            isRouteMode = false
                            if (currentRouteMode == "edit") {
                                onRouteViewComplete()
                            }
                        },
                        onSave = {
                            routeNameInput = route.name
                            showRouteDialog = false
                            showNameInputDialog = true
                        },
                        isEditing = currentRouteMode  == "edit"
                    )
                }
            }
            if (showNameInputDialog && calculatedRoute != null) {
                SaveRouteNameDialog(
                    currentName = routeNameInput,
                    onDismiss = {
                        showNameInputDialog = false
                        showRouteDialog = true
                    },
                    onSave = { newName ->
                        scope.launch {
                            val updatedRoute = calculatedRoute!!.copy(name = newName)

                            if (currentRouteMode == "edit") {
                                routeRepository.updateRoute(updatedRoute)
                            } else {
                                routeRepository.saveRoute(updatedRoute)
                            }

                            showNameInputDialog = false
                            resetRoute()
                            isRouteMode = false

                            if (currentRouteMode == "edit") {
                                setCurrentRouteMode(null)
                                onRouteViewComplete()
                            }
                        }
                    },
                    isEditing = currentRouteMode  == "edit"
                )
            }
        }
    }
}

/**
 * Диалог для отображения информации о маршруте.
 *
 * @param route маршрут для отображения
 * @param onDismiss обработчик закрытия диалога
 * @param onSave обработчик сохранения маршрута
 * @param isEditing флаг редактирования маршрута
 */

@Composable
fun RouteInfoDialog(
    route: Route,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    isEditing: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Сохранение изменений" else "Информация о маршруте")
        },
        text = {
            Column {
                Text(
                    text = "Название маршрута:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = route.description)
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Расстояние:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${(route.distance / 1000).format(2)} км",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column {
                        Text("Время:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${route.duration / 60} мин",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column {
                        Text("Точек:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${route.points.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Точки маршрута:", style = MaterialTheme.typography.bodySmall)
                route.points.forEachIndexed { index, point ->
                    Text(
                        "${index + 1}. ${point.name} (${point.latitude.format(5)}, ${point.longitude.format(5)})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "На следующем шаге вы сможете изменить название маршрута",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(if (isEditing) "Сохранить изменения" else "Далее")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Форматирует число Double с заданным количеством знаков после запятой.
 *
 * @param digits количество знаков после запятой
 * @return отформатированная строка
 */

fun Double.format(digits: Int) = "%.${digits}f".format(this)

/**
 * Создает кастомный маркер для места на карте.
 * Генерирует векторную графику с тенью, цветовой индикацией типа места и иконкой.
 *
 * @param context контекст для доступа к ресурсам
 * @param place место для которого создается маркер
 * @return Drawable с отрисованной иконкой маркера
 */

fun createCustomMarker(context: Context, place: Place): Drawable {
    val width = 96
    val height = 126

    // Создание bitmap для рисования
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Определение цвета маркера в зависимости от типа места
    val markerColor = when (place.type) {
        PlaceType.TOURIST_ATTRACTION -> Color.RED
        PlaceType.RESTAURANT -> Color.parseColor("#FF9800")
        PlaceType.CAFE -> Color.YELLOW
        PlaceType.HOTEL -> Color.BLUE
        PlaceType.SHOPPING -> Color.MAGENTA
        PlaceType.TRANSPORT -> Color.CYAN
        PlaceType.HOSPITAL -> Color.parseColor("#FF4081")
        PlaceType.PHARMACY -> Color.GREEN
        PlaceType.BANK -> Color.parseColor("#795548")
        PlaceType.TOILET -> Color.parseColor("#9E9E9E")
        else -> Color.GRAY
    }

    // Рисование тени маркера (смещенной на 3 пикселя)
    val shadowColor = Color.argb(150, 0, 0, 0) // Полупрозрачный черный
    val shadowPaint = Paint().apply {
        color = shadowColor
        isAntiAlias = true
    }

    val markerPaint = Paint().apply {
        color = markerColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    val borderPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    val centerX = width / 2f
    val bottomY = height - 15f
    val topY = 15f

    canvas.drawCircle(centerX + 3, bottomY + 3, 9f, shadowPaint)
    canvas.drawOval(
        centerX - 22 + 3,
        topY + 3,
        centerX + 22 + 3,
        bottomY - 30 + 3,
        shadowPaint
    )

    // Рисование основного тела маркера (эллипс + треугольник снизу)
    canvas.drawOval(
        centerX - 22,
        topY,
        centerX + 22,
        bottomY - 30,
        markerPaint
    )

    // Рисование треугольного указателя
    val path = android.graphics.Path()
    path.moveTo(centerX - 15, bottomY - 30)
    path.lineTo(centerX, bottomY)
    path.lineTo(centerX + 15, bottomY - 30)
    path.close()
    canvas.drawPath(path, markerPaint)

    canvas.drawOval(
        centerX - 22,
        topY,
        centerX + 22,
        bottomY - 30,
        borderPaint
    )
    canvas.drawPath(path, borderPaint)

    val iconPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // Добавление иконки типа места в центр маркера
    val iconChar = when (place.type) {
        PlaceType.TOURIST_ATTRACTION -> "⛪"
        PlaceType.RESTAURANT -> "🍽️"
        PlaceType.CAFE -> "☕"
        PlaceType.HOTEL -> "🏨"
        PlaceType.SHOPPING -> "🛍️"
        PlaceType.TRANSPORT -> "🚇"
        PlaceType.HOSPITAL -> "🏥"
        PlaceType.PHARMACY -> "💊"
        PlaceType.BANK -> "🏦"
        PlaceType.TOILET -> "🚻"
        else -> "📍"
    }

    canvas.drawText(iconChar, centerX, topY + 45, iconPaint)

    // Добавление звезды для избранных мест
    if (place.isFavorite) {
        val starPaint = Paint().apply {
            color = Color.YELLOW
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("★", centerX + 30, topY + 22, starPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Компактное меню для построения маршрута.
 *
 * @param routePoints список точек маршрута
 * @param isRouteBuildingFinished флаг завершения построения
 * @param onAddPoint обработчик добавления точки
 * @param onRemoveLastPoint обработчик удаления последней точки
 * @param onFinish обработчик завершения построения
 * @param onReset обработчик сброса маршрута
 * @param onCancel обработчик отмены построения
 */

@Composable
fun CompactRouteMenu(
    routePoints: List<GeoPoint>,
    isRouteBuildingFinished: Boolean,
    onAddPoint: () -> Unit,
    onRemoveLastPoint: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${routePoints.size} точек",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onAddPoint,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.AddLocation,
                        contentDescription = "Добавить точку"
                    )
                }

                IconButton(
                    onClick = onRemoveLastPoint,
                    enabled = routePoints.isNotEmpty(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Удалить последнюю точку"
                    )
                }

                IconButton(
                    onClick = onFinish,
                    enabled = routePoints.size >= 2,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Завершить построение"
                    )
                }

                IconButton(
                    onClick = onReset,
                    enabled = routePoints.isNotEmpty(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Сбросить маршрут"
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Выйти из режима построения"
                    )
                }
            }
        }
    }
}

/**
 * Диалог для ввода названия маршрута.
 *
 * @param currentName текущее название маршрута
 * @param onDismiss обработчик закрытия диалога
 * @param onSave обработчик сохранения названия
 * @param isEditing флаг редактирования маршрута
 */

@Composable
fun SaveRouteNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    isEditing: Boolean = false
) {
    var routeName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Изменить название маршрута" else "Название маршрута")
        },
        text = {
            Column {
                Text(
                    if (isEditing) "Введите новое название для маршрута"
                    else "Дайте название вашему маршруту",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Название маршрута") },
                    placeholder = { Text("Например: Прогулка по центру") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(routeName) },
                enabled = routeName.isNotBlank()
            ) {
                Text(if (isEditing) "Сохранить изменения" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}