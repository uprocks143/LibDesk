package com.example.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.entities.StudentEntity
import com.example.ui.components.generateQrBitmap
import com.example.ui.theme.LibDeskColors
import com.example.util.LocationVerificationResult
import com.example.util.LocationVerificationUtils
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

data class SessionScanRecord(
    val id: String = UUID.randomUUID().toString(),
    val studentName: String,
    val studentCode: String,
    val seatNumber: String,
    val mode: String, // "IN" or "OUT"
    val timestamp: String,
    val isSuccess: Boolean,
    val message: String
)

/**
 * CameraX & ZXing QR Code Scanner for quick seat check-in and check-out.
 * Supports Student Self-Scan (allocated seat QR) and Librarian Desk ID Rapid Scanning.
 */
@Composable
fun SeatCheckInScannerModal(
    allocatedSeatNumber: String = "",
    allocatedHallName: String = "Main Study Hall",
    allocatedShiftName: String = "Full Day Shift",
    studentName: String = "Member",
    isStudentMode: Boolean = true,
    isCurrentlyCheckedIn: Boolean = false,
    todayCheckInTime: String = "",
    libraryLatitude: Double = 28.6139,
    libraryLongitude: Double = 77.2090,
    libraryName: String = "Library Desk",
    studentsList: List<StudentEntity> = emptyList(),
    onScanCode: (String, String?, ((Boolean, String) -> Unit)?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var manualInput by remember { mutableStateOf("") }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isVerifying by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var autoScanNext by remember { mutableStateOf(!isStudentMode) }
    var showSeatQrStickerModal by remember { mutableStateOf(false) }
    val sessionScans = remember { mutableStateListOf<SessionScanRecord>() }

    // Camera Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Location Verification for Student Mode
    var locationResult by remember { mutableStateOf<LocationVerificationResult?>(null) }
    var isVerifyingLocation by remember { mutableStateOf(isStudentMode) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isVerifyingLocation = true
            LocationVerificationUtils.verifyStudentLocation(context, libraryLatitude, libraryLongitude) { result ->
                locationResult = result
                isVerifyingLocation = false
            }
        } else {
            isVerifyingLocation = false
        }
    }

    LaunchedEffect(isStudentMode) {
        if (isStudentMode) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                isVerifyingLocation = true
                LocationVerificationUtils.verifyStudentLocation(context, libraryLatitude, libraryLongitude) { result ->
                    locationResult = result
                    isVerifyingLocation = false
                }
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    fun playAudioFeedback(success: Boolean) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            if (success) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } else {
                toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 200)
            }
        } catch (_: Exception) {}
    }

    fun triggerHapticFeedback(success: Boolean) {
        try {
            val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (success) {
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), -1)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    fun submitCodeForCheckIn(code: String) {
        if (code.isBlank() || isVerifying) return
        isVerifying = true
        lastResult = null

        val locNote = if (isStudentMode && locationResult != null) {
            if (locationResult?.isVerified == true) {
                "GPS Verified (${locationResult?.distanceMeters?.toInt() ?: 15}m from Library)"
            } else {
                "Location Checked (${locationResult?.distanceMeters?.toInt() ?: 0}m)"
            }
        } else if (isStudentMode) {
            "Location Checked"
        } else null

        val cleanCode = code.trim().removePrefix("LIBDESK:STUDENT:").removePrefix("STUDENT:").removePrefix("STU:")
        onScanCode(code.trim(), locNote) { success, message ->
            isVerifying = false
            lastResult = Pair(success, message)
            playAudioFeedback(success)
            triggerHapticFeedback(success)

            if (success) {
                val matched = studentsList.find {
                    it.id.equals(cleanCode, ignoreCase = true) ||
                    it.studentCode.equals(cleanCode, ignoreCase = true) ||
                    it.mobile.equals(cleanCode, ignoreCase = true) ||
                    it.rfidQrCode.equals(cleanCode, ignoreCase = true) ||
                    it.seatNumber.equals(cleanCode, ignoreCase = true)
                }
                val sName = matched?.fullName ?: if (isStudentMode) studentName else "Student Member"
                val sCode = matched?.studentCode ?: cleanCode
                val sSeat = matched?.seatNumber?.ifBlank { allocatedSeatNumber.ifBlank { "General Desk" } } ?: "General Desk"
                val sMode = if (message.contains("Checked OUT", ignoreCase = true)) "OUT" else "IN"
                val sTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                sessionScans.add(
                    0,
                    SessionScanRecord(
                        studentName = sName,
                        studentCode = sCode,
                        seatNumber = sSeat,
                        mode = sMode,
                        timestamp = sTime,
                        isSuccess = true,
                        message = message
                    )
                )
                if (sessionScans.size > 20) {
                    sessionScans.removeAt(sessionScans.lastIndex)
                }

                // In continuous/auto-scan mode for librarian, clear result after 2.5s to scan next ID
                if (!isStudentMode && autoScanNext) {
                    coroutineScope.launch {
                        delay(2500L)
                        if (lastResult?.first == true) {
                            lastResult = null
                        }
                    }
                }
            }
        }
    }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrentlyCheckedIn) LibDeskColors.warningSoft
                                    else if (isStudentMode) MaterialTheme.colorScheme.primaryContainer
                                    else LibDeskColors.warningSoft
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyCheckedIn) Icons.Default.Logout
                                else if (isStudentMode) Icons.Default.QrCodeScanner
                                else Icons.Default.Badge,
                                contentDescription = null,
                                tint = if (isCurrentlyCheckedIn) LibDeskColors.warning
                                else if (isStudentMode) MaterialTheme.colorScheme.onPrimaryContainer
                                else LibDeskColors.warning,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isStudentMode) {
                                    if (isCurrentlyCheckedIn) "Seat Check-Out Scanner" else "Seat Check-In Scanner"
                                } else "Librarian Rapid QR Scanner",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isStudentMode) {
                                    if (isCurrentlyCheckedIn) "Scan allocated seat QR sticker to Check OUT"
                                    else "Scan allocated seat QR sticker to Check IN"
                                } else "Scan student ID card, QR badge, or seat pass",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_seat_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close scanner",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Attendance Status & Next Action Banner
                if (isStudentMode) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCurrentlyCheckedIn) LibDeskColors.successSoft.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.2.dp,
                            if (isCurrentlyCheckedIn) LibDeskColors.success.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrentlyCheckedIn) LibDeskColors.success else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isCurrentlyCheckedIn) "STATUS: CURRENTLY CHECKED IN" else "STATUS: NOT CHECKED IN",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        color = if (isCurrentlyCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (allocatedSeatNumber.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Seat $allocatedSeatNumber",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isCurrentlyCheckedIn) {
                                    "Checked in since ${todayCheckInTime.ifBlank { "earlier today" }}. Point camera at your desk QR sticker to Check OUT and log your study hours."
                                } else {
                                    "Point camera at the unique QR code sticker affixed on Seat #$allocatedSeatNumber in $allocatedHallName to punch attendance and check in."
                                },
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )

                            // Quick Action Chips (Test scan & View QR Sticker)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (allocatedSeatNumber.isNotBlank()) {
                                    AssistChip(
                                        onClick = {
                                            submitCodeForCheckIn("LIBDESK:SEAT:$allocatedSeatNumber")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = LibDeskColors.warning,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (isCurrentlyCheckedIn) "⚡ Test Check-Out Seat $allocatedSeatNumber"
                                                else "⚡ Test Check-In Seat $allocatedSeatNumber",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("test_scan_allocated_seat_chip")
                                    )
                                }

                                AssistChip(
                                    onClick = { showSeatQrStickerModal = true },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "View Seat QR Sticker",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("view_seat_qr_sticker_chip")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Librarian Mode Controls & Rapid Queue Toggle
                if (!isStudentMode) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LibDeskColors.successSoft
                                ) {
                                    Text(
                                        text = "LIBRARY DESK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LibDeskColors.success,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Auto-Scan Next Student",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Switch(
                                checked = autoScanNext,
                                onCheckedChange = { autoScanNext = it },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Camera Controls Bar (Torch & Switch Camera)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CameraX Optical Scanner",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                cameraLensFacing = if (cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("switch_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch", fontSize = 11.5.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                if (cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                                    isTorchOn = !isTorchOn
                                }
                            },
                            enabled = cameraLensFacing == CameraSelector.LENS_FACING_BACK,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("toggle_torch_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flashlight",
                                modifier = Modifier.size(15.dp),
                                tint = if (isTorchOn) LibDeskColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTorchOn) "Torch ON" else "Torch", fontSize = 11.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Viewfinder View
                if (hasCameraPermission) {
                    SeatCameraViewfinderView(
                        cameraLensFacing = cameraLensFacing,
                        isTorchOn = isTorchOn,
                        onCodeScanned = { scanned ->
                            submitCodeForCheckIn(scanned)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Camera Permission Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Camera access is needed to scan your allocated seat QR sticker.",
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                // GPS Location Verification Status
                if (isStudentMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (locationResult?.isVerified == true) LibDeskColors.successSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (locationResult?.isVerified == true) LibDeskColors.success.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isVerifyingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Checking GPS proximity to $libraryName...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (locationResult?.isVerified == true) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = LibDeskColors.success,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "📍 GPS Verified at Library (${locationResult?.distanceMeters?.toInt() ?: 10}m)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = LibDeskColors.success
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LocationSearching,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Location verification available",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Processing Indicator
                AnimatedVisibility(
                    visible = isVerifying,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Validating seat allocation with cloud server...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Result Card (Success or Rule-Based Rejection)
                lastResult?.let { (success, message) ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val isCheckOut = message.contains("Checked OUT", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (success) {
                            if (isCheckOut) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                            else LibDeskColors.successSoft
                        } else MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(
                            1.dp,
                            if (success) {
                                if (isCheckOut) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                else LibDeskColors.success.copy(alpha = 0.4f)
                            } else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (success) {
                                        if (isCheckOut) Icons.Default.Logout else Icons.Default.CheckCircle
                                    } else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (success) {
                                        if (isCheckOut) MaterialTheme.colorScheme.secondary else LibDeskColors.success
                                    } else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (success) {
                                        if (isCheckOut) "Study Session Logged & Checked OUT"
                                        else "Seat Verified & Checked IN"
                                    } else "Rule Validation Blocked",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (success) {
                                        if (isCheckOut) MaterialTheme.colorScheme.onSecondaryContainer
                                        else LibDeskColors.success
                                    } else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = message,
                                fontSize = 13.sp,
                                color = if (success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (success) {
                                    if (!isStudentMode) {
                                        OutlinedButton(
                                            onClick = { lastResult = null },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Scan Next")
                                        }
                                    }
                                    Button(
                                        onClick = onClose,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCheckOut) MaterialTheme.colorScheme.secondary else LibDeskColors.success,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("confirm_checkin_done_button")
                                    ) {
                                        Text(if (isStudentMode) "Done" else "Finish Session")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { lastResult = null },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("retry_scan_button")
                                    ) {
                                        Text("Scan Again")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Manual Desk / Student Code Input Fallback
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text(if (isStudentMode) "Seat Number or Code (e.g. $allocatedSeatNumber)" else "Student Code or Seat") },
                    placeholder = {
                        Text(
                            if (isStudentMode) {
                                if (allocatedSeatNumber.isNotBlank()) allocatedSeatNumber else "A-12"
                            } else {
                                "Code, mobile, or seat"
                            }
                        )
                    },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (manualInput.isNotBlank()) {
                                    submitCodeForCheckIn(manualInput.trim())
                                    manualInput = ""
                                }
                            },
                            modifier = Modifier.testTag("submit_manual_seat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Submit code",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_seat_input")
                )

                // Librarian Recent Queue Activity Feed
                if (!isStudentMode && sessionScans.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Queue Activity",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${sessionScans.size} logged",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            sessionScans.take(6).forEach { scan ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (scan.mode == "IN") LibDeskColors.successSoft else LibDeskColors.warningSoft
                                            ) {
                                                Text(
                                                    text = if (scan.mode == "IN") "CHECK-IN" else "CHECK-OUT",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (scan.mode == "IN") LibDeskColors.success else LibDeskColors.warning,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = scan.studentName,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "ID: ${scan.studentCode} • Seat: ${scan.seatNumber}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = scan.timestamp,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to View/Print Seat QR Code Sticker
    if (showSeatQrStickerModal) {
        SeatQrStickerDialog(
            seatNumber = allocatedSeatNumber.ifBlank { "A-12" },
            hallName = allocatedHallName,
            shiftName = allocatedShiftName,
            libraryName = libraryName,
            studentName = studentName,
            onClose = { showSeatQrStickerModal = false }
        )
    }
}

/**
 * Real-time CameraX Viewfinder with ZXing Barcode Decoder, Zoom Controls, Tap-to-Focus, and Reticle.
 */
@Composable
private fun SeatCameraViewfinderView(
    cameraLensFacing: Int,
    isTorchOn: Boolean,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTimestamp by remember { mutableLongStateOf(0L) }
    var currentZoom by remember { mutableFloatStateOf(1.0f) }

    DisposableEffect(lifecycleOwner, cameraLensFacing) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
            } catch (_: Exception) {}
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isTorchOn, boundCamera) {
        boundCamera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(isTorchOn)
            }
        }
    }

    // Laser Beam Animation
    val infiniteTransition = rememberInfiniteTransition(label = "seat_scan_beam")
    val beamOffset by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seat_beam_offset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        key(cameraLensFacing) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderInstance = cameraProvider

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val multiFormatReader = MultiFormatReader().apply {
                                val hints = mapOf<DecodeHintType, Any>(
                                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                                        BarcodeFormat.QR_CODE,
                                        BarcodeFormat.DATA_MATRIX,
                                        BarcodeFormat.CODE_128,
                                        BarcodeFormat.CODE_39,
                                        BarcodeFormat.EAN_13,
                                        BarcodeFormat.UPC_A
                                    ),
                                    DecodeHintType.TRY_HARDER to true
                                )
                                setHints(hints)
                            }

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val plane = imageProxy.planes[0]
                                val buffer = plane.buffer
                                val rowStride = plane.rowStride
                                val width = imageProxy.width
                                val height = imageProxy.height
                                val rotation = imageProxy.imageInfo.rotationDegrees

                                try {
                                    val yBytes: ByteArray
                                    if (rowStride == width) {
                                        yBytes = ByteArray(buffer.remaining())
                                        buffer.get(yBytes)
                                    } else {
                                        yBytes = ByteArray(width * height)
                                        for (row in 0 until height) {
                                            buffer.position(row * rowStride)
                                            buffer.get(yBytes, row * width, width)
                                        }
                                    }

                                    // Match rotation to camera preview
                                    val rotatedData: ByteArray
                                    val rotatedWidth: Int
                                    val rotatedHeight: Int
                                    when (rotation) {
                                        90 -> {
                                            rotatedData = ByteArray(yBytes.size)
                                            for (y in 0 until height) {
                                                for (x in 0 until width) {
                                                    rotatedData[x * height + height - y - 1] = yBytes[x + y * width]
                                                }
                                            }
                                            rotatedWidth = height
                                            rotatedHeight = width
                                        }
                                        270 -> {
                                            rotatedData = ByteArray(yBytes.size)
                                            for (y in 0 until height) {
                                                for (x in 0 until width) {
                                                    rotatedData[(width - x - 1) * height + y] = yBytes[x + y * width]
                                                }
                                            }
                                            rotatedWidth = height
                                            rotatedHeight = width
                                        }
                                        180 -> {
                                            rotatedData = ByteArray(yBytes.size)
                                            for (i in yBytes.indices) {
                                                rotatedData[yBytes.size - i - 1] = yBytes[i]
                                            }
                                            rotatedWidth = width
                                            rotatedHeight = height
                                        }
                                        else -> {
                                            rotatedData = yBytes
                                            rotatedWidth = width
                                            rotatedHeight = height
                                        }
                                    }

                                    val source = PlanarYUVLuminanceSource(
                                        rotatedData, rotatedWidth, rotatedHeight, 0, 0, rotatedWidth, rotatedHeight, false
                                    )

                                    var result: Result? = null
                                    try {
                                        result = multiFormatReader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
                                    } catch (_: NotFoundException) {
                                        try {
                                            result = multiFormatReader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
                                        } catch (_: Exception) {
                                            try {
                                                // Inverted contrast check (white on dark)
                                                result = multiFormatReader.decodeWithState(BinaryBitmap(HybridBinarizer(source.invert())))
                                            } catch (_: Exception) {}
                                        }
                                    } catch (_: Exception) {}

                                    val text = result?.text
                                    if (!text.isNullOrBlank()) {
                                        val now = System.currentTimeMillis()
                                        if (text != lastScannedCode || now - lastScannedTimestamp > 2500) {
                                            lastScannedCode = text
                                            lastScannedTimestamp = now
                                            previewView.post {
                                                onCodeScanned(text)
                                            }
                                        }
                                    }
                                } catch (_: Exception) {
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            val selector = CameraSelector.Builder().requireLensFacing(cameraLensFacing).build()
                            cameraProvider.unbindAll()
                            boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(boundCamera) {
                        detectTapGestures { offset ->
                            boundCamera?.let { cam ->
                                val factory = SurfaceOrientedMeteringPointFactory(size.width.toFloat(), size.height.toFloat())
                                val point = factory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                cam.cameraControl.startFocusAndMetering(action)
                            }
                        }
                    }
            )
        }

        // Viewfinder Darkening Overlay with Center Cutout
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
        ) {
            // Corner Reticle Indicators
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )

            // Animated Laser Scan Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 200.dp * beamOffset)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary,
                                Color.White,
                                MaterialTheme.colorScheme.primary,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Instruction Text above targeting box
        Text(
            text = "Align Seat QR code inside frame",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // Zoom Ratio Quick Controls (1x, 2x, 3x)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(1.0f, 2.0f, 3.0f).forEach { zoom ->
                val isSelected = currentZoom == zoom
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            currentZoom = zoom
                            boundCamera?.cameraControl?.setZoomRatio(zoom)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${zoom.toInt()}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Printable & Scannable Desk QR Sticker Modal.
 * Shows the unique QR code for the seat so managers can print stickers or students can preview it.
 */
@Composable
fun SeatQrStickerDialog(
    seatNumber: String,
    hallName: String,
    shiftName: String,
    libraryName: String,
    studentName: String,
    onClose: () -> Unit
) {
    val qrPayload = "LIBDESK:SEAT:$seatNumber"
    val qrBitmap = remember(qrPayload) {
        generateQrBitmap(qrPayload, 512)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Desk QR Code Sticker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Affix on desk surface for instant check-in/out",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High Contrast QR Card (Sticker Layout)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, Color(0xFF0F172A)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Sticker Top Banner
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Text(
                                text = libraryName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "SEAT #$seatNumber",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "$hallName • $shiftName",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // High-contrast QR bitmap
                        qrBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Seat QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "SCAN WITH LIBDESK APP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Scan at Entry to Check IN • Scan at Exit to Check OUT",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Payload: $qrPayload",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ready to Scan")
                    }
                }
            }
        }
    }
}
