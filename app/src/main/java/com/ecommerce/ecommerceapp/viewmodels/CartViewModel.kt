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

class CartViewModel(private val sessionManager: SessionManager) : ViewModel() {

    // Estado del carrito
    var cart by mutableStateOf<Cart?>(null)
    var cartItemsCount by mutableStateOf(0)

    // Estados de carga
    var isLoadingCart by mutableStateOf(false)
    var isAddingToCart by mutableStateOf(false)
    var isUpdatingCart by mutableStateOf(false)
    var isRemovingFromCart by mutableStateOf(false)
    var isClearingCart by mutableStateOf(false)
    var isCheckingOut by mutableStateOf(false)

    // Estados de error y éxito
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // Checkout
    var checkoutDescription by mutableStateOf("")
    var lastCompletedSale by mutableStateOf<Sale?>(null)

    init {
        loadCart()
    }

    fun loadCart() {
        isLoadingCart = true
        Log.d("CART_DEBUG", "Cargando carrito...")

        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                Log.d("CART_DEBUG", "Token para cargar carrito: ${token?.take(20)}...")

                if (token != null) {
                    val response = ApiClient.apiService.getCart("Bearer $token")
                    Log.d("CART_DEBUG", "Response code loadCart: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        Log.d("CART_DEBUG", "Response body recibido:")
                        Log.d("CART_DEBUG", "- Items en respuesta: ${responseBody.items.size}")
                        Log.d("CART_DEBUG", "- Total items: ${responseBody.total_items}")
                        Log.d("CART_DEBUG", "- Total amount: ${responseBody.total_amount}")
                        responseBody.items.forEach { item ->
                            Log.d("CART_DEBUG", "  * Producto: ${item.product?.ProductName}, Cantidad: ${item.quantity}, Subtotal: ${item.subtotal}")
                        }

                        cart = responseBody
                        cartItemsCount = cart?.total_items ?: 0
                        Log.d("CART_DEBUG", "Carrito actualizado - cartItemsCount: $cartItemsCount")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("CART_DEBUG", "Error response body: $errorBody")

                        // Si es 404, significa carrito vacío
                        if (response.code() == 404) {
                            cart = Cart(items = emptyList(), total_items = 0, total_amount = 0.0)
                            cartItemsCount = 0
                            Log.d("CART_DEBUG", "Carrito vacío (404) - inicializando carrito vacío")
                        } else {
                            errorMessage = "Error al cargar el carrito"
                            Log.e("CART_DEBUG", "Error response loadCart: ${response.code()}")
                        }
                    }
                } else {
                    errorMessage = "Sesión expirada"
                    Log.e("CART_DEBUG", "Token es null al cargar carrito")
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("CART_DEBUG", "Exception loading cart", e)
            } finally {
                isLoadingCart = false
                Log.d("CART_DEBUG", "Finalizando loadCart")
            }
        }
    }

    fun addToCart(productId: Int, quantity: Int = 1) {
        isAddingToCart = true
        Log.d("CART_DEBUG", "Intentando agregar producto $productId con cantidad $quantity")

        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                Log.d("CART_DEBUG", "Token obtenido: ${token?.take(20)}...")

                if (token != null) {
                    val request = AddToCartRequest(
                        id_Product = productId,
                        quantity = quantity
                    )
                    Log.d("CART_DEBUG", "Enviando request: $request")

                    val response = ApiClient.apiService.addToCart("Bearer $token", request)
                    Log.d("CART_DEBUG", "Response code: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        loadCart()
                        successMessage = "Producto agregado al carrito"
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("CART_DEBUG", "Error response body: $errorBody")

                        errorMessage = when (response.code()) {
                            404 -> "Producto no encontrado"
                            400 -> "Stock insuficiente o datos inválidos"
                            401 -> "Sesión expirada, por favor inicia sesión nuevamente"
                            else -> "Error al agregar al carrito (${response.code()})"
                        }
                        Log.e("CART_DEBUG", "Error adding to cart: ${response.code()} - ${errorMessage}")
                    }
                } else {
                    errorMessage = "Sesión expirada, por favor inicia sesión nuevamente"
                    Log.e("CART_DEBUG", "Token es null")
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("CART_DEBUG", "Exception adding to cart", e)
            } finally {
                isAddingToCart = false
                Log.d("CART_DEBUG", "Finalizando addToCart. isAddingToCart = false")
            }
        }
    }

    fun updateCartItemQuantity(itemId: Int, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(itemId)
            return
        }

        isUpdatingCart = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    val request = UpdateCartQuantityRequest(quantity = newQuantity)
                    val response = ApiClient.apiService.updateCartItem(
                        token = "Bearer $token",
                        itemId = itemId,
                        quantity = request
                    )

                    if (response.isSuccessful && response.body() != null) {
                        loadCart()
                        successMessage = "Cantidad actualizada"
                        Log.d("CART_DEBUG", "Cantidad actualizada en el carrito")
                    } else {
                        errorMessage = when (response.code()) {
                            404 -> "Item no encontrado en el carrito"
                            400 -> "Stock insuficiente"
                            else -> "Error al actualizar cantidad"
                        }
                        Log.e("CART_DEBUG", "Error updating cart item: ${response.code()}")
                    }
                } else {
                    errorMessage = "Sesión expirada"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("CART_DEBUG", "Exception updating cart item", e)
            } finally {
                isUpdatingCart = false
            }
        }
    }

    fun removeFromCart(itemId: Int) {
        isRemovingFromCart = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    val response = ApiClient.apiService.removeFromCart("Bearer $token", itemId)

                    if (response.isSuccessful) {
                        // Recargar carrito después de eliminar
                        loadCart()
                        successMessage = "Producto eliminado del carrito"
                        Log.d("CART_DEBUG", "Producto eliminado del carrito")
                    } else {
                        errorMessage = when (response.code()) {
                            404 -> "Item no encontrado en el carrito"
                            else -> "Error al eliminar del carrito"
                        }
                        Log.e("CART_DEBUG", "Error removing from cart: ${response.code()}")
                    }
                } else {
                    errorMessage = "Sesión expirada"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("CART_DEBUG", "Exception removing from cart", e)
            } finally {
                isRemovingFromCart = false
            }
        }
    }

    fun clearCart() {
        isClearingCart = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    val response = ApiClient.apiService.clearCart("Bearer $token")

                    if (response.isSuccessful) {
                        cart = Cart(items = emptyList(), total_items = 0, total_amount = 0.0)
                        cartItemsCount = 0
                        successMessage = "Carrito vaciado"
                        Log.d("CART_DEBUG", "Carrito vaciado")
                    } else {
                        errorMessage = "Error al vaciar el carrito"
                        Log.e("CART_DEBUG", "Error clearing cart: ${response.code()}")
                    }
                } else {
                    errorMessage = "Sesión expirada"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("CART_DEBUG", "Exception clearing cart", e)
            } finally {
                isClearingCart = false
            }
        }
    }

    fun checkout() {
        if (isCheckingOut || isLoadingCart || isAddingToCart || isUpdatingCart ||
            isRemovingFromCart || isClearingCart) return
        clearMessages()
        lastCompletedSale = null
        if (checkoutDescription.isBlank()) {
            errorMessage = "Ingrese una descripción para el pedido"
            return
        }

        if (cart?.items?.isEmpty() != false) {
            errorMessage = "El carrito está vacío"
            return
        }

        isCheckingOut = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    val request = CheckoutRequest(DescripcionSale = checkoutDescription.trim())
                    val response = ApiClient.apiService.checkout("Bearer $token", request)

                    if (response.isSuccessful && response.body() != null) {
                        val checkoutResponse = response.body()!!

                        if (checkoutResponse.isSuccess()) {
                            // Carrito se vacía después del checkout exitoso
                            cart = Cart(items = emptyList(), total_items = 0, total_amount = 0.0)
                            cartItemsCount = 0
                            lastCompletedSale = checkoutResponse.sale

                            successMessage = "¡Pedido realizado exitosamente!"
                            checkoutDescription = ""
                            Log.d("CART_DEBUG", "Checkout exitoso: ${checkoutResponse.sale_id}")
                        } else {
                            errorMessage = checkoutResponse.message ?: "Error al procesar el pedido"
                        }
                    } else {
                        errorMessage = when (response.code()) {
                            400 -> "Datos de pedido inválidos"
                            404 -> "Carrito vacío o no encontrado"
                            409 -> "Stock insuficiente para algunos productos"
                            else -> "Error al procesar el pedido"
                        }
                        Log.e("CART_DEBUG", "Error during checkout: ${response.code()}")
                    }
                } else {
                    errorMessage = "Sesión expirada"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("CART_DEBUG", "Exception during checkout", e)
            } finally {
                isCheckingOut = false
            }
        }
    }

    // Funciones de utilidad
    fun getCartItemCount(): Int = cartItemsCount

    fun getCartTotal(): Double = cart?.total_amount ?: 0.0

    fun isCartEmpty(): Boolean = cart?.items?.isEmpty() != false

    fun getProductQuantityInCart(productId: Int): Int {
        return cart?.items?.find { it.id_Product == productId }?.quantity ?: 0
    }

    fun hasProductInCart(productId: Int): Boolean {
        return cart?.items?.any { it.id_Product == productId } == true
    }

    // Funciones de actualización para la UI
    fun updateCheckoutDescription(description: String) {
        checkoutDescription = description
        clearMessages()
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }

    fun clearLastCompletedSale() {
        lastCompletedSale = null
    }

    fun refreshCart() {
        Log.d("CART_DEBUG", "=== REFRESH CART INICIADO ===")
        Log.d("CART_DEBUG", "Estado ANTES del refresh: ${getDebugInfo()}")
        loadCart()
        Log.d("CART_DEBUG", "=== REFRESH CART FINALIZADO ===")
    }

    // Función para obtener info de debugging
    fun getDebugInfo(): String {
        return """
            Cart Debug Info:
            - Items count: $cartItemsCount
            - Cart items: ${cart?.items?.size ?: 0}
            - Total amount: ${cart?.total_amount ?: 0.0}
            - Is loading: $isLoadingCart
            - Is adding: $isAddingToCart
            - Cart object: ${cart?.let { "EXISTS" } ?: "NULL"}
            - Items details: ${cart?.items?.joinToString { "${it.product?.ProductName}(${it.quantity})" } ?: "NONE"}
        """.trimIndent()
    }

    // Nueva función para debug desde UI
    fun debugCurrentState() {
        Log.d("CART_DEBUG", "=== DEBUG MANUAL ===")
        Log.d("CART_DEBUG", getDebugInfo())
        Log.d("CART_DEBUG", "==================")
    }

    // Función para testear la API directamente
    fun testApiConnection() {
        Log.d("CART_DEBUG", "=== TESTING API CONNECTION ===")
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                Log.d("CART_DEBUG", "Token test: ${token?.take(50)}...")

                if (token != null) {
                    // Test get cart
                    val cartResponse = ApiClient.apiService.getCart("Bearer $token")
                    Log.d("CART_DEBUG", "GET CART Test - Code: ${cartResponse.code()}")
                    Log.d("CART_DEBUG", "GET CART Test - Success: ${cartResponse.isSuccessful}")

                    if (cartResponse.isSuccessful) {
                        val body = cartResponse.body()
                        Log.d("CART_DEBUG", "GET CART Test - Body: $body")
                    } else {
                        val error = cartResponse.errorBody()?.string()
                        Log.d("CART_DEBUG", "GET CART Test - Error: $error")
                    }
                } else {
                    Log.e("CART_DEBUG", "TEST FAILED: Token es null")
                }
            } catch (e: Exception) {
                Log.e("CART_DEBUG", "TEST FAILED: Exception", e)
            }
        }
        Log.d("CART_DEBUG", "===========================")
    }
}