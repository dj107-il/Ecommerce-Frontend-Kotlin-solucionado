package com.ecommerce.ecommerceapp.models

import com.google.gson.annotations.SerializedName

data class Country(
    @SerializedName("iD_Country")
    val iD_Country: Int,

    @SerializedName("CountryName")
    val CountryName: String
)

data class State(
    @SerializedName("iD_States")
    val iD_States: Int,

    @SerializedName("StatesName")
    val StatesName: String,

    @SerializedName("iD_Country")
    val iD_Country: Int,

    @SerializedName("country")
    val country: String? = null
)

data class CreateLocationRequest(
    @SerializedName("CountryName")
    val CountryName: String? = null,

    @SerializedName("StatesName")
    val StatesName: String? = null,

    @SerializedName("CityName")
    val CityName: String? = null,

    @SerializedName("iD_Country")
    val iD_Country: Int? = null,

    @SerializedName("iD_States")
    val iD_States: Int? = null
)

data class CountriesResponse(
    @SerializedName("countries")
    val countries: List<Country> = emptyList()
)

data class StatesResponse(
    @SerializedName("states")
    val states: List<State> = emptyList()
)

data class CitiesResponse(
    @SerializedName("cities")
    val cities: List<City> = emptyList()
)

data class CountryResponse(
    @SerializedName("country")
    val country: Country? = null
)

data class StateResponse(
    @SerializedName("state")
    val state: State? = null
)

data class CityResponse(
    @SerializedName("city")
    val city: City? = null
)