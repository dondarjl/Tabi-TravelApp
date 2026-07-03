package com.uc3m.travelapp.screens.addtrip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uc3m.travelapp.FirebaseManager
import com.uc3m.travelapp.NotificationHelper
import com.uc3m.travelapp.R
import com.uc3m.travelapp.model.DayEntry
import com.uc3m.travelapp.model.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTripScreen(onTripSaved: () -> Unit, onBackClick: () -> Unit) {

    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var flightCost by remember { mutableStateOf("") }
    var accommodationCost by remember { mutableStateOf("") }
    var foodCost by remember { mutableStateOf("") }
    var transportCost by remember { mutableStateOf("") }
    var activitiesCost by remember { mutableStateOf("") }
    var otherCost by remember { mutableStateOf("") }

    // Itinerary state - list of day entries generated from the selected dates (MA-02)
    var itinerary by remember { mutableStateOf<List<DayEntry>>(emptyList()) }

    // Raw text for each day's places field - allows spaces and commas while typing (MA-02)
    // We store this separately from itinerary.places to avoid splitting on every keystroke
    var placesTexts by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var tripSaved by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val availableCategories = listOf(
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri
    }

    // Regenerate itinerary when both dates are set (MA-02)
    LaunchedEffect(startDate, endDate) {
        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            try {
                val start = dateFormatter.parse(startDate)
                val end = dateFormatter.parse(endDate)
                if (start != null && end != null && end >= start) {
                    val days = ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt() + 1
                    if (days != itinerary.size) {
                        itinerary = List(days) { index ->
                            itinerary.getOrNull(index) ?: DayEntry(dayNumber = index + 1)
                        }
                        // Sync placesTexts with the new itinerary preserving existing input (MA-02)
                        placesTexts = itinerary.associate { day ->
                            day.dayNumber to (placesTexts[day.dayNumber]
                                ?: day.places.joinToString(", "))
                        }
                    }
                }
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(tripSaved) {
        if (tripSaved) onTripSaved()
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        startDate = dateFormatter.format(Date(millis))
                    }
                    showStartDatePicker = false
                }) { Text(stringResource(R.string.date_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            }
        ) {
            DatePicker(state = startDatePickerState, title = {
                Text(
                    text = stringResource(R.string.date_picker_start_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            })
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        endDate = dateFormatter.format(Date(millis))
                    }
                    showEndDatePicker = false
                }) { Text(stringResource(R.string.date_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            }
        ) {
            DatePicker(state = endDatePickerState, title = {
                Text(
                    text = stringResource(R.string.date_picker_end_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.add_trip_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.add_trip_cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // --- Cover photo ---
            item {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = stringResource(R.string.add_trip_cover_photo_cd),
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.add_trip_cover_photo_change)) }
                } else {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.add_trip_cover_photo_button)) }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item { SectionTitle(text = stringResource(R.string.add_trip_section_general)) }

            item {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.add_trip_field_title_label)) },
                    placeholder = { Text(stringResource(R.string.add_trip_field_title_placeholder)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = destination, onValueChange = { destination = it },
                    label = { Text(stringResource(R.string.add_trip_field_destination_label)) },
                    placeholder = { Text(stringResource(R.string.add_trip_field_destination_placeholder)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.add_trip_field_description_label)) },
                    placeholder = { Text(stringResource(R.string.add_trip_field_description_placeholder)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate, onValueChange = {},
                        label = { Text(stringResource(R.string.add_trip_field_start_date_label)) },
                        placeholder = { Text(stringResource(R.string.add_trip_field_date_placeholder)) },
                        modifier = Modifier.weight(1f), singleLine = true, readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showStartDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = stringResource(R.string.date_picker_cd_open),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = endDate, onValueChange = {},
                        label = { Text(stringResource(R.string.add_trip_field_end_date_label)) },
                        placeholder = { Text(stringResource(R.string.add_trip_field_date_placeholder)) },
                        modifier = Modifier.weight(1f), singleLine = true, readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = stringResource(R.string.date_picker_cd_open),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item { SectionTitle(text = stringResource(R.string.add_trip_section_budget)) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = flightCost, onValueChange = { flightCost = it },
                        label = { Text(stringResource(R.string.add_trip_field_flights_label)) },
                        placeholder = { Text(stringResource(R.string.add_trip_field_cost_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = accommodationCost, onValueChange = { accommodationCost = it },
                        label = { Text(stringResource(R.string.add_trip_field_accommodation_label)) },
                        placeholder = { Text(stringResource(R.string.add_trip_field_cost_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = foodCost, onValueChange = { foodCost = it },
                        label = { Text(stringResource(R.string.add_trip_field_food_label)) },
                        placeholder = { Text(stringResource(R.string.add_trip_field_cost_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = transportCost, onValueChange = { transportCost = it },
                        label = { Text(stringResource(R.string.add_trip_field_transport_label)) },
                        placeholder = { Text(stringResource(R.string.add_trip_field_cost_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activitiesCost, onValueChange = { activitiesCost = it },
                        label = { Text(stringResource(R.string.add_trip_field_activities_label)) },
                        placeholder = { Text(stringResource(R.string.add_trip_field_cost_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = otherCost, onValueChange = { otherCost = it },
                        label = { Text(stringResource(R.string.add_trip_field_other_label)) },
                        placeholder = { Text(stringResource(R.string.add_trip_field_cost_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item { SectionTitle(text = stringResource(R.string.add_trip_section_itinerary)) }

            if (itinerary.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.add_trip_itinerary_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                itinerary.forEachIndexed { index, day ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Only day number shown - no title field (MA-02)
                            Text(
                                text = stringResource(R.string.detail_day_label, day.dayNumber),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Raw text stored separately so spaces after commas work (MA-02)
                            OutlinedTextField(
                                value = placesTexts[day.dayNumber]
                                    ?: day.places.joinToString(", "),
                                onValueChange = { newText ->
                                    placesTexts = placesTexts + (day.dayNumber to newText)
                                    itinerary = itinerary.toMutableList().also {
                                        it[index] = it[index].copy(
                                            places = newText.split(",")
                                                .map { p -> p.trim() }
                                                .filter { p -> p.isNotBlank() }
                                        )
                                    }
                                },
                                label = {
                                    Text(stringResource(R.string.add_trip_day_places_label, day.dayNumber))
                                },
                                placeholder = {
                                    Text(stringResource(R.string.add_trip_day_places_placeholder))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }
                    if (index < itinerary.size - 1) {
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item { SectionTitle(text = stringResource(R.string.add_trip_section_categories)) }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableCategories.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategories.contains(key),
                            onClick = {
                                selectedCategories = if (selectedCategories.contains(key))
                                    selectedCategories - key
                                else selectedCategories + key
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item {
                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }

            item {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            if (title.isBlank() || destination.isBlank()) {
                                errorMessage = context.getString(R.string.add_trip_error_required_fields)
                                return@Button
                            }
                            if (accommodationCost.isBlank() || foodCost.isBlank() || transportCost.isBlank()) {
                                errorMessage = context.getString(R.string.add_trip_error_budget_required)
                                return@Button
                            }
                            if (selectedCategories.isEmpty()) {
                                errorMessage = context.getString(R.string.add_trip_error_category_required)
                                return@Button
                            }
                            val currentUser = FirebaseManager.currentUser
                            if (currentUser == null) {
                                errorMessage = context.getString(R.string.add_trip_error_not_logged_in)
                                return@Button
                            }
                            isLoading = true
                            errorMessage = ""

                            val flight = flightCost.toDoubleOrNull() ?: 0.0
                            val accommodation = accommodationCost.toDoubleOrNull() ?: 0.0
                            val food = foodCost.toDoubleOrNull() ?: 0.0
                            val transport = transportCost.toDoubleOrNull() ?: 0.0
                            val activities = activitiesCost.toDoubleOrNull() ?: 0.0
                            val other = otherCost.toDoubleOrNull() ?: 0.0

                            val trip = Trip(
                                title = title,
                                destination = destination,
                                description = description,
                                authorId = currentUser.uid,
                                authorEmail = currentUser.email ?: "",
                                startDate = startDate,
                                endDate = endDate,
                                flightCost = flight,
                                accommodationCost = accommodation,
                                foodCost = food,
                                transportCost = transport,
                                activitiesCost = activities,
                                otherCost = other,
                                totalBudget = flight + accommodation + food + transport + activities + other,
                                itinerary = itinerary,
                                categories = selectedCategories.toList()
                            )

                            if (selectedImageUri != null) {
                                val storageRef = FirebaseManager.storage.reference
                                    .child("covers/${currentUser.uid}/${System.currentTimeMillis()}.jpg")
                                storageRef.putFile(selectedImageUri!!)
                                    .addOnSuccessListener {
                                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                            FirebaseManager.firestore.collection("trips")
                                                .add(trip.copy(coverImageUrl = downloadUrl.toString()))
                                                .addOnSuccessListener {
                                                    NotificationHelper.showTripPublishedNotification(context, destination)
                                                    isLoading = false
                                                    tripSaved = true
                                                }
                                                .addOnFailureListener { exception ->
                                                    isLoading = false
                                                    errorMessage = exception.message
                                                        ?: context.getString(R.string.add_trip_error_saving)
                                                }
                                        }.addOnFailureListener { exception ->
                                            isLoading = false
                                            errorMessage = exception.message
                                                ?: context.getString(R.string.add_trip_error_uploading)
                                        }
                                    }.addOnFailureListener { exception ->
                                        isLoading = false
                                        errorMessage = exception.message
                                            ?: context.getString(R.string.add_trip_error_uploading)
                                    }
                            } else {
                                FirebaseManager.firestore.collection("trips").add(trip)
                                    .addOnSuccessListener {
                                        NotificationHelper.showTripPublishedNotification(context, destination)
                                        isLoading = false
                                        tripSaved = true
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        errorMessage = exception.message
                                            ?: context.getString(R.string.add_trip_error_saving)
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.add_trip_publish_button))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}