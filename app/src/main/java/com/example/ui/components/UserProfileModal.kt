package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.SeatEntity
import com.example.data.local.entities.ShiftEntity
import com.example.data.local.entities.MembershipPlanEntity
import com.example.ui.theme.*


@Composable
fun UserProfileModal(
    currentRole: String, 
    library: LibraryEntity? = null,
    student: StudentEntity? = null,
    userName: String = "",
    userEmail: String = "",
    seats: List<SeatEntity> = emptyList(),
    shifts: List<ShiftEntity> = emptyList(),
    plans: List<MembershipPlanEntity> = emptyList(),
    onUpdateLibrary: (LibraryEntity) -> Unit = {},
    onUpdateStudent: (StudentEntity) -> Unit = {},
    isMandatory: Boolean = false,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    
    var libName by remember(library) { mutableStateOf(library?.name ?: "") }
    var ownerName by remember(library, userName) {
        mutableStateOf(library?.ownerName?.takeIf { it.isNotBlank() } ?: userName)
    }
    val initialOwnerPhone = remember(library) { splitCountryCodeAndPhone(library?.ownerPhone ?: "") }
    var ownerCountryCode by remember(library) { mutableStateOf(initialOwnerPhone.first) }
    var ownerPhone by remember(library) { mutableStateOf(initialOwnerPhone.second) }
    var ownerEmail by remember(library, userEmail) {
        mutableStateOf(library?.ownerEmail?.takeIf { it.isNotBlank() } ?: userEmail)
    }
    var upiId by remember(library) { mutableStateOf(library?.upiId ?: "") }
    var address by remember(library) { mutableStateOf(library?.address ?: "") }
    var city by remember(library) { mutableStateOf(library?.city ?: "") }
    var state by remember(library) { mutableStateOf(library?.state ?: "") }
    var pincode by remember(library) { mutableStateOf(library?.pincode ?: "") }
    var regNumber by remember(library) { mutableStateOf(library?.regNumber ?: "") }
    var latitudeStr by remember(library) { mutableStateOf(if ((library?.latitude ?: 0.0) != 0.0) library?.latitude.toString() else "") }
    var longitudeStr by remember(library) { mutableStateOf(if ((library?.longitude ?: 0.0) != 0.0) library?.longitude.toString() else "") }
    var stateDropdownExpanded by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    fun fetchAndSetCurrentLocation() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                Toast.makeText(context, "Location service not available", Toast.LENGTH_SHORT).show()
                return
            }

            isFetchingLocation = true
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                try {
                    val l = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                } catch (e: SecurityException) {
                    // Ignore individual provider exception
                }
            }

            if (bestLocation != null) {
                latitudeStr = String.format(java.util.Locale.US, "%.6f", bestLocation.latitude)
                longitudeStr = String.format(java.util.Locale.US, "%.6f", bestLocation.longitude)
                Toast.makeText(context, "Current location captured successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Turn on GPS to get current location", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not get location: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            isFetchingLocation = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchAndSetCurrentLocation()
        } else {
            Toast.makeText(context, "Location permission is needed to fetch current GPS coordinates", Toast.LENGTH_SHORT).show()
        }
    }

    val indianStates = remember {
        listOf(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
            "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
            "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
            "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
            "Uttar Pradesh", "Uttarakhand", "West Bengal",
            "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
            "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
        )
    }

    
    var studentFullName by remember(student, userName) {
        mutableStateOf(student?.fullName?.takeIf { it.isNotBlank() } ?: userName)
    }
    val initialStudentPhone = remember(student) { splitCountryCodeAndPhone(student?.mobile ?: "") }
    var studentCountryCode by remember(student) { mutableStateOf(initialStudentPhone.first) }
    var studentMobile by remember(student) { mutableStateOf(initialStudentPhone.second) }
    var studentEmail by remember(student, userEmail) {
        mutableStateOf(student?.email?.takeIf { it.isNotBlank() } ?: userEmail.ifBlank { "" })
    }
    var targetExam by remember(student) { mutableStateOf(student?.targetExam ?: "") }
    var courseClass by remember(student) { mutableStateOf(student?.courseClass ?: "") }
    var studentAddress by remember(student) { mutableStateOf(student?.address ?: "") }
    var parentName by remember(student) { mutableStateOf(student?.parentName ?: "") }
    val initialParentPhone = remember(student) { splitCountryCodeAndPhone(student?.parentMobile ?: "") }
    var parentCountryCode by remember(student) { mutableStateOf(initialParentPhone.first) }
    var parentMobile by remember(student) { mutableStateOf(initialParentPhone.second) }
    var emergencyContact by remember(student) { mutableStateOf(student?.parentMobile ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var profileFormError by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = !isMandatory) { onClose() }

    Dialog(
        onDismissRequest = { if (!isMandatory) onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (currentRole == "MANAGER") Icons.Default.AccountBalance else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (currentRole == "MANAGER") "Library & Manager Profile" else "Student Member Profile",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = if (currentRole == "MANAGER") "Manage institute details & billing" else "Update personal & study info",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            if (!isMandatory) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isMandatory) {
                        Surface(
                            color = LibDeskColors.warningSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = LibDeskColors.warning, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Welcome! Please complete your profile before continuing — this is a one-time step.",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (currentRole == "MANAGER") {

                            Text(
                                text = "INSTITUTE & STUDY HALL DETAILS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 0.5.sp
                            )

                            OutlinedTextField(
                                value = libName,
                                onValueChange = { libName = it },
                                label = { Text("Institute / Library Name") },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = library?.code ?: "VAN-101",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Library Code") },
                                    leadingIcon = { Icon(Icons.Default.Tag, null, tint = Color.Gray) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = regNumber,
                                    onValueChange = { regNumber = it },
                                    label = { Text("Reg / License No.") },
                                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                            
                            Text(
                                text = "MANAGER & CONTACT INFORMATION",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 0.5.sp
                            )

                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text("Owner / Manager Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            CountryCodePhoneField(
                                mobile = ownerPhone,
                                onMobileChange = { ownerPhone = it },
                                countryCode = ownerCountryCode,
                                onCountryCodeChange = { ownerCountryCode = it },
                                label = "Contact Phone / WhatsApp",
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = ownerEmail,
                                onValueChange = { ownerEmail = it },
                                label = { Text("Official Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it.trim() },
                                label = { Text("UPI ID for Fee Payments") },
                                leadingIcon = { Icon(Icons.Default.QrCode, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                placeholder = { Text("e.g. yourlib@upi") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                            
                            Text(
                                text = "CAMPUS LOCATION & ADDRESS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 0.5.sp
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Street Address / Landmark") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text("City") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.2f)
                                )

                                OutlinedTextField(
                                    value = pincode,
                                    onValueChange = { pincode = it.filter { c -> c.isDigit() } },
                                    label = { Text("PIN Code") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenuBox(
                                expanded = stateDropdownExpanded,
                                onExpandedChange = { stateDropdownExpanded = !stateDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = state,
                                    onValueChange = { state = it },
                                    readOnly = false,
                                    label = { Text("State / UT") },
                                    placeholder = { Text("Select or type State") },
                                    leadingIcon = { Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = stateDropdownExpanded,
                                    onDismissRequest = { stateDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 280.dp)
                                ) {
                                    val filteredStates = if (state.isBlank()) indianStates else indianStates.filter {
                                        it.contains(state, ignoreCase = true)
                                    }
                                    val listToShow = if (filteredStates.isEmpty()) indianStates else filteredStates
                                    listToShow.forEach { stateName ->
                                        DropdownMenuItem(
                                            text = { Text(stateName, fontSize = 14.sp) },
                                            onClick = {
                                                state = stateName
                                                stateDropdownExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }

                            // Optional Geo-Location Tagging for Google Maps Navigation
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Navigation,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Geo Location Tagging (Optional)",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "Students can find 1-tap Google Maps directions to this library",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick Location Actions: Use Current GPS Location & Pick/Search on Map
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(
                                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            },
                                            enabled = !isFetchingLocation,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (isFetchingLocation) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Fetching...", fontSize = 11.5.sp)
                                            } else {
                                                Icon(
                                                    Icons.Default.MyLocation,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text("Current Location", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val query = listOfNotNull(
                                                        libName.takeIf { it.isNotBlank() },
                                                        address.takeIf { it.isNotBlank() },
                                                        city.takeIf { it.isNotBlank() },
                                                        state.takeIf { it.isNotBlank() }
                                                    ).joinToString(", ")
                                                    val mapUri = if (query.isNotBlank()) {
                                                        Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                                                    } else {
                                                        Uri.parse("geo:0,0?q=Library")
                                                    }
                                                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                                    context.startActivity(mapIntent)
                                                    Toast.makeText(context, "Map opened! Drop/select pin or copy coordinates here.", Toast.LENGTH_LONG).show()
                                                } catch (e: Exception) {
                                                    val webUri = Uri.parse("https://maps.google.com")
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.Map,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Select on Map", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = latitudeStr,
                                            onValueChange = { latitudeStr = it },
                                            label = { Text("Latitude") },
                                            placeholder = { Text("e.g. 26.8467") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = longitudeStr,
                                            onValueChange = { longitudeStr = it },
                                            label = { Text("Longitude") },
                                            placeholder = { Text("e.g. 80.9462") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    val latVal = latitudeStr.toDoubleOrNull()
                                    val lngVal = longitudeStr.toDoubleOrNull()
                                    if (latVal != null && lngVal != null && latVal != 0.0 && lngVal != 0.0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(
                                            onClick = {
                                                try {
                                                    val mapUri = android.net.Uri.parse("geo:$latVal,$lngVal?q=$latVal,$lngVal(${android.net.Uri.encode(libName)})")
                                                    val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, mapUri)
                                                    context.startActivity(mapIntent)
                                                } catch (e: Exception) {
                                                    // Fallback to web maps
                                                    val webUri = android.net.Uri.parse("https://maps.google.com/?q=$latVal,$lngVal")
                                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, webUri))
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Test Pin on Google Maps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        } else {

                            if (student != null) {
                                val resolvedPlan = plans.find { it.id == student.planId } ?: plans.find { it.name.equals(student.planName, ignoreCase = true) }
                                val resolvedSeat = seats.find { it.id == student.seatId } ?: seats.find { it.seatNumber.equals(student.seatNumber, ignoreCase = true) }
                                val resolvedShift = shifts.find { it.id == student.shiftId } ?: shifts.find { it.name.equals(student.shiftName, ignoreCase = true) }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VerifiedUser,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "MEMBERSHIP & SEAT STATUS",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    letterSpacing = 0.4.sp
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Text(
                                                    text = student.status.uppercase(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), thickness = 0.8.dp)

                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "CURRENT PLAN",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = student.planName.ifBlank { resolvedPlan?.name ?: "Standard Study Plan" },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    softWrap = true,
                                                    maxLines = 2
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "EXPIRES ON",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = student.expiryDate.ifBlank { "Active" },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    softWrap = true
                                                )
                                            }
                                        }

                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "ASSIGNED SEAT",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (student.seatNumber.isNotBlank()) "Seat ${student.seatNumber}" else (resolvedSeat?.seatNumber?.let { "Seat $it" } ?: "Not Assigned"),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    softWrap = true
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "STUDY SHIFT",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = student.shiftName.ifBlank { resolvedShift?.name ?: "Full Day" },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    softWrap = true
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            
                            Text(
                                text = "PERSONAL & CONTACT DETAILS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 0.5.sp
                            )

                            OutlinedTextField(
                                value = studentFullName,
                                onValueChange = { studentFullName = it },
                                label = { Text("Student Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            CountryCodePhoneField(
                                mobile = studentMobile,
                                onMobileChange = { studentMobile = it },
                                countryCode = studentCountryCode,
                                onCountryCodeChange = { studentCountryCode = it },
                                label = "Mobile Number",
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = studentEmail,
                                onValueChange = { studentEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                            
                            Text(
                                text = "ACADEMIC & TARGET EXAM",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 0.5.sp
                            )

                            OutlinedTextField(
                                value = targetExam,
                                onValueChange = { targetExam = it },
                                label = { Text("Target Exam (e.g. UPSC, NEET, Banking)") },
                                leadingIcon = { Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = courseClass,
                                onValueChange = { courseClass = it },
                                label = { Text("Current Course / Qualification") },
                                leadingIcon = { Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = studentAddress,
                                onValueChange = { studentAddress = it },
                                label = { Text("Residential Address / Hostel") },
                                leadingIcon = { Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                            
                            Text(
                                text = "GUARDIAN & EMERGENCY CONTACT",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 0.5.sp
                            )

                            OutlinedTextField(
                                value = parentName,
                                onValueChange = { parentName = it },
                                label = { Text("Guardian Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            CountryCodePhoneField(
                                mobile = parentMobile,
                                onMobileChange = { parentMobile = it },
                                countryCode = parentCountryCode,
                                onCountryCodeChange = { parentCountryCode = it },
                                label = "Guardian Phone",
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (profileFormError != null) {
                            Text(
                                text = profileFormError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!isMandatory) {
                                OutlinedButton(
                                    onClick = onClose,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Button(
                                onClick = {
                                    if (currentRole == "MANAGER") {
                                        val fullOwnerPhone = combineCountryCodeAndPhone(ownerCountryCode, ownerPhone).trim()
                                        if (libName.isBlank() || ownerName.isBlank() || fullOwnerPhone.isBlank() ||
                                            address.isBlank() || city.isBlank() || state.isBlank() || upiId.isBlank()
                                        ) {
                                            profileFormError = "Please fill in all required fields (name, address, city, state, and UPI ID) before saving."
                                            return@Button
                                        }
                                        profileFormError = null
                                        isSaving = true
                                        val curLib = library ?: LibraryEntity(
                                            id = java.util.UUID.randomUUID().toString(),
                                            name = libName,
                                            code = "LIB-${(1000..9999).random()}"
                                        )
                                        val updated = curLib.copy(
                                            name = libName.trim(),
                                            regNumber = regNumber.trim(),
                                            ownerName = ownerName.trim(),
                                            ownerPhone = fullOwnerPhone,
                                            ownerEmail = ownerEmail.trim(),
                                            upiId = upiId.trim(),
                                            address = address.trim(),
                                            city = city.trim(),
                                            state = state.trim(),
                                            pincode = pincode.trim(),
                                            latitude = latitudeStr.trim().toDoubleOrNull() ?: 0.0,
                                            longitude = longitudeStr.trim().toDoubleOrNull() ?: 0.0,
                                            phone = fullOwnerPhone,
                                            email = ownerEmail.trim(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        onUpdateLibrary(updated)
                                        onClose()
                                    } else {
                                        val fullStudentPhone = combineCountryCodeAndPhone(studentCountryCode, studentMobile).trim()
                                        if (studentFullName.isBlank() || fullStudentPhone.isBlank()) {
                                            profileFormError = "Please fill in your name and mobile number before saving."
                                            return@Button
                                        }
                                        profileFormError = null
                                        isSaving = true
                                        val fullParentPhone = combineCountryCodeAndPhone(parentCountryCode, parentMobile).trim()
                                        val curSt = student ?: StudentEntity(
                                            id = "STU-${System.currentTimeMillis()}",
                                            libraryId = library?.id ?: "",
                                            studentCode = "STU-${(1000..9999).random()}",
                                            fullName = studentFullName,
                                            mobile = fullStudentPhone
                                        )
                                        val updated = curSt.copy(
                                            fullName = studentFullName.trim(),
                                            mobile = fullStudentPhone,
                                            email = studentEmail.trim(),
                                            targetExam = targetExam.trim(),
                                            courseClass = courseClass.trim(),
                                            address = studentAddress.trim(),
                                            parentName = parentName.trim(),
                                            parentMobile = fullParentPhone
                                        )
                                        onUpdateStudent(updated)
                                        onClose()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save Profile",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
