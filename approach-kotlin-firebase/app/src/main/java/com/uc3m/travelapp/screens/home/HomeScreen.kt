package com.uc3m.travelapp.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uc3m.travelapp.FirebaseManager
import com.uc3m.travelapp.R
import com.uc3m.travelapp.model.Trip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onTripClick: (String) -> Unit,
    onAddTripClick: () -> Unit
) {
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var maxBudget by remember { mutableStateOf(10000f) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf("all") }
    // Categories filter now supports multiple selection (MA-02)
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilters by remember { mutableStateOf(false) }
    // Search bar visibility controlled by the search icon (MA-02)
    var showSearch by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val durationOptions = listOf(
        "all" to stringResource(R.string.home_filter_duration_all),
        "short" to stringResource(R.string.home_filter_duration_short),
        "medium" to stringResource(R.string.home_filter_duration_medium),
        "long" to stringResource(R.string.home_filter_duration_long)
    )

    val categoryOptions = listOf(
        "urban" to stringResource(R.string.category_urban),
        "cultural" to stringResource(R.string.category_cultural),
        "nature" to stringResource(R.string.category_nature),
        "gastronomic" to stringResource(R.string.category_gastronomic),
        "party" to stringResource(R.string.category_party),
        "beach" to stringResource(R.string.category_beach),
        "adventure" to stringResource(R.string.category_adventure),
        "relax" to stringResource(R.string.category_relax),
        "roadtrip" to stringResource(R.string.category_roadtrip),
        "backpacker" to stringResource(R.string.category_backpacker)
    )

    // All filters combined - categories now use Set for multiple selection (MA-02)
    val filteredTrips = trips
        .filter { it.totalBudget <= maxBudget }
        .filter { trip ->
            if (searchQuery.isBlank()) true
            else trip.destination.contains(searchQuery, ignoreCase = true) ||
                    trip.title.contains(searchQuery, ignoreCase = true)
        }
        .filter { trip ->
            if (selectedDuration == "all") return@filter true
            val days = calculateTripDays(trip.startDate, trip.endDate)
            when (selectedDuration) {
                "short" -> days in 1..4
                "medium" -> days in 5..10
                "long" -> days > 10
                else -> true
            }
        }
        .filter { trip ->
            // If no categories selected show all, otherwise trip must match at least one (MA-02)
            if (selectedCategories.isEmpty()) true
            else trip.categories.any { it in selectedCategories }
        }

    LaunchedEffect(Unit) {
        FirebaseManager.firestore
            .collection("trips")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    errorMessage = error.message
                        ?: context.getString(R.string.home_error_loading_trips)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trips = snapshot.documents.mapNotNull { it.toObject(Trip::class.java) }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    // Search icon toggles the search bar visibility (MA-02)
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.home_cd_search),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // Filter icon toggles the filter panel (MA-02)
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.home_filter_cd_toggle),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTripClick,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_cd_add_trip),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            trips.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.home_no_trips_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_no_trips_subtitle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    // Search bar - only visible when search icon is tapped (MA-02)
                    if (showSearch) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.home_search_placeholder),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    // Collapsible filter panel
                    if (showFilters) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.home_filter_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = stringResource(
                                            R.string.home_filter_label,
                                            maxBudget.toInt()
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = maxBudget,
                                        onValueChange = { maxBudget = it },
                                        valueRange = 0f..10000f,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = stringResource(R.string.home_filter_duration_label),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        durationOptions.forEach { (key, label) ->
                                            FilterChip(
                                                selected = selectedDuration == key,
                                                onClick = { selectedDuration = key },
                                                label = {
                                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = stringResource(R.string.home_filter_category_label),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Categories now support multiple selection (MA-02)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        categoryOptions.forEach { (key, label) ->
                                            FilterChip(
                                                selected = selectedCategories.contains(key),
                                                onClick = {
                                                    selectedCategories =
                                                        if (selectedCategories.contains(key))
                                                            selectedCategories - key
                                                        else
                                                            selectedCategories + key
                                                },
                                                label = {
                                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            maxBudget = 10000f
                                            searchQuery = ""
                                            selectedDuration = "all"
                                            selectedCategories = emptySet()
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(stringResource(R.string.home_filter_clear))
                                    }
                                }
                            }
                        }
                    }

                    if (filteredTrips.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.home_no_trips_title),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredTrips) { trip ->
                            TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

private fun calculateTripDays(startDate: String, endDate: String): Int {
    return try {
        if (startDate.isBlank() || endDate.isBlank()) return 0
        val parts1 = startDate.split("/")
        val parts2 = endDate.split("/")
        if (parts1.size != 3 || parts2.size != 3) return 0
        val start = java.util.Calendar.getInstance().apply {
            set(parts1[2].toInt(), parts1[1].toInt() - 1, parts1[0].toInt())
        }
        val end = java.util.Calendar.getInstance().apply {
            set(parts2[2].toInt(), parts2[1].toInt() - 1, parts2[0].toInt())
        }
        val diff = end.timeInMillis - start.timeInMillis
        (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }
}

@Composable
fun TripCard(trip: Trip, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (trip.coverImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = trip.coverImageUrl,
                        contentDescription = stringResource(R.string.home_cd_cover_image, trip.title),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.home_no_cover_placeholder), fontSize = 56.sp)
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Text(
                            text = "€${"%.0f".format(trip.totalBudget)}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = trip.likes.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${trip.destination}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (trip.startDate.isNotEmpty() && trip.endDate.isNotEmpty()) {
                        Text(
                            text = "${trip.startDate} → ${trip.endDate}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trip.authorEmail,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}