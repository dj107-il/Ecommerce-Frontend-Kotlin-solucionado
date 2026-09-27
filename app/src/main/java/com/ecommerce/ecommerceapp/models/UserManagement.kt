package com.ecommerce.ecommerceapp.models

import com.google.gson.annotations.SerializedName

data class UsersResponse(
    @SerializedName("users")
    val users: List<UserProfile> = emptyList(),

    @SerializedName("pagination")
    val pagination: SalesPagination? = null,

    @SerializedName("total")
    val total: Int = 0
)

data class Pagination(
    @SerializedName("page")
    val page: Int,

    @SerializedName("per_page")
    val per_page: Int,

    @SerializedName("total_pages")
    val total_pages: Int
)

data class UpdateUserRoleRequest(
    @SerializedName("role_ids")
    val role_ids: List<Int>
)

data class RolesResponse(
    @SerializedName("roles")
    val roles: List<Role> = emptyList()
)