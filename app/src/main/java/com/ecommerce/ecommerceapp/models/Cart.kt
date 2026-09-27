package com.ecommerce.ecommerceapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

@Parcelize
data class CartItem(
    @SerializedName("id_TemporalSales")
    val id_cart_item: Int = 0,  // Mapear id_TemporalSales a id_cart_item

    @SerializedName("id_Product")
    val id_Product: Int = 0,

    @SerializedName("quantity")
    val quantity: Int = 1,

    @SerializedName("product")
    val product: Product? = null,

    @SerializedName("subtotal")
    val subtotal: Double = 0.0,

    // Campos adicionales que devuelve la API
    @SerializedName("DateAdded")
    val DateAdded: String? = null,

    @SerializedName("iD_User")
    val iD_User: Int = 0,

    @SerializedName("id_Sale")
    val id_Sale: Int? = null
) : Parcelable

@Parcelize
data class Cart(
    @SerializedName("cart_items")
    val items: List<CartItem> = emptyList(),

    @SerializedName("count")
    val total_items: Int = 0,

    @SerializedName("total")
    val total_amount: Double = 0.0,

    @SerializedName("message")
    val message: String? = null
) : Parcelable

data class AddToCartRequest(
    @SerializedName("id_Product")
    val id_Product: Int,

    @SerializedName("quantity")
    val quantity: Int
)

data class UpdateCartQuantityRequest(
    @SerializedName("quantity")
    val quantity: Int
)

@Parcelize
data class Sale(
    @SerializedName("id_Sale")
    val id_Sale: Int = 0,

    @SerializedName("DescripcionSale")
    val DescripcionSale: String = "",

    @SerializedName("total")
    val TotalSale: Double = 0.0,

    @SerializedName("DateCreated")
    val DateSale: String = "",

    @SerializedName("iD_User")
    val iD_User: Int = 0,

    @SerializedName("user")
    val user: String? = null,

    @SerializedName("details")
    val sale_details: List<SaleDetail> = emptyList()
) : Parcelable

@Parcelize
data class SaleDetail(
    @SerializedName("id_SaleDetail")
    val id_SaleDetail: Int = 0,

    @SerializedName("amount")
    val Quantity: Int = 0,

    @SerializedName("ValueSale")
    val Subtotal: Double = 0.0,

    @SerializedName("id_Product")
    val id_Product: Int = 0,

    @SerializedName("product")
    val product: Product? = null
) : Parcelable{
    // Precio histórico: no usar el precio actual del catálogo.
    val UnitPrice: Double
        get() = if (Quantity > 0) Subtotal / Quantity else 0.0
}

data class CheckoutRequest(
    @SerializedName("DescripcionSale")
    val DescripcionSale: String
)

data class CheckoutResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("sale")
    val sale: Sale? = null,

    @SerializedName("sale_id")
    val sale_id: Int? = null
) {
    fun isSuccess(): Boolean {
        return (sale?.id_Sale ?: 0) > 0
    }
}

data class SalesResponse(
    @SerializedName("sales")
    val sales: List<Sale> = emptyList(),

    @SerializedName("pagination")
    val pagination: SalesPagination? = null,

    @SerializedName("total")
    val total: Int = 0
)

data class SaleResponse (
    @SerializedName("sale") val sale: Sale? = null
)

data class SalesPagination(
    @SerializedName("page") val page: Int = 1,
    @SerializedName("pages") val pages: Int = 1,
    @SerializedName("per_page") val perPage: Int = 10,
    @SerializedName("total") val total: Int = 0
)

data class CartMutationResponse(
    @SerializedName("message") val message: String? = null
)
