package com.motoclubgps.ui.screen

import android.Manifest
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.motoclubgps.data.model.UserLocation
import com.motoclubgps.ui.AppState
import com.motoclubgps.ui.AppViewModel
import com.motoclubgps.ui.theme.AppGreen
import com.motoclubgps.ui.theme.AppOrange
import com.motoclubgps.ui.theme.AppRed
import com.motoclubgps.ui.theme.AppSurface
import com.motoclubgps.ui.theme.AppTextSecondary
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs

@Composable
fun MapScreen(
    viewModel: AppViewModel,
    tripId: String,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.startSharing(tripId)
    }

    LaunchedEffect(tripId) {
        viewModel.observeTrip(tripId)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 300.dp),
            ) {
                RideDrawerContent(
                    state = state,
                    onClose = { scope.launch { drawerState.close() } },
                    onShare = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                    onHelp = { showHelpDialog = true },
                    onCancelHelp = viewModel::clearHelp,
                    onLeave = { showLeaveDialog = true },
                    onFinish = { showFinishDialog = true },
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
        ) {
            OpenStreetMapView(
                state = state,
                modifier = Modifier.fillMaxSize(),
            )
            MapTopBar(
                name = state.activeTrip?.name.orEmpty(),
                code = state.activeTrip?.code.orEmpty(),
                onBack = onBack,
            )
            MapQuickActions(
                isRequestingHelp = state.isRequestingHelp,
                onCenter = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
                onParticipants = { scope.launch { drawerState.open() } },
                onSos = { showHelpDialog = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
            )
            TripStatusPanel(
                state = state,
                onOpenDetails = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
            )
            EmergencyBanner(
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 142.dp),
            )
        }
    }

    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false },
            onConfirm = { message ->
                viewModel.requestHelp(message)
                showHelpDialog = false
            },
        )
    }

    if (showLeaveDialog) {
        ConfirmActionDialog(
            title = "Salir del viaje",
            message = "Vas a dejar de compartir tu ubicacion y volver al inicio. El viaje sigue activo para los demas.",
            confirmText = "Salir",
            onDismiss = { showLeaveDialog = false },
            onConfirm = {
                showLeaveDialog = false
                viewModel.leaveTrip(onBack)
            },
        )
    }

    if (showFinishDialog) {
        ConfirmActionDialog(
            title = "Finalizar viaje",
            message = "Vas a cerrar el viaje completo. Los demas ya no podran unirse con este codigo.",
            confirmText = "Finalizar",
            onDismiss = { showFinishDialog = false },
            onConfirm = {
                showFinishDialog = false
                viewModel.finishTrip(onBack)
            },
        )
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Aviso") },
            text = { Text(state.error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text("Aceptar")
                }
            },
        )
    }
}

@Composable
private fun MapTopBar(
    name: String,
    code: String,
    onBack: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        color = AppSurface.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    name.ifBlank { "Viaje en curso" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("Código: $code", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
            }
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar código", tint = AppOrange)
            }
        }
    }
}

@Composable
private fun MapQuickActions(
    isRequestingHelp: Boolean,
    onCenter: () -> Unit,
    onParticipants: () -> Unit,
    onSos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onCenter,
            modifier = Modifier.size(56.dp),
            containerColor = AppSurface,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Centrar y compartir mi ubicación")
        }
        Spacer(Modifier.height(12.dp))
        FloatingActionButton(
            onClick = onParticipants,
            modifier = Modifier.size(56.dp),
            containerColor = AppSurface,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
        ) {
            Icon(Icons.Default.Groups, contentDescription = "Ver participantes")
        }
        Spacer(Modifier.height(12.dp))
        FloatingActionButton(
            onClick = onSos,
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            containerColor = AppRed,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(10.dp),
        ) {
            Text(
                if (isRequestingHelp) "SOS!" else "SOS",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TripStatusPanel(
    state: AppState,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppSurface.copy(alpha = 0.97f),
        shadowElevation = 10.dp,
        onClick = onOpenDetails,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            if (state.isSharingLocation) AppGreen else AppTextSecondary,
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isSharingLocation) {
                        "Ubicación compartida"
                    } else {
                        "Ubicación sin compartir"
                    },
                    modifier = Modifier.weight(1f),
                    color = if (state.isSharingLocation) AppGreen else AppTextSecondary,
                    fontWeight = FontWeight.Medium,
                )
                Icon(Icons.Default.Groups, contentDescription = null, tint = AppOrange)
                Spacer(Modifier.width(5.dp))
                Text("${state.allRiders.size}")
            }
            if (state.pendingLocationCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${state.pendingLocationCount} ubicaciones pendientes de envío",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppOrange,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tocá para ver compañeros y opciones del viaje",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextSecondary,
            )
        }
    }
}

@Composable
private fun EmergencyBanner(
    state: AppState,
    modifier: Modifier = Modifier,
) {
    val activeHelp = state.allRiders.firstOrNull { it.needsHelp }
    if (activeHelp != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF4A1719),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Alerta de emergencia", tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Ayuda solicitada: ${activeHelp.displayName.ifBlank { "Motociclista" }}",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(activeHelp.helpMessage.ifBlank { "Necesita asistencia en ruta" }, color = AppTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun RideDrawerContent(
    state: AppState,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onHelp: () -> Unit,
    onCancelHelp: () -> Unit,
    onLeave: () -> Unit,
    onFinish: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val canFinishTrip = state.activeTrip?.ownerId == state.userId
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver al mapa")
            }
            Column(Modifier.weight(1f)) {
                Text("Control de ruta", style = MaterialTheme.typography.titleMedium)
                Text("${state.allRiders.size} motoqueros visibles")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar menu")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(state.activeTrip?.name.orEmpty().ifBlank { "Viaje en curso" }, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(state.activeTrip?.description.orEmpty().ifBlank { "Sin descripcion" })
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Codigo", style = MaterialTheme.typography.labelLarge)
                Text(state.activeTrip?.code.orEmpty().ifBlank { "-" })
            }
            IconButton(onClick = { clipboard.setText(AnnotatedString(state.activeTrip?.code.orEmpty())) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar codigo")
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        ElevatedButton(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (state.isSharingLocation) "Ubicacion activa" else "Compartir mi ubicacion")
        }
        if (state.pendingLocationCount > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.pendingLocationCount} ubicaciones pendientes de envio",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF92400E),
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Report, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Ayuda")
        }
        if (state.isRequestingHelp) {
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onCancelHelp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Shield, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Marcar que estoy bien")
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        Text("Companeros", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (state.allRiders.isEmpty()) {
            Text("Todavia no hay ubicaciones compartidas.")
            Spacer(Modifier.height(8.dp))
        }
        state.allRiders.forEach { rider ->
            RiderRow(rider)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Salir del viaje")
        }
        if (canFinishTrip) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.StopCircle, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Finalizar viaje")
            }
        }
    }
}

@Composable
private fun RiderRow(rider: UserLocation) {
    val isStale = rider.isStale()
    val color = when {
        rider.needsHelp -> Color(0xFFE11D48)
        isStale -> Color(0xFF6B7280)
        else -> rider.markerColor()
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            rider.needsHelp -> Color(0xFFFFF1F2)
            isStale -> Color(0xFFF3F4F6)
            else -> Color(0xFFF0FDFA)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (rider.needsHelp) Icons.Default.Warning else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(rider.displayName.ifBlank { "Motociclista" }, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        rider.needsHelp -> rider.helpMessage.ifBlank { "Necesita ayuda" }
                        isStale -> "Sin actualizar - ${rider.lastSeenText()}"
                        else -> "En ruta - ${rider.lastSeenText()}"
                    },
                )
            }
            Icon(Icons.Default.Groups, contentDescription = null, tint = color)
        }
    }
}

@Composable
private fun HelpDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var message by remember { mutableStateOf("Necesito ayuda en ruta") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pedir ayuda") },
        text = {
            Column {
                Text("Los companeros veran tu ubicacion marcada como emergencia.")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Mensaje") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(message) }) {
                Text("Enviar alerta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun OpenStreetMapView(
    state: AppState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val defaultPosition = remember { GeoPoint(-24.1858, -65.2995) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(it).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(13.0)
                controller.setCenter(defaultPosition)
            }
        },
        update = { mapView ->
            val markerOverlays = mapView.overlays.filterIsInstance<Marker>()
            mapView.overlays.removeAll(markerOverlays)
            val routeOverlays = mapView.overlays.filterIsInstance<Polyline>()
            mapView.overlays.removeAll(routeOverlays)

            state.routePoints
                .groupBy { it.userId }
                .values
                .filter { it.size >= 2 }
                .forEach { points ->
                    val firstPoint = points.first()
                    val routeColor = firstPoint.markerColor().toAndroidColor()
                    mapView.overlays.add(
                        Polyline().apply {
                            setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
                            outlinePaint.color = routeColor
                            outlinePaint.strokeWidth = 7f
                            outlinePaint.alpha = 190
                        },
                    )
                }

            state.allRiders.forEach { location ->
                val isStale = location.isStale()
                val markerColor = when {
                    location.needsHelp -> Color(0xFFE11D48)
                    isStale -> Color(0xFF6B7280)
                    else -> location.markerColor()
                }
                val label = location.displayName.ifBlank { "Motociclista" }
                mapView.addMarker(
                    position = GeoPoint(location.latitude, location.longitude),
                    title = label,
                    description = if (location.needsHelp) {
                        location.helpMessage.ifBlank { "Necesita ayuda" }
                    } else if (isStale) {
                        "Sin actualizar - ${location.lastSeenText()}"
                    } else {
                        "En ruta - ${location.lastSeenText()}"
                    },
                    icon = createRiderMarkerIcon(
                        resources = context.resources,
                        label = label,
                        color = markerColor,
                        muted = isStale,
                    ),
                )
            }

            state.myLocation?.let { location ->
                mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))
            }
            mapView.invalidate()
        },
    )
}

private fun MapView.addMarker(
    position: GeoPoint,
    title: String,
    description: String,
    icon: BitmapDrawable,
) {
    overlays.add(
        Marker(this).apply {
            this.position = position
            this.title = title
            this.snippet = description
            this.icon = icon
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        },
    )
}

private fun UserLocation.markerColor(): Color {
    val palette = listOf(
        Color(0xFF0F766E),
        Color(0xFF2563EB),
        Color(0xFF7C3AED),
        Color(0xFFDB2777),
        Color(0xFFEA580C),
        Color(0xFF16A34A),
        Color(0xFF0891B2),
        Color(0xFF9333EA),
    )
    val key = userId.ifBlank { displayName }
    val index = abs(key.hashCode()) % palette.size
    return palette[index]
}

private fun UserLocation.isStale(now: Long = System.currentTimeMillis()): Boolean {
    return now - updatedAt > STALE_LOCATION_MS
}

private fun UserLocation.lastSeenText(now: Long = System.currentTimeMillis()): String {
    val elapsedSeconds = ((now - updatedAt).coerceAtLeast(0L) / 1000L).toInt()
    return when {
        elapsedSeconds < 10 -> "recien"
        elapsedSeconds < 60 -> "hace ${elapsedSeconds}s"
        elapsedSeconds < 3600 -> "hace ${elapsedSeconds / 60} min"
        else -> "hace ${elapsedSeconds / 3600} h"
    }
}

private fun createRiderMarkerIcon(
    resources: Resources,
    label: String,
    color: Color,
    muted: Boolean,
): BitmapDrawable {
    val density = resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        textSize = 13f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val labelText = label.take(16)
    val textBounds = Rect()
    textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)

    val horizontalPadding = (10f * density).toInt()
    val labelHeight = (26f * density).toInt()
    val pinSize = (28f * density).toInt()
    val gap = (3f * density).toInt()
    val width = (textBounds.width() + horizontalPadding * 2).coerceAtLeast((42f * density).toInt())
    val height = labelHeight + gap + pinSize + (8f * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val markerColor = color.toAndroidColor()
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (muted) AndroidColor.rgb(75, 85, 99) else markerColor
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.argb(60, 0, 0, 0)
    }

    val labelRadius = 7f * density
    canvas.drawRoundRect(
        1f * density,
        1f * density,
        width - 1f * density,
        labelHeight.toFloat(),
        labelRadius,
        labelRadius,
        shadowPaint,
    )
    canvas.drawRoundRect(
        0f,
        0f,
        width.toFloat(),
        (labelHeight - 1).toFloat(),
        labelRadius,
        labelRadius,
        backgroundPaint,
    )
    canvas.drawText(
        labelText,
        ((width - textBounds.width()) / 2f) - textBounds.left,
        (labelHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f),
        textPaint,
    )

    val centerX = width / 2f
    val pinTop = labelHeight + gap.toFloat()
    val circleRadius = pinSize * 0.38f
    canvas.drawCircle(centerX + density, pinTop + circleRadius + density, circleRadius, shadowPaint)
    canvas.drawCircle(centerX, pinTop + circleRadius, circleRadius, backgroundPaint)
    val pointer = Path().apply {
        moveTo(centerX - 7f * density, pinTop + circleRadius + 8f * density)
        lineTo(centerX + 7f * density, pinTop + circleRadius + 8f * density)
        lineTo(centerX, height.toFloat() - 1f * density)
        close()
    }
    canvas.drawPath(pointer, backgroundPaint)

    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.WHITE }
    canvas.drawCircle(centerX, pinTop + circleRadius, 5f * density, innerPaint)

    return BitmapDrawable(resources, bitmap)
}

private fun Color.toAndroidColor(): Int {
    return AndroidColor.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )
}

private const val STALE_LOCATION_MS = 60_000L
