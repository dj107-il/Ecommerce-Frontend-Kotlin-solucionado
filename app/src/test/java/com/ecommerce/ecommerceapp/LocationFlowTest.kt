package com.ecommerce.ecommerceapp

import com.ecommerce.ecommerceapp.models.*
import com.ecommerce.ecommerceapp.network.ApiService
import com.ecommerce.ecommerceapp.viewmodels.LocationManagementViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class LocationFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private val colombia = Country(1, "Colombia")
    private val peru = Country(2, "Perú")
    private val antioquia = State(10, "Antioquia", 1, "Colombia")
    private val lima = State(20, "Lima", 2, "Perú")

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    // Ninguna prueba utiliza Render ni altera usuarios reales.
    private val unusedApi = Proxy.newProxyInstance(
        ApiService::class.java.classLoader, arrayOf(ApiService::class.java)
    ) { _, method, _ -> error("Petición inesperada: " + method.name) } as ApiService

    private inner class FakeApi : ApiService by unusedApi {
        var createdCity: CreateLocationRequest? = null
        override suspend fun getCountries() = Response.success(CountriesResponse(listOf(colombia, peru)))
        override suspend fun getStates(countryId: Int?): Response<StatesResponse> {
            // Una respuesta antigua y lenta no debe reemplazar la nueva selección.
            delay(if (countryId == 1) 1000 else 10)
            return Response.success(StatesResponse(listOf(antioquia, lima).filter { it.iD_Country == countryId }))
        }
        override suspend fun getCities(stateId: Int?) = Response.success(CitiesResponse(emptyList()))
        override suspend fun createCity(request: CreateLocationRequest): Response<CityResponse> {
            createdCity = request
            return Response.success(CityResponse())
        }
    }

    @Test fun countriesLoadWhenViewModelStarts() = runTest(dispatcher) {
        val vm = LocationManagementViewModel(FakeApi())
        advanceUntilIdle()
        assertEquals(listOf(colombia, peru), vm.countries)
        assertFalse(vm.isLoadingCountries)
    }

    @Test fun changingCountryClearsPreviousStateAndLoadsOnlyItsStates() = runTest(dispatcher) {
        val vm = LocationManagementViewModel(FakeApi())
        vm.updateSelectedCountryForState(colombia)
        advanceUntilIdle()
        vm.updateSelectedStateForCity(antioquia)
        advanceUntilIdle()
        vm.updateSelectedCountryForState(peru)
        assertNull(vm.selectedStateForCity)
        assertTrue(vm.states.isEmpty())
        assertTrue(vm.cities.isEmpty())
        advanceUntilIdle()
        assertEquals(listOf(lima), vm.states)
        assertFalse(vm.isLoadingStates)
    }

    @Test fun slowOldCountryCannotOverwriteLatestCountry() = runTest(dispatcher) {
        val vm = LocationManagementViewModel(FakeApi())
        vm.updateSelectedCountryForState(colombia)
        runCurrent()
        vm.updateSelectedCountryForState(peru)
        advanceUntilIdle()
        assertEquals(peru, vm.selectedCountryForState)
        assertEquals(listOf(lima), vm.states)
        assertNull(vm.errorMessage)
        assertFalse(vm.isLoadingStates)
    }

    @Test fun cityCreationSendsSelectedStateAndRejectsOtherCountryState() = runTest(dispatcher) {
        val api = FakeApi()
        val vm = LocationManagementViewModel(api)
        vm.updateSelectedCountryForState(peru)
        advanceUntilIdle()
        vm.updateSelectedStateForCity(antioquia)
        assertNull(vm.selectedStateForCity)
        vm.updateSelectedStateForCity(lima)
        vm.updateNewCityName("Miraflores")
        vm.createCity()
        advanceUntilIdle()
        assertEquals(20, api.createdCity?.iD_States)
        assertEquals("Miraflores", api.createdCity?.CityName)
    }

    @Test fun profileWithoutCityKeepsNullInsteadOfInventingCityOne() {
        val json = """{"user":{"iD_User":5,"UserName":"Ana","Email":"ana@example.com","iD_City":null,"city":null}}"""
        val result = Gson().fromJson(json, UserProfileResponse::class.java)
        assertNotNull(result.user)
        assertNull(result.user!!.iD_City)
    }

    @Test fun profileRequestUsesBackendEmailAndCityFieldNames() {
        val request = UpdateProfileRequest("Ana", 35, "ana@example.com")
        val json = Gson().fromJson(Gson().toJson(request), JsonObject::class.java)
        assertEquals("ana@example.com", json["Email"].asString)
        assertEquals(35, json["iD_City"].asInt)
        assertEquals("Ana", json["UserName"].asString)
    }
}
