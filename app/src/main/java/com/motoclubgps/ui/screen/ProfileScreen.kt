package com.motoclubgps.ui.screen

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motoclubgps.ui.AppViewModel
import com.motoclubgps.ui.theme.AppGreen
import com.motoclubgps.ui.theme.AppOrange
import com.motoclubgps.ui.theme.AppTextSecondary
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val appVersion = remember {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var name by remember(state.displayName) { mutableStateOf(state.displayName) }
    var email by remember(state.email) { mutableStateOf(state.email) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) selectedAvatarUri = uri
    }

    LaunchedEffect(selectedAvatarUri, state.avatarUrl) {
        avatarBitmap = withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = if (selectedAvatarUri != null) {
                    context.contentResolver.openInputStream(selectedAvatarUri!!)?.use(BitmapFactory::decodeStream)
                } else if (state.avatarUrl.isNotBlank()) {
                    URL(state.avatarUrl).openStream().use(BitmapFactory::decodeStream)
                } else {
                    null
                }
                bitmap?.asImageBitmap()
            }.getOrNull()
        }
    }

    ScreenScaffold(
        title = if (isEditing) "Editar perfil" else "Perfil",
        isLoading = state.isLoading,
        error = state.error,
        onClearError = viewModel::clearError,
        onBack = onBack,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(AppOrange.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap!!,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(104.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Avatar de perfil",
                        tint = AppOrange,
                        modifier = Modifier.size(58.dp),
                    )
                }
            }
            if (isEditing) {
                IconButton(
                    onClick = { avatarPicker.launch("image/*") },
                    modifier = Modifier
                        .size(40.dp)
                        .background(AppOrange, CircleShape),
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Seleccionar foto de perfil",
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (isEditing) {
            AppCard {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre",
                    leadingIcon = Icons.Default.Person,
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = Icons.Default.Email,
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Si cambiás el email, Supabase puede solicitar confirmación en la nueva dirección.",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = "Guardar cambios",
                    enabled = name.length >= 2 && email.isNotBlank() && !state.isLoading,
                    leadingIcon = Icons.Default.Save,
                ) {
                    scope.launch {
                        val avatarBytesAndType = withContext(Dispatchers.IO) {
                            selectedAvatarUri?.let { uri ->
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                bytes to context.contentResolver.getType(uri)
                            }
                        }
                        viewModel.updateProfile(
                            displayName = name,
                            email = email,
                            avatarBytes = avatarBytesAndType?.first,
                            avatarMimeType = avatarBytesAndType?.second,
                        ) {
                            selectedAvatarUri = null
                            savedMessage = "Perfil actualizado"
                            isEditing = false
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Cancelar") {
                    name = state.displayName
                    email = state.email
                    selectedAvatarUri = null
                    isEditing = false
                }
            }
        } else {
            Text(
                state.displayName.ifBlank { "Sin nombre" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(state.email.ifBlank { "Sin email" }, color = AppTextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = AppGreen,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Conectado", color = AppGreen, style = MaterialTheme.typography.labelLarge)
            }
            if (savedMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(savedMessage.orEmpty(), color = AppGreen)
            }
            Spacer(Modifier.height(24.dp))
            AppCard {
                SectionTitle("Información de la cuenta")
                Spacer(Modifier.height(16.dp))
                ProfileInfoRow(
                    icon = Icons.Default.Person,
                    label = "Nombre",
                    value = state.displayName.ifBlank { "Sin nombre" },
                )
                Spacer(Modifier.height(16.dp))
                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = state.email.ifBlank { "Sin email" },
                )
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Editar perfil", leadingIcon = Icons.Default.Edit) {
                savedMessage = null
                isEditing = true
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Cerrar sesión", leadingIcon = Icons.Default.Logout) {
                viewModel.logout()
                onBack()
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "MotoClub GPS · versión $appVersion",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AppOrange)
        Spacer(Modifier.size(12.dp))
        androidx.compose.foundation.layout.Column {
            Text(label, color = AppTextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(value)
        }
    }
}
