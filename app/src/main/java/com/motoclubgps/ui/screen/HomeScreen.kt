package com.motoclubgps.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motoclubgps.R
import com.motoclubgps.ui.AppViewModel
import com.motoclubgps.ui.theme.AppGreen
import com.motoclubgps.ui.theme.AppOrange
import com.motoclubgps.ui.theme.AppTextSecondary

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onLogin: () -> Unit,
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onProfile: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    ScreenScaffold(
        title = "MotoClub GPS",
        isLoading = state.isLoading,
        error = state.error,
        onClearError = viewModel::clearError,
    ) { modifier ->
        HomeLogo()
        Spacer(Modifier.height(18.dp))
        if (!state.isLoggedIn) {
            Text(
                "Conectados en cada ruta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Organizá viajes, compartí tu ubicación y mantené unido al grupo.",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(28.dp))
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(AppOrange.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = AppOrange)
                    }
                    Spacer(Modifier.size(14.dp))
                    Text(
                        "Iniciá sesión para crear o unirte a un viaje.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Ingresar o registrarme", leadingIcon = Icons.Default.Login, onClick = onLogin)
            return@ScreenScaffold
        }

        Text(
            "Hola, ${state.displayName.ifBlank { "motociclista" }}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "¿Listo para una nueva aventura?",
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(22.dp))
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(AppOrange.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = AppOrange)
                }
                Spacer(Modifier.size(14.dp))
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(
                        state.displayName.ifBlank { "Mi perfil" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(state.email, color = AppTextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = AppGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(5.dp))
                        Text("Sesión activa", color = AppGreen, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Crear viaje", leadingIcon = Icons.Default.AddRoad, onClick = onCreateTrip)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Unirse a un viaje", leadingIcon = Icons.Default.Route, onClick = onJoinTrip)
        Spacer(Modifier.height(24.dp))
        SectionTitle("Cuenta")
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Ver perfil", leadingIcon = Icons.Default.Person, onClick = onProfile)
    }
}

@Composable
private fun HomeLogo() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_motoclub),
            contentDescription = "MotoClub Jujuy",
            modifier = Modifier.size(132.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
