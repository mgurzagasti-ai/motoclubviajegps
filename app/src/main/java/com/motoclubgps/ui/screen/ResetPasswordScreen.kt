package com.motoclubgps.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motoclubgps.ui.AppViewModel
import com.motoclubgps.ui.theme.AppGreen
import com.motoclubgps.ui.theme.AppTextSecondary

@Composable
fun ResetPasswordScreen(
    recoveryAccessToken: String,
    onComplete: () -> Unit,
    appViewModel: AppViewModel = viewModel(),
) {
    val state by appViewModel.state.collectAsState()
    var password by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordUpdated by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Nueva contraseña",
        isLoading = state.isLoading,
        error = state.error,
        onClearError = appViewModel::clearError,
    ) {
        if (passwordUpdated) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AppGreen,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Contraseña actualizada",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Ya podés ingresar con tu contraseña nueva.",
                color = AppTextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Ir a iniciar sesión", onClick = onComplete)
        } else {
            Text(
                "Elegí una contraseña de al menos 6 caracteres.",
                color = AppTextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            AppCard {
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Nueva contraseña",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) {
                                    "Ocultar contraseña"
                                } else {
                                    "Mostrar contraseña"
                                },
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = repeatedPassword,
                    onValueChange = { repeatedPassword = it },
                    label = "Repetir contraseña",
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (repeatedPassword.isNotEmpty() && repeatedPassword != password) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Las contraseñas no coinciden.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = "Guardar contraseña",
                    enabled = password.length >= 6 &&
                        repeatedPassword == password &&
                        recoveryAccessToken.isNotBlank() &&
                        !state.isLoading,
                ) {
                    appViewModel.updatePassword(recoveryAccessToken, password) {
                        passwordUpdated = true
                    }
                }
            }
        }
    }
}
