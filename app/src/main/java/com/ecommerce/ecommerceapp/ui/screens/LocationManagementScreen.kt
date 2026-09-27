package com.ecommerce.ecommerceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.ecommerce.ecommerceapp.ui.components.LocationDropdown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.ecommerceapp.ui.components.LoadingButton
import com.ecommerce.ecommerceapp.viewmodels.LocationManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocationManagementViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    // Manejar mensajes
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Ubicaciones") },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadCountries()
                        if (viewModel.selectedCountryForState != null) viewModel.loadStates()
                        if (selectedTab == 2) viewModel.loadCities()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar ubicaciones")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Países") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (viewModel.selectedCountryForState != null) viewModel.loadStates()
                    },
                    text = { Text("Estados") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        viewModel.loadCities()
                    },
                    text = { Text("Ciudades") }
                )
            }

            // Contenido según tab seleccionado
            when (selectedTab) {
                0 -> CountriesTab(viewModel) { selectedTab = 1 }
                1 -> StatesTab(viewModel) { selectedTab = 2 }
                2 -> CitiesTab(viewModel)
            }
        }
    }
}

@Composable
fun CountriesTab(viewModel: LocationManagementViewModel, onShowStates: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Formulario para crear país
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Crear Nuevo País",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = viewModel.newCountryName,
                    onValueChange = viewModel::updateNewCountryName,
                    label = { Text("Nombre del País") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LoadingButton(
                    text = "Crear País",
                    isLoading = viewModel.isCreating,
                    onClick = viewModel::createCountry,
                    enabled = viewModel.newCountryName.isNotBlank()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de países
        Text(
            text = "Países Existentes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (viewModel.isLoadingCountries) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.countries) { country ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = country.CountryName,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            IconButton(
                                onClick = { viewModel.updateSelectedCountryForState(country); onShowStates() }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ver estados")  }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatesTab(viewModel: LocationManagementViewModel, onShowCities: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Formulario para crear estado
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Crear Nuevo Estado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                LocationDropdown(
                    label = "País", options = viewModel.countries,
                    selected = viewModel.selectedCountryForState,
                    optionLabel = { it.CountryName }, onSelect = viewModel::updateSelectedCountryForState,
                    loading = viewModel.isLoadingCountries, enabled = !viewModel.isCreating
                )
                LocationDropdown(
                    label = "Estado / departamento", options = viewModel.states,
                    selected = viewModel.selectedStateForCity,
                    optionLabel = { it.StatesName }, onSelect = viewModel::updateSelectedStateForCity,
                    loading = viewModel.isLoadingStates,
                    enabled = viewModel.selectedCountryForState != null && !viewModel.isCreating
                )

                OutlinedTextField(
                    value = viewModel.newStateName,
                    onValueChange = viewModel::updateNewStateName,
                    label = { Text("Nombre del Estado") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LoadingButton(
                    text = "Crear Estado",
                    isLoading = viewModel.isCreating,
                    onClick = viewModel::createState,
                    enabled = viewModel.newStateName.isNotBlank() && viewModel.selectedCountryForState != null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de estados
        Text(
            text = "Estados Existentes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (viewModel.isLoadingStates) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.states) { state ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = state.StatesName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = state.country ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.updateSelectedStateForCity(state); onShowCities() }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ver Ciudades")   }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesTab(viewModel: LocationManagementViewModel) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Formulario para crear ciudad
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Crear Nueva Ciudad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                LocationDropdown(
                    label = "País",
                    options = viewModel.countries,
                    selected = viewModel.selectedCountryForState,
                    optionLabel = { it.CountryName },
                    onSelect = viewModel::updateSelectedCountryForState,
                    loading = viewModel.isLoadingCountries,
                    enabled = !viewModel.isCreating
                )
                LocationDropdown(
                    label = "Estado / departamento",
                    options = viewModel.states,
                    selected = viewModel.selectedStateForCity,
                    optionLabel = { it.StatesName },
                    onSelect = viewModel::updateSelectedStateForCity,
                    loading = viewModel.isLoadingStates,
                    enabled = viewModel.selectedCountryForState != null && !viewModel.isCreating
                )

                OutlinedTextField(
                    value = viewModel.newCityName,
                    onValueChange = viewModel::updateNewCityName,
                    label = { Text("Nombre de la Ciudad") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LoadingButton(
                    text = "Crear Ciudad",
                    isLoading = viewModel.isCreating,
                    onClick = viewModel::createCity,
                    enabled = viewModel.newCityName.isNotBlank() && viewModel.selectedStateForCity != null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de ciudades
        Text(
            text = "Ciudades Existentes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (viewModel.isLoadingCities) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.cities) { city ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = city.CityName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${city.state}, ${city.country}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
