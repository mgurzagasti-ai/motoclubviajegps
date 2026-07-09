package com.motoclubgps.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motoclubgps.ui.AppViewModel
import com.motoclubgps.ui.theme.AppTextSecondary

@Composable
fun CreateTripScreen(
    viewModel: AppViewModel,
    onTripReady: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Crear viaje",
        isLoading = state.isLoading,
        error = state.error,
        onClearError = viewModel::clearError,
        onBack = onBack,
    ) { modifier ->
        Text(
            "Prepará la próxima salida",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Definí los datos que verá el grupo.",
            color = AppTextSecondary,
        )
        Spacer(Modifier.height(20.dp))
        AppCard {
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre del viaje",
                leadingIcon = Icons.Default.Route,
            )
            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción",
                leadingIcon = Icons.Default.Description,
            )
            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = code,
                onValueChange = {
                    code = it
                        .uppercase()
                        .filter { character -> character.isLetterOrDigit() }
                        .take(8)
                },
                label = "Código para compartir",
                leadingIcon = Icons.Default.ConfirmationNumber,
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Compartí este código con tus compañeros. Si lo dejás vacío, se generará automáticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextSecondary,
            )
            Spacer(Modifier.height(22.dp))
            PrimaryButton(
                text = "Crear y abrir mapa",
                enabled = name.isNotBlank() && state.isLoggedIn,
                onClick = { viewModel.createTrip(name, description, code, onTripReady) },
            )
        }
    }
}
