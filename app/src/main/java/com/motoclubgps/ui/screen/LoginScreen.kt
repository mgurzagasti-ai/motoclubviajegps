package com.motoclubgps.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.motoclubgps.ui.AppViewModel
import com.motoclubgps.ui.theme.AppOrange
import com.motoclubgps.ui.theme.AppTextSecondary

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    onLoggedIn: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordRecovery by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }
    var recoverySent by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = if (isRegisterMode) "Crear cuenta" else "Iniciar sesión",
        isLoading = state.isLoading,
        error = state.error,
        onClearError = viewModel::clearError,
        onBack = onBack,
    ) { modifier ->
        Text(
            "MOTOCLUB JUJUY",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = AppOrange,
        )
        Text(
            if (isRegisterMode) "Unite al grupo y empezá a rodar." else "Conectados en cada ruta.",
            color = AppTextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        AppCard {
            if (isRegisterMode) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre",
                    leadingIcon = Icons.Default.Person,
                )
                Spacer(Modifier.height(12.dp))
            }
            AppTextField(
            value = email,
            onValueChange = { email = it },
                label = "Email",
                leadingIcon = Icons.Default.Email,
            singleLine = true,
        )
            Spacer(Modifier.height(12.dp))
            AppTextField(
            value = password,
            onValueChange = { password = it },
                label = "Contraseña",
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
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
            Spacer(Modifier.height(20.dp))
            if (isRegisterMode) {
                PrimaryButton(
                    text = "Registrarme",
                    enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6,
                    onClick = { viewModel.register(email, password, name, onLoggedIn) },
                )
            } else {
                PrimaryButton(
                    text = "Ingresar",
                    enabled = email.isNotBlank() && password.length >= 6,
                    onClick = { viewModel.login(email, password, onLoggedIn) },
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        recoveryEmail = email
                        recoverySent = false
                        showPasswordRecovery = true
                    },
                ) {
                    Text("Olvidé mi contraseña")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SecondaryButton(
            text = if (isRegisterMode) "Ya tengo cuenta: iniciar sesión" else "Crear una cuenta",
            onClick = { isRegisterMode = !isRegisterMode },
        )
    }

    if (showPasswordRecovery) {
        AlertDialog(
            onDismissRequest = { showPasswordRecovery = false },
            title = {
                Text(if (recoverySent) "Revisá tu email" else "Recuperar contraseña")
            },
            text = {
                if (recoverySent) {
                    Text(
                        "Te enviamos un enlace para crear una contraseña nueva. Revisá también la carpeta de correo no deseado.",
                    )
                } else {
                    androidx.compose.foundation.layout.Column {
                        Text("Ingresá el email asociado a tu cuenta.")
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = recoveryEmail,
                            onValueChange = { recoveryEmail = it },
                            label = "Email",
                            leadingIcon = Icons.Default.Email,
                            singleLine = true,
                        )
                    }
                }
            },
            confirmButton = {
                if (recoverySent) {
                    TextButton(onClick = { showPasswordRecovery = false }) {
                        Text("Aceptar")
                    }
                } else {
                    TextButton(
                        enabled = recoveryEmail.isNotBlank() && !state.isLoading,
                        onClick = {
                            viewModel.requestPasswordReset(recoveryEmail) {
                                recoverySent = true
                            }
                        },
                    ) {
                        Text("Enviar enlace")
                    }
                }
            },
            dismissButton = {
                if (!recoverySent) {
                    TextButton(onClick = { showPasswordRecovery = false }) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }
}
