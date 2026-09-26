package com.example.util.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper utility class to generate shareable UPI QR codes, UPI deep links,
 * and facilitate manual subscription payments and WhatsApp receipt sharing.
 */
object UpiPaymentHelper {

    /**
     * Constructs a standard NPCI compliant UPI Deep Link URI.
     * Example: upi://pay?pa=libdesk.billing@upi&pn=LibDesk%20SaaS&am=499.00&cu=INR&tn=Sub_Plan&tr=TXN_1234
     */
    fun buildUpiDeepLink(
        upiId: String,
        payeeName: String,
        amount: Double,
        note: String,
        transactionRef: String = "TXN_${System.currentTimeMillis()}"
    ): String {
        val cleanUpi = upiId.trim()
        val formattedPa = if (!cleanUpi.contains("@") && cleanUpi.length == 10 && cleanUpi.all { it.isDigit() }) {
            "$cleanUpi@upi"
        } else {
            cleanUpi
        }
        val cleanName = Uri.encode(payeeName.trim().ifBlank { "LibDesk Subscriptions" })
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val cleanNote = Uri.encode(note.trim().ifBlank { "LibDesk Subscription Plan" })

        return "upi://pay?pa=$formattedPa&pn=$cleanName&am=$formattedAmount&cu=INR&tn=$cleanNote&tr=$transactionRef"
    }

    /**
     * Generates a QR code Bitmap for the UPI Deep Link using the ZXing library.
     */
    fun generateUpiQrBitmap(
        upiDeepLink: String,
        size: Int = 600,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            }
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(upiDeepLink, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback generated bitmap with message in case of encoding anomaly
            createFallbackBitmap(size, "UPI QR Code: $upiDeepLink")
        }
    }

    /**
     * Generates a branded, high-resolution shareable UPI Payment Card bitmap.
     * Contains the LibDesk branding, plan name, amount in INR, generated QR code,
     * UPI ID, and clear instructions for manual payment & verification.
     */
    fun generateBrandedUpiPaymentCard(
        context: Context,
        upiDeepLink: String,
        planName: String,
        amount: Double,
        upiId: String,
        payeeName: String = "LibDesk Cloud Subscriptions",
        libraryName: String? = null,
        billingCycle: String = "MONTHLY"
    ): Bitmap {
        val width = 800
        val height = 1100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background - Crisp clean card with subtle gradient
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header Background Banner (Deep Indigo/Navy)
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1E1B4B")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 180f, headerPaint)

        // Header Accent Stripe (Vibrant Emerald / Cyan)
        val accentPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 175f, width.toFloat(), 185f, accentPaint)

        // App Logo / Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("LIBDESK SUBSCRIPTION PAYMENT", width / 2f, 75f, titlePaint)

        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val subHeading = if (!libraryName.isNullOrBlank()) "For: $libraryName" else "Smart Library Cloud Platform"
        canvas.drawText(subHeading, width / 2f, 120f, subTitlePaint)

        val cycleBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PLAN: ${planName.uppercase()} ($billingCycle)", width / 2f, 155f, cycleBadgePaint)

        // Amount Display Section
        val amountBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EEF2FF")
            style = Paint.Style.FILL
        }
        val amountRect = RectF(60f, 210f, width - 60f, 290f)
        canvas.drawRoundRect(amountRect, 16f, 16f, amountBgPaint)

        val amountBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C7D2FE")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(amountRect, 16f, 16f, amountBorderPaint)

        val amountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4338CA")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("PAYABLE AMOUNT:", 90f, 260f, amountLabelPaint)

        val amountValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("₹${String.format(Locale.US, "%.2f", amount)}", width - 90f, 262f, amountValPaint)

        // QR Code Box (Rounded White Card with Shadow-like border)
        val qrBoxSize = 440f
        val qrBoxLeft = (width - qrBoxSize) / 2f
        val qrBoxTop = 315f
        val qrBoxRect = RectF(qrBoxLeft, qrBoxTop, qrBoxLeft + qrBoxSize, qrBoxTop + qrBoxSize)

        val qrBoxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(qrBoxRect, 24f, 24f, qrBoxBgPaint)

        val qrBoxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(qrBoxRect, 24f, 24f, qrBoxBorderPaint)

        // Draw the ZXing generated QR code inside
        val qrBitmap = generateUpiQrBitmap(upiDeepLink, size = 380)
        val qrBitmapLeft = qrBoxLeft + (qrBoxSize - 380) / 2f
        val qrBitmapTop = qrBoxTop + (qrBoxSize - 380) / 2f
        canvas.drawBitmap(qrBitmap, qrBitmapLeft, qrBitmapTop, null)

        // UPI ID Pill
        val upiPillTop = qrBoxTop + qrBoxSize + 25f
        val upiPillRect = RectF(80f, upiPillTop, width - 80f, upiPillTop + 60f)
        val upiPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(upiPillRect, 30f, 30f, upiPillPaint)

        val upiTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("UPI ID: $upiId", width / 2f, upiPillTop + 38f, upiTextPaint)

        // Payee Name
        val payeePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Payee: $payeeName", width / 2f, upiPillTop + 95f, payeePaint)

        // Instructions Footer
        val instructPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("1. Scan with GPay, PhonePe, Paytm, BHIM, or any UPI App", width / 2f, height - 120f, instructPaint)
        canvas.drawText("2. Complete payment & note the 12-digit UTR / Reference No.", width / 2f, height - 90f, instructPaint)

        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("3. Share receipt on WhatsApp for instant activation!", width / 2f, height - 55f, highlightPaint)

        return bitmap
    }

    /**
     * Saves a Bitmap to the app cache directory under 'images/' and returns a shareable content Uri.
     */
    fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        fileNamePrefix: String = "upi_sub_payment"
    ): Uri? {
        return try {
            val cacheImagesDir = File(context.cacheDir, "images").apply {
                if (!exists()) mkdirs()
            }
            val imageFile = File(cacheImagesDir, "${fileNamePrefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Launches a generic share sheet allowing the user to share the generated UPI QR image.
     */
    fun shareUpiPaymentCard(
        context: Context,
        bitmap: Bitmap,
        planName: String,
        amount: Double,
        upiId: String,
        upiDeepLink: String
    ) {
        val uri = saveBitmapToCache(context, bitmap, "libdesk_sub_${planName.lowercase().replace(" ", "_")}")
        if (uri == null) {
            // Toast.makeText(context, "Could not prepare shareable image", Toast.LENGTH_SHORT).show()
            return
        }

        val caption = """
            💳 LibDesk Subscription Payment
            Plan: $planName
            Amount: ₹${String.format(Locale.US, "%.2f", amount)}
            UPI ID: $upiId
            
            Pay via UPI or scan the attached QR code.
            Deep Link: $upiDeepLink
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("UPI Payment QR", uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share UPI Payment QR"))
    }

    /**
     * Opens an external UPI payment application installed on the device (e.g. GPay, PhonePe, Paytm).
     */
    fun openUpiPayIntent(context: Context, upiDeepLink: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(upiDeepLink)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                // Fallback chooser
                val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(upiDeepLink)), "Choose UPI Payment App")
                context.startActivity(chooser)
                true
            } catch (err: Exception) {
                // Toast.makeText(context, "No UPI apps installed. Please scan the QR code.", Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    /**
     * Triggers a WhatsApp intent to share the manual payment receipt directly with support.
     * Supports both plain text message and optional receipt screenshot image.
     */
    fun shareReceiptToWhatsApp(
        context: Context,
        whatsappNumber: String? = "",
        planName: String,
        amount: Double,
        utrNumber: String,
        libraryName: String = "My Library",
        ownerName: String = "Library Owner",
        billingCycle: String = "MONTHLY",
        receiptImageUri: Uri? = null
    ) {
        val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        var rawDigits = (whatsappNumber ?: "").filter { it.isDigit() }
        while (rawDigits.startsWith("0")) {
            rawDigits = rawDigits.drop(1)
        }
        val cleanPhone = when {
            rawDigits.length == 10 -> "91$rawDigits"
            rawDigits.length == 12 && rawDigits.startsWith("91") -> rawDigits
            else -> rawDigits
        }

        val messageText = """
            🧾 *LibDesk Subscription Payment Receipt*
            ══════════════════════════
            • *Library:* $libraryName
            • *Owner:* $ownerName
            • *Plan:* $planName ($billingCycle)
            • *Amount Paid:* ₹${String.format(Locale.US, "%.2f", amount)}
            • *Payment Mode:* UPI Manual Transfer
            • *UTR / Transaction Ref:* ${utrNumber.trim()}
            • *Date:* $dateFormatted
            ══════════════════════════
            Please verify my payment and activate the library subscription. Thank you!
        """.trimIndent()

        try {
            if (receiptImageUri != null) {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newUri(context.contentResolver, "Payment Receipt", receiptImageUri)
                    clipboard?.setPrimaryClip(clip)
                } catch (_: Exception) {}
            }

            if (cleanPhone.isNotBlank()) {
                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val w4bIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            setPackage("com.whatsapp.w4b")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(w4bIntent)
                    } catch (_: Exception) {
                        val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(genericIntent)
                    }
                }
            } else {
                val chooser = Intent(Intent.ACTION_SEND).apply {
                    type = if (receiptImageUri != null) "image/*" else "text/plain"
                    if (receiptImageUri != null) {
                        putExtra(Intent.EXTRA_STREAM, receiptImageUri)
                        clipData = ClipData.newRawUri("Payment Receipt", receiptImageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    putExtra(Intent.EXTRA_TEXT, messageText)
                }
                context.startActivity(Intent.createChooser(chooser, "Send Receipt via WhatsApp or other apps"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Copies the UPI ID to the device clipboard and shows a brief confirmation Toast.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        // Toast.makeText(context, "Copied $label to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun createFallbackBitmap(size: Int, text: String): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(10f, 10f, (size - 10).toFloat(), (size - 10).toFloat(), borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("UPI PAYMENT QR", size / 2f, size / 2f, textPaint)
        return bitmap
    }
}
