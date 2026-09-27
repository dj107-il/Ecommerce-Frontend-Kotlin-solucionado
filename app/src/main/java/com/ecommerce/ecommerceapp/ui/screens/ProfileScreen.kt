package com.ecommerce.ecommerceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ecommerce.ecommerceapp.ui.components.LocationDropdown
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.ecommerceapp.ui.components.LoadingButton
import com.ecommerce.ecommerceapp.utils.SessionManager
import com.ecommerce.ecommerceapp.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    onLoginRequired: () -> Unit = onNavigateBack,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel(sessionManager) }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // Manejar mensajes
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                actions = {
                    IconButton(onClick = viewModel::loadProfile, enabled = !viewModel.isUpdatingProfile && !viewModel.isLoadingProfile && !viewModel.isSessionExpired) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar perfil")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.isSessionExpired) {
                Text("Tu sesión venció o no es válida.")
                Button(onClick = onLoginRequired) { Text("Iniciar sesión") }
            }
            if (viewModel.isLoadingProfile) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            // Información del perfil
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Información Personal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Campo Nombre
                    OutlinedTextField(
                        value = viewModel.userName,
                        onValueChange = viewModel::updateUserName,
                        label = { Text("Nombre de Usuario") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isUpdatingProfile && !viewModel.isLoadingProfile && !viewModel.isSessionExpired
                    )

                    val canEdit = !viewModel.isLoadingProfile && !viewModel.isUpdatingProfile && !viewModel.isSessionExpired
                    LocationDropdown(
                        label = "País", options = viewModel.countries,
                        selected = viewModel.countries.find { it.iD_Country == viewModel.selectedCountryId },
                        optionLabel = { it.CountryName }, onSelect = viewModel::selectCountry,
                        enabled = canEdit
                    )
                    LocationDropdown(
                        label = "Estado / departamento", options = viewModel.states,
                        selected = viewModel.states.find { it.iD_States == viewModel.selectedStateId },
                        optionLabel = { it.StatesName }, onSelect = viewModel::selectState,
                        loading = viewModel.isLoadingStates,
                        enabled = canEdit && viewModel.selectedCountryId != null
                    )
                    LocationDropdown(
                        label = "Ciudad", options = viewModel.cities,
                        selected = viewModel.cities.find { it.iD_City == viewModel.selectedCityId },
                        optionLabel = { it.CityName }, onSelect = { viewModel.updateSelectedCity(it.iD_City) },
                        loading = viewModel.isLoadingCities,
                        enabled = canEdit && viewModel.selectedStateId != null
                    )

                    // Correo editable y validado antes de enviar
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = viewModel::updateEmail,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canEdit
                    )

                    // Botón actualizar perfil
                    LoadingButton(
                        text = "Actualizar Perfil",
                        isLoading = viewModel.isUpdatingProfile,
                        onClick = viewModel::updateProfile,
                        enabled = canEdit && !viewModel.isLoadingStates && !viewModel.isLoadingCities && viewModel.userProfile != null
                    )
                }
            }

            // Sección de contraseña
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Seguridad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showChangePasswordDialog = true },
                        enabled = !viewModel.isSessionExpired,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cambiar Contraseña")
                    }
                }
            }

            // Información de roles
            viewModel.userProfile?.roles?.let { roles ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Roles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        roles.forEach { role ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(role.TypeRole)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog para cambiar contraseña
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Cambiar Contraseña") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.currentPassword,
                        onValueChange = viewModel::updateCurrentPassword,
                        label = { Text("Contraseña Actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.newPassword,
                        onValueChange = viewModel::updateNewPassword,
                        label = { Text("Nueva Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        label = { Text("Confirmar Nueva Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                LoadingButton(
                    text = "Cambiar",
                    isLoading = viewModel.isChangingPassword,
                    onClick = {
                        viewModel.changePassword()
                        if (viewModel.successMessage != null) {
                            showChangePasswordDialog = false
                        }
                    },
                    modifier = Modifier.width(120.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}