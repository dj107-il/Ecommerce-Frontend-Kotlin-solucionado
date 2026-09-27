package com.ecommerce.ecommerceapp.models

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("iD_User")
    val iD_User: Int,

    @SerializedName("UserName")
    val UserName: String,

    @SerializedName("Email")
    val Email: String,

    @SerializedName("iD_City")
    val iD_City: Int?,

    @SerializedName("city")
    val city: City? = null,

    @SerializedName("roles")
    val roles: List<Role>? = null
)

data class UpdateProfileRequest(
    @SerializedName("UserName")
    val UserName: String,

    @SerializedName("iD_City")
    val iD_City: Int?,

    @SerializedName("Email")
    val Email: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password")
    val current_password: String,

    @SerializedName("new_password")
    val new_password: String
)

data class UserProfileResponse(
    @SerializedName("user")
    val user: UserProfile? = null
)