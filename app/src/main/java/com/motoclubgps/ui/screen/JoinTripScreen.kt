package com.motoclubgps.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motoclubgps.ui.AppViewModel
import com.motoclubgps.ui.theme.AppOrange
import com.motoclubgps.ui.theme.AppTextSecondary

@Composable
fun JoinTripScreen(
    viewModel: AppViewModel,
    onTripReady: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var code by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Unirse a viaje",
        isLoading = state.isLoading,
        error = state.error,
        onClearError = viewModel::clearError,
        onBack = onBack,
    ) { modifier ->
        Text(
            "Rodá con tu grupo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Ingresá el código que te compartió quien creó el viaje.",
            color = AppTextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        AppCard {
            AppTextField(
                value = code,
                onValueChange = {
                    code = it
                        .uppercase()
                        .filter { character -> character.isLetterOrDigit() }
                        .take(8)
                },
                label = "Código del viaje",
                leadingIcon = Icons.Default.ConfirmationNumber,
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${code.length}/8 caracteres",
                style = MaterialTheme.typography.labelMedium,
                color = if (code.isNotBlank()) AppOrange else AppTextSecondary,
            )
            Spacer(Modifier.height(22.dp))
            PrimaryButton(
                text = "Unirme al viaje",
                enabled = code.isNotBlank() && state.isLoggedIn,
                leadingIcon = Icons.Default.Groups,
                onClick = { viewModel.joinTrip(code, onTripReady) },
            )
        }
    }
}
