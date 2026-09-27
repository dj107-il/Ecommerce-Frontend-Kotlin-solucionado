package com.ecommerce.ecommerceapp.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.ecommerceapp.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WelcomeViewModel(private val sessionManager: SessionManager) : ViewModel() {

    var userName by mutableStateOf("")
    var isAdmin by mutableStateOf(false)
    var loginTime by mutableStateOf("")

    init {
        loadUserData()
        setLoginTime()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            sessionManager.userName.collect { userName = it ?: "Usuario"}
        }
        viewModelScope.launch {
            sessionManager.isAdmin.collect { isAdmin = it }
        }
    }

    private fun setLoginTime() {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date())
        loginTime = "Hora de inicio: $currentTime"
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }
}