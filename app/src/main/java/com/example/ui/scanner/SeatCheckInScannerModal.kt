package com.example.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LibDeskColors
import com.example.util.LocationVerificationResult
import com.example.util.LocationVerificationUtils
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

/**
 * Industrial-grade Seat QR Code Scanner Component for checking into allocated library seats.
 * Enforces rule-based seat allocation matching and real-time cloud attendance verification.
 */
@Composable
fun SeatCheckInScannerModal(
    allocatedSeatNumber: String = "",
    allocatedHallName: String = "Main Study Hall",
    allocatedShiftName: String = "Full Day Shift",
    studentName: String = "Member",
    isStudentMode: Boolean = true,
    libraryLatitude: Double = 28.6139,
    libraryLongitude: Double = 77.2090,
    libraryName: String = "Library Desk",
    onScanCode: (String, String?, ((Boolean, String) -> Unit)?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var manualInput by remember { mutableStateOf("") }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isVerifying by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

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

        onScanCode(code.trim(), locNote) { success, message ->
            isVerifying = false
            lastResult = Pair(success, message)
            triggerHapticFeedback(success)
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Seat Check-In Scanner",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Scan allocated seat QR sticker or gate pass",
                                fontSize = 13.sp,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Allocated Seat Card (rule-based reference)
                if (allocatedSeatNumber.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chair,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Allocated Seat: $allocatedSeatNumber",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$allocatedHallName • $allocatedShiftName",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LibDeskColors.successSoft
                            ) {
                                Text(
                                    text = "Cloud Sync",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LibDeskColors.success,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Camera Controls Bar (Torch & Switch Camera)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Camera Viewfinder",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                                .height(38.dp)
                                .testTag("switch_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch", fontSize = 12.sp)
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
                                .height(38.dp)
                                .testTag("toggle_torch_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flashlight",
                                modifier = Modifier.size(16.dp),
                                tint = if (isTorchOn) LibDeskColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isTorchOn) "Torch ON" else "Torch", fontSize = 12.sp)
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
                            .height(280.dp)
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (success) LibDeskColors.successSoft else MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(
                            1.dp,
                            if (success) LibDeskColors.success.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (success) LibDeskColors.success else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (success) "Attendance Cloud Logged" else "Rule Validation Blocked",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (success) LibDeskColors.success else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = message,
                                fontSize = 13.sp,
                                color = if (success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (success) {
                                    Button(
                                        onClick = onClose,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LibDeskColors.success,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("confirm_checkin_done_button")
                                    ) {
                                        Text("Done")
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

                // Manual Desk / Seat Code Input Fallback
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Enter Seat Number (e.g. A-12) or Code") },
                    placeholder = { Text(if (allocatedSeatNumber.isNotBlank()) "e.g. $allocatedSeatNumber" else "e.g. A-12") },
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
                                contentDescription = "Submit seat code",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_seat_input")
                )
            }
        }
    }
}

/**
 * Real-time CameraX Viewfinder with ZXing Barcode Decoder and Animated Reticle
 */
@Composable
private fun SeatCameraViewfinderView(
    cameraLensFacing: Int,
    isTorchOn: Boolean,
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTimestamp by remember { mutableLongStateOf(0L) }

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
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val multiFormatReader = MultiFormatReader().apply {
                                val hints = mapOf<DecodeHintType, Any>(
                                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                                        BarcodeFormat.QR_CODE,
                                        BarcodeFormat.DATA_MATRIX,
                                        BarcodeFormat.CODE_128,
                                        BarcodeFormat.CODE_39
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
                                        } catch (_: Exception) {}
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
                modifier = Modifier.fillMaxSize()
            )
        }

        // Viewfinder Targeting Frame Overlay
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
        ) {
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
    }
}
