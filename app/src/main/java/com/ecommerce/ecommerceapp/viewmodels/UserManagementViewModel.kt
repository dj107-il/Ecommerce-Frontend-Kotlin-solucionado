package com.ecommerce.ecommerceapp.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.ecommerceapp.models.*
import com.ecommerce.ecommerceapp.network.ApiClient
import com.ecommerce.ecommerceapp.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UserManagementViewModel(private val sessionManager: SessionManager) : ViewModel() {

    var isSessionExpired by mutableStateOf(false)
    var users by mutableStateOf<List<UserProfile>>(emptyList())
    var currentPage by mutableStateOf(1)
    var totalPages by mutableStateOf(1)
    var searchQuery by mutableStateOf("")
    var selectedUser by mutableStateOf<UserProfile?>(null)
    var availableRoles by mutableStateOf<List<Role>>(emptyList())

    // Estados de carga
    var isLoadingUsers by mutableStateOf(false)
    var isLoadingRoles by mutableStateOf(false)
    var isUpdatingRole by mutableStateOf(false)
    var isDeletingUser by mutableStateOf(false)

    // Estados de error y éxito
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init {
        loadUsers()
        loadRoles()
    }

    fun loadUsers(page: Int = 1, search: String = "") {
        isLoadingUsers = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token.isNullOrBlank()) {
                    expireSession()
                    return@launch
                }
                val response = ApiClient.apiService.getUsers(
                    token = "Bearer $token",
                    page = page,
                    perPage = 10,
                    search = search.ifBlank { null }
                )

                if (response.isSuccessful && response.body() != null) {
                    val usersResponse = response.body()!!
                    users = usersResponse.users
                    currentPage = page
                    totalPages = (usersResponse.pagination?.pages ?: 1).coerceAtLeast(1)
                } else {
                    handleHttpError(response.code(), "Error al cargar usuarios")
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("USER_MGMT_DEBUG", "Error loading users", e)
            } finally {
                isLoadingUsers = false
            }
        }
    }

    fun loadRoles() {
        isLoadingRoles = true
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getRoles()
                if (response.isSuccessful && response.body() != null) {
                    availableRoles = response.body()!!.roles
                } else {
                    errorMessage = "Error al cargar roles"
                }
            } catch (e: Exception) {
                errorMessage = "No se pudieron cargar los roles"
                Log.e("USER_MGMT_DEBUG", "Error loading roles", e)
            } finally {
                isLoadingRoles = false
            }
        }
    }

    fun searchUsers(query: String) {
        searchQuery = query
        loadUsers(page = 1, search = query)
    }

    fun loadPage(page: Int) {
        loadUsers(page = page, search = searchQuery)
    }

    fun selectUser(user: UserProfile) {
        selectedUser = user
    }

    fun updateUserRole(userId: Int, roleId: Int) {
        if (isUpdatingRole) return
        clearMessages()
        if (availableRoles.none { it.iDRole == roleId }) {
            errorMessage = "Selecciona un rol válido"
            return
        }
        isUpdatingRole = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token.isNullOrBlank()) {
                    expireSession()
                    return@launch
                }
                val roleRequest = UpdateUserRoleRequest(role_ids = listOf(roleId))
                val response = ApiClient.apiService.updateUserRoles(
                    token = "Bearer $token",
                    id = userId,
                    roleRequest = roleRequest
                )

                if (response.isSuccessful && response.body()?.user != null) {
                    val updatedUser = response.body()!!.user!!
                    users = users.map { if (it.iD_User == updatedUser.iD_User) updatedUser else it }
                    selectedUser = updatedUser
                    successMessage = "Rol de usuario actualizado exitosamente"
                    // Consultar la sesión actual: el administrador puede cambiar su propio rol.
                    val profileResponse = ApiClient.apiService.getProfile("Bearer $token")
                    if (profileResponse.code() == 401) {
                        expireSession()
                        return@launch
                    }
                    val currentProfile = profileResponse.body()?.user
                    if (currentProfile != null) {
                        sessionManager.saveUserSession(
                            token = token,
                            userName = currentProfile.UserName,
                            userEmail = currentProfile.Email,
                            isAdmin = currentProfile.roles.orEmpty().any { it.TypeRole == "Administrador" },
                            roles = currentProfile.roles.orEmpty().map { it.TypeRole }
                        )
                    }
                    if (currentProfile != null && currentProfile.roles.orEmpty().none { it.TypeRole == "Administrador" }) {
                        users = emptyList()
                        availableRoles = emptyList()
                    } else {
                        loadUsers(currentPage, searchQuery)
                    }
                } else {
                    handleHttpError(response.code(), "Error al actualizar el rol del usuario")
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("USER_MGMT_DEBUG", "Error updating user role", e)
            } finally {
                isUpdatingRole = false
            }
        }
    }

    fun deleteUser(userId: Int) {
        isDeletingUser = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token.isNullOrBlank()) {
                    expireSession()
                    return@launch
                }
                val response = ApiClient.apiService.deleteUser("Bearer $token", userId)

                if (response.isSuccessful) {
                    successMessage = "Usuario eliminado exitosamente"
                    loadUsers(currentPage, searchQuery) // Recargar lista
                } else {
                    handleHttpError(response.code(), "Error al eliminar el usuario")
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("USER_MGMT_DEBUG", "Error deleting user", e)
            } finally {
                isDeletingUser = false
            }
        }
    }

    private suspend fun expireSession() {
        isSessionExpired = true
        users = emptyList()
        availableRoles = emptyList()
        selectedUser = null
        successMessage = null
        errorMessage = "Tu sesión venció o no es válida. Inicia sesión nuevamente."
        sessionManager.clearSession()
    }

    private suspend fun handleHttpError(code: Int, fallback: String) {
        when (code) {
            401 -> expireSession()
            403 -> {
                users = emptyList()
                selectedUser = null
                errorMessage = "No tienes permisos de administrador"
            }
            else -> errorMessage = fallback
        }
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}
