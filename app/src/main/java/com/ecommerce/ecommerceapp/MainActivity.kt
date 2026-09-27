package com.ecommerce.ecommerceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecommerce.ecommerceapp.models.AuthResponse
import com.ecommerce.ecommerceapp.ui.screens.CartScreen
import com.ecommerce.ecommerceapp.ui.screens.CategoryManagementScreen
import com.ecommerce.ecommerceapp.ui.screens.LocationManagementScreen
import com.ecommerce.ecommerceapp.ui.screens.LoginScreen
import com.ecommerce.ecommerceapp.ui.screens.OrderSuccessScreen
import com.ecommerce.ecommerceapp.ui.screens.ProductCatalogScreen
import com.ecommerce.ecommerceapp.ui.screens.ProductManagementScreen
import com.ecommerce.ecommerceapp.ui.screens.ProfileScreen
import com.ecommerce.ecommerceapp.ui.screens.RegisterScreen
import com.ecommerce.ecommerceapp.ui.screens.SalesHistoryScreen
import com.ecommerce.ecommerceapp.ui.screens.UserManagementScreen
import com.ecommerce.ecommerceapp.ui.screens.WelcomeScreen
import com.ecommerce.ecommerceapp.ui.theme.EcommerceAppTheme
import com.ecommerce.ecommerceapp.utils.SessionManager
import com.ecommerce.ecommerceapp.viewmodels.CartViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = SessionManager(this)

        setContent {
            EcommerceAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EcommerceApp(
                        sessionManager = sessionManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun EcommerceApp(
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)

    // ViewModel del carrito (solo para usuarios logueados)
    val cartViewModel: CartViewModel? = if (isLoggedIn) {
        androidx.lifecycle.viewmodel.compose.viewModel { CartViewModel(sessionManager) }
    } else {
        null
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "welcome" else "catalog",
        modifier = modifier
    ) {
        // ========== PANTALLA PRINCIPAL: CATÁLOGO DE PRODUCTOS ==========
        // Accesible para todos (Anónimo, Usuario, Admin)
        composable("catalog") {
            ProductCatalogScreen(
                onNavigateBack = {
                    // Si está logueado, ir a welcome, sino salir o mostrar opciones
                    if (isLoggedIn) {
                        navController.navigate("welcome")
                    } else {
                        // Mostrar opciones de login/registro
                        navController.navigate("auth_options")
                    }
                },
                onNavigateToCart = if (isLoggedIn && cartViewModel != null) {
                    { navController.navigate("cart") }
                } else null,
                onProductClick = { product ->
                    // TODO: Navegar a detalle del producto cuando se implemente
                    // navController.navigate("product_detail/${product.id_Product}")
                },
                cartViewModel = cartViewModel,
                isUserLoggedIn = isLoggedIn
            )
        }

        // ========== PANTALLA DE OPCIONES DE AUTENTICACIÓN ==========
        composable("auth_options") {
            AuthOptionsScreen(
                onNavigateToLogin = {
                    navController.navigate("login")
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToCatalog = {
                    navController.navigate("catalog")
                }
            )
        }

        // ========== PANTALLAS DEL CARRITO (Solo usuarios logueados) ==========
        composable("cart") {
            if (isLoggedIn && cartViewModel != null) {
                CartScreen(
                    sessionManager = sessionManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onCheckoutSuccess = {
                        navController.navigate("order_success") {
                            popUpTo("cart") { inclusive = true }
                        }
                    },
                    viewModel = cartViewModel
                )
            }
        }

        composable("order_success") {
            OrderSuccessScreen(
                onNavigateToCatalog = {
                    navController.navigate("catalog") {
                        popUpTo("order_success") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("welcome") {
                        popUpTo("order_success") { inclusive = true }
                    }
                }
            )
        }

        composable("sales_history") {
            if (isLoggedIn) {
                SalesHistoryScreen(
                    sessionManager = sessionManager,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // ========== PANTALLA DE LOGIN ==========
        composable("login") {
            LoginScreen(
                onLoginSuccess = { authResponse ->
                    // Guardar sesión con los datos del AuthResponse
                    val context = navController.context
                    if (context is ComponentActivity) {
                        context.lifecycleScope.launch {
                            sessionManager.saveUserSession(
                                token = authResponse.token!!,
                                userName = authResponse.user!!.UserName,
                                userEmail = authResponse.user!!.Email,
                                isAdmin = authResponse.isAdmin(),
                                roles = authResponse.getRoleNames()
                            )

                            // Navegar a welcome después de guardar la sesión
                            navController.navigate("welcome") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                registeredEmail = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("registered_email")
            )
        }

        // ========== PANTALLA DE REGISTRO ==========
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { email ->
                    // Guardar el email para pre-llenarlo en login
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("registered_email", email)

                    // Navegar al login
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ========== PANTALLA PRINCIPAL PARA USUARIOS LOGUEADOS ==========
        composable("welcome") {
            WelcomeScreen(
                sessionManager = sessionManager,
                onLogout = {
                    navController.navigate("catalog") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToUserManagement = {
                    navController.navigate("user_management")
                },
                onNavigateToLocationManagement = {
                    navController.navigate("location_management")
                },
                onNavigateToCategoryManagement = {
                    navController.navigate("category_management")
                },
                onNavigateToProductManagement = {
                    navController.navigate("product_management")
                },
                onNavigateToCatalog = {
                    navController.navigate("catalog")
                },
                onNavigateToCart = {
                    navController.navigate("cart")
                },
                onNavigateToSalesHistory = {
                    navController.navigate("sales_history")
                },
                cartViewModel = cartViewModel
            )
        }

        // ========== PANTALLA DE PERFIL ==========
        composable("profile") {
            ProfileScreen(
                sessionManager = sessionManager,
                onLoginRequired = {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== PANTALLAS DE ADMINISTRACIÓN ==========
        composable("user_management") {
            UserManagementScreen(
                sessionManager = sessionManager,
                onLoginRequired = {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("location_management") {
            LocationManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("category_management") {
            CategoryManagementScreen(
                sessionManager = sessionManager,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("product_management") {
            ProductManagementScreen(
                sessionManager = sessionManager,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun AuthOptionsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToCatalog: () -> Unit
) {
    // Pantalla simple con opciones para usuarios anónimos
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "E-commerce App",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(48.dp))

        androidx.compose.material3.Button(
            onClick = onNavigateToCatalog,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.ShoppingCart,
                contentDescription = null
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Text("Ver Catálogo de Productos")
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.material3.Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.Login,
                contentDescription = null
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Text("Iniciar Sesión")
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.PersonAdd,
                contentDescription = null
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Text("Crear Cuenta")
        }
    }
}