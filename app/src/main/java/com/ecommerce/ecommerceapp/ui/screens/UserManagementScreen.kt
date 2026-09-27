package com.ecommerce.ecommerceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.ecommerceapp.models.UserProfile
import com.ecommerce.ecommerceapp.utils.SessionManager
import com.ecommerce.ecommerceapp.viewmodels.UserManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    onLoginRequired: () -> Unit = onNavigateBack,
    viewModel: UserManagementViewModel = viewModel { UserManagementViewModel(sessionManager) }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showUserDialog by remember { mutableStateOf(false) }

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
                title = { Text("Gestión de Usuarios") },
                actions = {
                    IconButton(onClick = { viewModel.loadRoles(); viewModel.loadPage(viewModel.currentPage) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar usuarios y roles")
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
        ) {
            if (viewModel.isSessionExpired) {
                Text("Tu sesión venció o no es válida.")
                Button(onClick = onLoginRequired) { Text("Iniciar sesión") }
            }
            // Barra de búsqueda
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { query ->
                    viewModel.searchUsers(query)
                },
                label = { Text("Buscar usuarios") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de usuarios
            if (viewModel.isLoadingUsers) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.users, key = { it.iD_User }) { user ->
                        UserCard(
                            user = user,
                            availableRoles = viewModel.availableRoles,
                            isUpdatingRole = viewModel.isUpdatingRole,
                            onUpdateRole = { roleId ->
                                viewModel.updateUserRole(user.iD_User, roleId)
                            },
                            onDeleteUser = {
                                viewModel.deleteUser(user.iD_User)
                            }
                        )
                    }

                    // Paginación
                    if (viewModel.totalPages > 1) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { viewModel.loadPage(viewModel.currentPage - 1) },
                                    enabled = viewModel.currentPage > 1
                                ) {
                                    Text("Anterior")
                                }

                                Text(
                                    text = "Página ${viewModel.currentPage} de ${viewModel.totalPages}",
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )

                                Button(
                                    onClick = { viewModel.loadPage(viewModel.currentPage + 1) },
                                    enabled = viewModel.currentPage < viewModel.totalPages
                                ) {
                                    Text("Siguiente")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCard(
    user: UserProfile,
    availableRoles: List<com.ecommerce.ecommerceapp.models.Role>,
    isUpdatingRole: Boolean,
    onUpdateRole: (Int) -> Unit,
    onDeleteUser: () -> Unit
) {
    var expandedRoles by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = user.UserName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.Email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar usuario",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Información de ubicación
            user.city?.let { city ->
                Text(
                    text = "📍 ${city.CityName}, ${city.state}, ${city.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Roles actuales
            user.roles?.let { roles ->
                Text(
                    text = "Roles: ${roles.joinToString(", ") { it.TypeRole }}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Selector de nuevo rol
            ExposedDropdownMenuBox(
                expanded = expandedRoles,
                onExpandedChange = { if (!isUpdatingRole) expandedRoles = !expandedRoles }
            ) {
                OutlinedTextField(
                    value = "Cambiar rol",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Asignar rol") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoles) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedRoles,
                    onDismissRequest = { expandedRoles = false }
                ) {
                    if (availableRoles.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sin roles disponibles. Usa Recargar.") },
                            onClick = {}, enabled = false
                        )
                    }
                    availableRoles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.TypeRole) },
                            onClick = {
                                onUpdateRole(role.iDRole)
                                expandedRoles = false
                            },
                            enabled = !isUpdatingRole
                        )
                    }
                }
            }
        }
    }

    // Dialog de confirmación para eliminar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Usuario") },
            text = { Text("¿Estás seguro de que deseas eliminar a ${user.UserName}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
