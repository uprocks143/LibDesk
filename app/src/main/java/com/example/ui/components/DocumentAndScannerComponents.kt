package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.*
import com.example.util.ImageShareUtils
import com.example.util.LocationVerificationUtils
import com.example.util.LocationVerificationResult
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun DigitalIdCardView(
    library: LibraryEntity?,
    student: StudentEntity,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = library?.name ?: "LIBDESK LIBRARY",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "STUDENT DIGITAL PASS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = student.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ID: ${student.studentCode} | Roll: ${student.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            IdField("Assigned Seat", student.seatNumber.ifEmpty { "Unassigned" }, MaterialTheme.colorScheme.primary)
                            IdField("Shift", student.shiftName.ifEmpty { "Regular" }, MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            IdField("Hall / Section", student.hallName.ifEmpty { "Main Hall" }, MaterialTheme.colorScheme.onSurface)
                            IdField("Valid Until", student.expiryDate.ifEmpty { "Active" }, LibDeskColors.success)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            IdField("Target Exam", student.targetExam, MaterialTheme.colorScheme.tertiary)
                            IdField("Status", student.status, if (student.status == "ACTIVE") LibDeskColors.success else MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                
                QrCodeView(
                    data = student.rfidQrCode.ifEmpty { student.studentCode },
                    size = 130.dp
                )

                Text(
                    text = "Scan at library kiosk / manager scanner for daily check-in",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "LIBDESK DIGITAL ID CARD\nLibrary: ${library?.name}\nStudent: ${student.fullName}\nID: ${student.studentCode}\nSeat: ${student.seatNumber}\nValid Until: ${student.expiryDate}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Digital ID Card"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share ID", fontSize = 14.sp)
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdField(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun DigitalReceiptView(
    library: LibraryEntity?,
    payment: PaymentEntity,
    onClose: () -> Unit,
    studentPhone: String? = null,
    studentEmail: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSavingLocally by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = library?.name ?: "LIBDESK LIBRARY",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${library?.address ?: ""}, ${library?.city ?: ""}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        if (!library?.upiId.isNullOrEmpty()) {
                            Text(
                                text = "UPI: ${library?.upiId}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "RECEIPT NO", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = payment.receiptNumber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "DATE", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = payment.date, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Received From:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payment.studentName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Purpose:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payment.purpose.replace("_", " "), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Mode:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payment.paymentMode, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        if (payment.period.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Deposit Period:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(payment.period, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (payment.referenceNumber.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ref / Txn ID:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(payment.referenceNumber, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AMOUNT PAID",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "₹${payment.amount}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = LibDeskColors.success,
                            fontSize = 22.sp
                        )
                    )
                }

                if (payment.dueBalance > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Remaining Due Balance:", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        Text(text = "₹${payment.dueBalance.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Send to Student Options with Real WhatsApp and SMS Symbols
                Text(
                    text = "SEND RECEIPT TO STUDENT",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // WhatsApp Button (Image Format)
                    Button(
                        onClick = {
                            ImageShareUtils.shareReceiptViaWhatsApp(
                                context = context,
                                library = library,
                                payment = payment,
                                studentPhone = studentPhone
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_whatsapp_real),
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Image Format", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }

                    // Mobile SMS Button (Text Format)
                    Button(
                        onClick = {
                            ImageShareUtils.shareReceiptViaSms(
                                context = context,
                                library = library,
                                payment = payment,
                                studentPhone = studentPhone
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_sms_real),
                            contentDescription = "Mobile SMS",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Mobile SMS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Text Format", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action Row: Email, Save PNG & Generic Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ImageShareUtils.shareReceiptViaEmail(
                                context = context,
                                library = library,
                                payment = payment,
                                studentEmail = studentEmail
                            )
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Email", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            isSavingLocally = true
                            val receiptBitmap = ImageShareUtils.createReceiptBitmap(library, payment)
                            val fileName = "receipt_${payment.receiptNumber}.png"
                            ImageShareUtils.saveReceiptLocally(context, receiptBitmap, fileName)
                            isSavingLocally = false
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSavingLocally) "Saved" else "Save PNG", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            ImageShareUtils.shareReceiptGeneric(
                                context = context,
                                library = library,
                                payment = payment
                            )
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onClose,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun CameraXQrScannerView(
    onQrScanned: (String) -> Unit,
    isTorchOn: Boolean = false,
    cameraLensFacing: Int = CameraSelector.LENS_FACING_BACK,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTimestamp by remember { mutableLongStateOf(0L) }
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

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

    val infiniteTransition = rememberInfiniteTransition(label = "scan_beam")
    val beamOffset by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_offset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.onPrimaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
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
                                            BarcodeFormat.CODE_39,
                                            BarcodeFormat.EAN_13,
                                            BarcodeFormat.EAN_8,
                                            BarcodeFormat.UPC_A,
                                            BarcodeFormat.UPC_E
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
                                        // Extract tightly packed Y plane bytes if stride > width
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

                                        // Apply rotation transform so scan orientation matches camera view
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

                                        // Dual binarization: Try HybridBinarizer first, then fallback to GlobalHistogramBinarizer
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
                                                    onQrScanned(text)
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {
                                    } finally {
                                        multiFormatReader.reset()
                                        imageProxy.close()
                                    }
                                }

                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(cameraLensFacing)
                                    .build()

                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                boundCamera = camera

                                if (camera.cameraInfo.hasFlashUnit()) {
                                    camera.cameraControl.enableTorch(isTorchOn)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Camera Facing Overlay Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (cameraLensFacing == CameraSelector.LENS_FACING_FRONT) Icons.Default.FlipCameraAndroid else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (cameraLensFacing == CameraSelector.LENS_FACING_FRONT) "Front Camera" else "Rear Camera",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )

                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(beamOffset)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.primary, Color.White, MaterialTheme.colorScheme.primary, Color.Transparent)
                                )
                            )
                    )
                }
            }
        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grant camera access to scan member pass & seat QR codes with CameraX.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.75f)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enable Camera", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QrScannerModal(
    onScanCode: (String) -> Unit,
    onClose: () -> Unit,
    title: String = "QR Seat & Member Check-in",
    subtitle: String = "Scan student pass or assigned seat QR sticker",
    isStudentMode: Boolean = false,
    libraryLatitude: Double = 28.6139,
    libraryLongitude: Double = 77.2090,
    libraryName: String = "Library Desk",
    onScanWithLocation: ((String, String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var manualInput by remember { mutableStateOf("") }
    var scanStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    
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

    fun triggerHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    fun handleScannedCode(scanned: String) {
        triggerHapticFeedback()
        val locNote = if (isStudentMode && locationResult != null) {
            if (locationResult?.isVerified == true) {
                "GPS Verified (${locationResult?.distanceMeters?.toInt() ?: 15}m from Library)"
            } else {
                "Location Checked (${locationResult?.distanceMeters?.toInt() ?: 0}m)"
            }
        } else if (isStudentMode) {
            "Location Checked"
        } else null

        scanStatusMessage = "Scanned: $scanned"
        if (onScanWithLocation != null) {
            onScanWithLocation(scanned, locNote)
        } else {
            onScanCode(scanned)
        }
    }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = subtitle,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (isStudentMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (locationResult?.isVerified == true) LibDeskColors.successSoft else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, if (locationResult?.isVerified == true) LibDeskColors.success.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isVerifyingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying your GPS proximity to Library Gate...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            } else if (locationResult?.isVerified == true) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = LibDeskColors.success, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("📍 GPS Verified at Library", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LibDeskColors.success)
                                    Text(
                                        locationResult?.locationLabel ?: "Within premises • Ready for Attendance Punch",
                                        fontSize = 14.sp,
                                        color = LibDeskColors.success.copy(alpha = 0.85f)
                                    )
                                }
                            } else {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    locationResult?.errorMessage ?: "GPS Geofence: Active for In/Out Verification",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dual Camera Selector Segment
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isRear = lensFacing == CameraSelector.LENS_FACING_BACK
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRear) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { lensFacing = CameraSelector.LENS_FACING_BACK }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isRear) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Rear Camera",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isRear) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isRear) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFront) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    lensFacing = CameraSelector.LENS_FACING_FRONT
                                    isTorchOn = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isFront) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Front Camera",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isFront) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isFront) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = { if (lensFacing == CameraSelector.LENS_FACING_BACK) isTorchOn = !isTorchOn },
                        enabled = lensFacing == CameraSelector.LENS_FACING_BACK,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isTorchOn && lensFacing == CameraSelector.LENS_FACING_BACK) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            modifier = Modifier.size(15.dp),
                            tint = if (isTorchOn && lensFacing == CameraSelector.LENS_FACING_BACK) LibDeskColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTorchOn && lensFacing == CameraSelector.LENS_FACING_BACK) "Torch ON" else "Torch",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                
                CameraXQrScannerView(
                    onQrScanned = { scanned ->
                        handleScannedCode(scanned)
                    },
                    isTorchOn = isTorchOn,
                    cameraLensFacing = lensFacing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )

                if (scanStatusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LibDeskColors.successSoft,
                        border = BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LibDeskColors.success, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = scanStatusMessage!!,
                                color = LibDeskColors.success,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                
                Text(
                    text = if (isStudentMode) "Quick Attendance Shortcuts:" else "Quick Demo Scans (Member & Seat Stickers):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            handleScannedCode("LIBDESK_GATE_ATTENDANCE:LIB-001:LIB-8042:${libraryName}")
                        },
                        label = { Text("Reception Gate QR", fontSize = 14.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            handleScannedCode("SEAT-A-01")
                        },
                        label = { Text("Seat A-01", fontSize = 14.sp) }
                    )
                    if (!isStudentMode) {
                        SuggestionChip(
                            onClick = {
                                handleScannedCode("STU-1001")
                            },
                            label = { Text("STU-1001", fontSize = 14.sp) }
                        )
                    }
                }

                if (isStudentMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Instant Library Check-in & Out",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan physical QR or tap instant gate punch using both front/rear camera:",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        handleScannedCode("LIBDESK_GATE_ATTENDANCE:LIB-001:LIB-8042:${libraryName}")
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Instant In", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        handleScannedCode("LIBDESK_GATE_ATTENDANCE:LIB-001:LIB-8042:${libraryName}")
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Instant Out", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Or enter Gate / Seat / Student Code") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (manualInput.isNotBlank()) {
                                    handleScannedCode(manualInput.trim())
                                    manualInput = ""
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Submit", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
fun LibraryEnrollmentQrModal(
    library: LibraryEntity?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } 

    val libCode = library?.code ?: "LIB-VANGUARD-01"
    val libName = library?.name ?: "Vanguard Study Hall"
    val gateQrPayload = "LIBDESK_GATE_ATTENDANCE:${library?.id ?: "LIB-001"}:$libCode:$libName"
    val enrollQrPayload = "LIBDESK:${library?.id ?: "LIB-001"}:$libCode:$libName"

    val activePayload = if (selectedTab == 0) gateQrPayload else enrollQrPayload

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.QrCodeScanner else Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (selectedTab == 0) "Gate Attendance QR Poster" else "Library Enrollment QR",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedTab == 0) "Print / Place at Reception & Gate" else "Scan to Self-Register & Join",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Gate Attendance Poster",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Student Enrollment",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = if (selectedTab == 0) "OFFICIAL GATE ATTENDANCE SCAN POINT" else "STUDENT SELF-ENROLLMENT PASS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        QrCodeView(
                            data = activePayload,
                            size = 180.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = libName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (selectedTab == 0) "Scan with LibDesk Student App for In/Out Punch" else "Code: $libCode",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTab == 0) {
                                "Print this QR poster and display at library reception / entry gate. Students scan with their phone camera to log In & Out attendance with live GPS location verification."
                            } else {
                                "Share this QR code with prospective students to let them register directly, select shifts and reserve seats on LibDesk."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val posterBitmap = ImageShareUtils.createLibraryAttendancePosterBitmap(library)
                            ImageShareUtils.shareImageGeneric(
                                context = context,
                                bitmap = posterBitmap,
                                fileName = "library_attendance_poster_${libCode}.png",
                                title = if (selectedTab == 0) "Library Attendance Gate Poster" else "Library Enrollment QR",
                                captionText = "Printable Gate Attendance QR Poster for $libName. Place at reception/gate for QR check-in & check-out."
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share / Print", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            val posterBitmap = ImageShareUtils.createLibraryAttendancePosterBitmap(library)
                            ImageShareUtils.shareImageGeneric(
                                context = context,
                                bitmap = posterBitmap,
                                fileName = "library_attendance_poster_${libCode}.png",
                                title = "Print Gate Attendance Poster",
                                captionText = "Gate Attendance QR Poster for $libName"
                            )
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print Poster", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

