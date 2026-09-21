package com.example.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CountryCodePhoneField
import com.example.ui.components.combineCountryCodeAndPhone
import com.example.ui.theme.*

@Composable
fun OnboardingWizardScreen(
    onCompleteSetup: (String, String, String, String, String, String, String, String, String, String, String, Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) }

    
    var ownerName by remember { mutableStateOf("") }
    var ownerCountryCode by remember { mutableStateOf("+91") }
    var ownerPhone by remember { mutableStateOf("") }
    var ownerEmail by remember { mutableStateOf("") }

    
    var libraryName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("Delhi") }
    var pincode by remember { mutableStateOf("110009") }

    
    var upiId by remember { mutableStateOf("") }
    var upiPayeeName by remember { mutableStateOf("") }
    var initialHallName by remember { mutableStateOf("Main Reading Hall") }
    var initialSeatCountText by remember { mutableStateOf("24") }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (step > 1) step-- else onCancel()
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "Register Library",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepBadge(stepNumber = 1, label = "Owner", isCurrent = step == 1, isCompleted = step > 1)
                    Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    StepBadge(stepNumber = 2, label = "Library", isCurrent = step == 2, isCompleted = step > 2)
                    Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    StepBadge(stepNumber = 3, label = "Seats", isCurrent = step == 3, isCompleted = step > 3)
                }
            }

            
            when (step) {
                1 -> {

                    item {
                        Text(
                            text = "Library Owner & Admin Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Enter the primary administrator contact details for system access and receipts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner / Manager Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        CountryCodePhoneField(
                            mobile = ownerPhone,
                            onMobileChange = { ownerPhone = it },
                            countryCode = ownerCountryCode,
                            onCountryCodeChange = { ownerCountryCode = it },
                            label = "Mobile Phone Number *",
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = ownerEmail,
                            onValueChange = { ownerEmail = it },
                            label = { Text("Official Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (ownerName.isNotBlank() && ownerPhone.isNotBlank()) step = 2
                            },
                            enabled = ownerName.isNotBlank() && ownerPhone.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Next: Library Identity →")
                        }
                    }
                }
                2 -> {

                    item {
                        Text(
                            text = "Library Name & Physical Address",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "This appears on student digital ID cards and fee receipts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = libraryName,
                            onValueChange = { libraryName = it },
                            label = { Text("Library / Study Hall Name *") },
                            leadingIcon = { Icon(Icons.Default.LocalLibrary, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Street Address / Landmark") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City *") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pincode,
                                onValueChange = { pincode = it },
                                label = { Text("Pincode") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    if (libraryName.isNotBlank() && city.isNotBlank()) step = 3
                                },
                                enabled = libraryName.isNotBlank() && city.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("Next: Seats & UPI →")
                            }
                        }
                    }
                }
                3 -> {

                    item {
                        Text(
                            text = "Payment UPI & Initial Reading Hall",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Configure instant student fee collections and auto-generate your first batch of seats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID (e.g. yourname@upi) *") },
                            leadingIcon = { Icon(Icons.Default.QrCode, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = upiPayeeName,
                            onValueChange = { upiPayeeName = it },
                            label = { Text("Payee Name (displayed on GPay / PhonePe)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = initialHallName,
                                onValueChange = { initialHallName = it },
                                label = { Text("Main Hall Name") },
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = initialSeatCountText,
                                onValueChange = { initialSeatCountText = it },
                                label = { Text("Seats") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { step = 2 },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val count = initialSeatCountText.toIntOrNull() ?: 24
                                    val fullPhone = combineCountryCodeAndPhone(ownerCountryCode, ownerPhone)
                                    onCompleteSetup(
                                        libraryName,
                                        ownerName,
                                        fullPhone,
                                        ownerEmail,
                                        address,
                                        city,
                                        state,
                                        pincode,
                                        upiId,
                                        upiPayeeName.ifEmpty { libraryName },
                                        initialHallName,
                                        count
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("Launch Library 🚀")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBadge(stepNumber: Int, label: String, isCurrent: Boolean, isCompleted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) LibDeskColors.success
                    else if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = "$stepNumber",
                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
