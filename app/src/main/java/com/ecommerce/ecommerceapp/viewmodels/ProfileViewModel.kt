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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import android.util.Patterns

class ProfileViewModel(private val sessionManager: SessionManager) : ViewModel() {

    var userProfile by mutableStateOf<UserProfile?>(null)
    private var statesJob: Job? = null
    private var citiesJob: Job? = null
    var email by mutableStateOf("")
    var countries by mutableStateOf<List<Country>>(emptyList())
    var states by mutableStateOf<List<State>>(emptyList())
    var selectedCountryId by mutableStateOf<Int?>(null)
    var selectedStateId by mutableStateOf<Int?>(null)
    var isLoadingStates by mutableStateOf(false)
    var isSessionExpired by mutableStateOf(false)
    var userName by mutableStateOf("")
    var selectedCityId by mutableStateOf<Int?>(null)
    var cities by mutableStateOf<List<City>>(emptyList())

    // Estados de carga
    var isLoadingProfile by mutableStateOf(false)
    var isUpdatingProfile by mutableStateOf(false)
    var isChangingPassword by mutableStateOf(false)
    var isLoadingCities by mutableStateOf(false)

    // Estados de error y éxito
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // Cambio de contraseña
    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (isLoadingProfile || isUpdatingProfile) return
        statesJob?.cancel()
        citiesJob?.cancel()
        isLoadingStates = false
        isLoadingCities = false
        clearMessages()
        isLoadingProfile = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token.isNullOrBlank()) {
                    expireSession()
                    return@launch
                }
                val response = ApiClient.apiService.getProfile("Bearer $token")
                val profile = response.body()?.user
                if (response.code() == 401) {
                    expireSession()
                    return@launch
                }
                if (!response.isSuccessful || profile == null) {
                    errorMessage = "No se pudo cargar el perfil. Usa Recargar."
                    return@launch
                }
                userProfile = profile
                userName = profile.UserName
                email = profile.Email
                selectedCountryId = null
                selectedStateId = null
                selectedCityId = null
                countries = emptyList()
                states = emptyList()
                cities = emptyList()
                val countryResponse = ApiClient.apiService.getCountries()
                val stateResponse = ApiClient.apiService.getStates()
                if (!countryResponse.isSuccessful || !stateResponse.isSuccessful ||
                    countryResponse.body() == null || stateResponse.body() == null) {
                    errorMessage = "No se pudieron cargar las ubicaciones. Usa Recargar."
                    return@launch
                }
                countries = countryResponse.body()!!.countries
                // La ciudad del perfil contiene el ID de su estado; este identifica al país.
                val currentState = stateResponse.body()!!.states.find { it.iD_States == profile.city?.iD_States }
                if (currentState != null && countries.any { it.iD_Country == currentState.iD_Country }) {
                    selectedCountryId = currentState.iD_Country
                    states = stateResponse.body()!!.states.filter { it.iD_Country == selectedCountryId }
                    selectedStateId = currentState.iD_States
                    val cityResponse = ApiClient.apiService.getCities(currentState.iD_States)
                    if (cityResponse.isSuccessful && cityResponse.body() != null) {
                        cities = cityResponse.body()!!.cities
                        selectedCityId = cities.find { it.iD_City == profile.iD_City }?.iD_City
                    } else {
                        errorMessage = "No se pudieron cargar las ciudades. Usa Recargar."
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "No se pudo cargar el perfil o sus ubicaciones. Revisa la conexión y recarga."
                Log.e("PROFILE_DEBUG", "Error loading profile", e)
            } finally {
                isLoadingProfile = false
            }
        }
    }

    private suspend fun expireSession() {
        isSessionExpired = true
        errorMessage = "Tu sesión venció o no es válida. Inicia sesión nuevamente."
        sessionManager.clearSession()
    }

    fun updateProfile() {
        if (isUpdatingProfile || isLoadingProfile || isLoadingStates || isLoadingCities || isSessionExpired || userProfile == null) return
        clearMessages()
        if (userName.isBlank()) {
            errorMessage = "El nombre de usuario es requerido"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            errorMessage = "Escribe un correo electrónico válido"
            return
        }
        val state = states.find { it.iD_States == selectedStateId && it.iD_Country == selectedCountryId }
        val city = cities.find { it.iD_City == selectedCityId && it.iD_States == state?.iD_States }
        if (state == null || city == null) {
            errorMessage = "Selecciona país, estado y ciudad antes de guardar"
            return
        }
        // Capturar los datos antes de iniciar la petición.
        val updateRequest = UpdateProfileRequest(
            UserName = userName.trim(), iD_City = city.iD_City, Email = email.trim()
        )
        isUpdatingProfile = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token.isNullOrBlank()) {
                    expireSession()
                    return@launch
                }
                val response = ApiClient.apiService.updateProfile("Bearer $token", updateRequest)

                if (response.isSuccessful && response.body()?.user != null) {
                    userProfile = response.body()!!.user

                    // Actualizar sesión con nuevo nombre
                    sessionManager.saveUserSession(
                        token = token,
                        userName = userProfile!!.UserName,
                        userEmail = userProfile!!.Email,
                        isAdmin = userProfile?.roles?.any { it.TypeRole == "Administrador" } ?: false,
                        roles = userProfile?.roles?.map { it.TypeRole } ?: emptyList()
                    )

                    email = userProfile!!.Email
                    userName = userProfile!!.UserName
                    selectedCityId = userProfile?.iD_City
                    successMessage = "Perfil actualizado exitosamente"
                } else {
                    if (response.code() == 401) expireSession()
                    else errorMessage = when (response.code()) {
                        409 -> "Ese correo ya está en uso. Elige otro."
                        400 -> "Revisa los datos del perfil antes de guardar."
                        else -> "No se pudo guardar el perfil. Intenta nuevamente."
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("PROFILE_DEBUG", "Error updating profile", e)
            } finally {
                isUpdatingProfile = false
            }
        }
    }

    fun changePassword() {
        if (isChangingPassword || isSessionExpired) return
        clearMessages()
        // Validaciones
        if (currentPassword.isBlank()) {
            errorMessage = "La contraseña actual es requerida"
            return
        }

        if (newPassword.isBlank()) {
            errorMessage = "La nueva contraseña es requerida"
            return
        }

        if (newPassword.length < 6) {
            errorMessage = "La nueva contraseña debe tener al menos 6 caracteres"
            return
        }

        if (newPassword != confirmPassword) {
            errorMessage = "Las contraseñas no coinciden"
            return
        }

        isChangingPassword = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token.isNullOrBlank()) {
                    expireSession()
                    return@launch
                }
                val passwordRequest = ChangePasswordRequest(
                    current_password = currentPassword,
                    new_password = newPassword
                )

                val response = ApiClient.apiService.changePassword("Bearer $token", passwordRequest)

                if (response.isSuccessful) {
                    successMessage = "Contraseña cambiada exitosamente"
                    // Limpiar campos
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                } else {
                    if (response.code() == 401) expireSession()
                    else errorMessage = when (response.code()) {
                        400 -> "Revisa la contraseña actual y la nueva contraseña"
                        else -> "Error al cambiar la contraseña"
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("PROFILE_DEBUG", "Error changing password", e)
            } finally {
                isChangingPassword = false
            }
        }
    }

    fun selectCountry(country: Country) {
        if (isLoadingProfile || isUpdatingProfile || selectedCountryId == country.iD_Country) return
        statesJob?.cancel()
        citiesJob?.cancel()
        selectedCountryId = country.iD_Country
        selectedStateId = null
        selectedCityId = null
        states = emptyList()
        cities = emptyList()
        isLoadingCities = false
        clearMessages()
        isLoadingStates = true
        statesJob = viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getStates(country.iD_Country)
                if (response.isSuccessful && response.body() != null) states = response.body()!!.states
                else errorMessage = "No se pudieron cargar los estados. Usa Recargar."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Error de conexión al cargar estados. Usa Recargar."
            } finally {
                if (kotlin.coroutines.coroutineContext[Job]?.isActive == true) isLoadingStates = false
            }
        }
    }

    fun selectState(state: State) {
        if (isLoadingProfile || isUpdatingProfile || state.iD_Country != selectedCountryId) return
        citiesJob?.cancel()
        selectedStateId = state.iD_States
        selectedCityId = null
        cities = emptyList()
        clearMessages()
        isLoadingCities = true
        citiesJob = viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getCities(state.iD_States)
                if (response.isSuccessful && response.body() != null) cities = response.body()!!.cities
                else errorMessage = "No se pudieron cargar las ciudades. Usa Recargar."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Error de conexión al cargar ciudades. Usa Recargar."
            } finally {
                if (kotlin.coroutines.coroutineContext[Job]?.isActive == true) isLoadingCities = false
            }
        }
    }

    fun updateEmail(value: String) {
        email = value
        clearMessages()
    }

    fun updateUserName(newName: String) {
        userName = newName
        clearMessages()
    }

    fun updateSelectedCity(cityId: Int) {
        if (cities.none { it.iD_City == cityId && it.iD_States == selectedStateId }) return
        selectedCityId = cityId
        clearMessages()
    }

    fun updateCurrentPassword(password: String) {
        currentPassword = password
        clearMessages()
    }

    fun updateNewPassword(password: String) {
        newPassword = password
        clearMessages()
    }

    fun updateConfirmPassword(password: String) {
        confirmPassword = password
        clearMessages()
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}
