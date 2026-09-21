package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.Color as AndroidColor
import android.graphics.Paint.Align
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.components.generateQrBitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageShareUtils {

    
    fun createReceiptBitmap(
        library: LibraryEntity?,
        payment: PaymentEntity
    ): Bitmap {
        val width = 1080
        val height = 1560
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        
        val bgPaint = Paint().apply {
            color = AndroidColor.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        
        val cardMargin = 36f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorderPaint)

        
        val headerHeight = 240f
        val headerRect = RectF(cardMargin, cardMargin, width - cardMargin, cardMargin + headerHeight)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardMargin, cardMargin,
                width - cardMargin, cardMargin + headerHeight,
                AndroidColor.parseColor("#0F172A"),
                AndroidColor.parseColor("#1E293B"),
                Shader.TileMode.CLAMP
            )
        }
        val clipPath = Path().apply {
            addRoundRect(cardRect, 32f, 32f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawRect(headerRect, headerPaint)
        canvas.restore()

        
        val libName = library?.name?.ifBlank { "LIBDESK DIGITAL LIBRARY" } ?: "LIBDESK DIGITAL LIBRARY"
        val libAddress = "${library?.address ?: "Central Study Hub"}, ${library?.city ?: "Education Zone"}"
        val libContact = "Helpline: ${library?.phone ?: ""} | UPI: ${library?.upiId ?: ""}"

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText(libName.uppercase(), width / 2f, cardMargin + 72f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 24f
            textAlign = Align.CENTER
        }
        canvas.drawText(libAddress, width / 2f, cardMargin + 120f, subPaint)
        canvas.drawText(libContact, width / 2f, cardMargin + 160f, subPaint)

        
        val badgeRect = RectF(width / 2f - 240f, cardMargin + headerHeight - 34f, width / 2f + 240f, cardMargin + headerHeight + 34f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#10B981")
            style = Paint.Style.FILL
        }
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(badgeRect, 34f, 34f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 34f, 34f, badgeBorderPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText("OFFICIAL FEE RECEIPT", width / 2f, cardMargin + headerHeight + 10f, badgeTextPaint)

        var curY = cardMargin + headerHeight + 80f

        
        val metaLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 24f
        }
        val metaValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("RECEIPT NUMBER", cardMargin + 40f, curY, metaLabelPaint)
        canvas.drawText(payment.receiptNumber, cardMargin + 40f, curY + 36f, metaValPaint)

        val formattedDate = try {
            if (payment.createdAt > 0) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(payment.createdAt))
            } else {
                payment.date
            }
        } catch (_: Exception) {
            payment.date
        }

        val rightMetaAlign = Paint(metaValPaint).apply { textAlign = Align.RIGHT }
        val rightLabelAlign = Paint(metaLabelPaint).apply { textAlign = Align.RIGHT }
        canvas.drawText("DATE & TIME", width - cardMargin - 40f, curY, rightLabelAlign)
        canvas.drawText(formattedDate, width - cardMargin - 40f, curY + 36f, rightMetaAlign)

        curY += 80f

        
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        canvas.drawLine(cardMargin + 30f, curY, width - cardMargin - 30f, curY, divPaint)
        curY += 30f

        
        // Deposit Period support
        val hasPeriod = payment.period.isNotBlank()
        val studentBoxHeight = if (hasPeriod) 240f else 180f
        val studentBoxRect = RectF(cardMargin + 30f, curY, width - cardMargin - 30f, curY + studentBoxHeight)
        val studentBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#FAFAFA")
            style = Paint.Style.FILL
        }
        val studentBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E4E4E7")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(studentBoxRect, 20f, 20f, studentBoxPaint)
        canvas.drawRoundRect(studentBoxRect, 20f, 20f, studentBoxBorder)

        val sLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#71717A")
            textSize = 22f
        }
        val sValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#18181B")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Received From:", cardMargin + 60f, curY + 45f, sLabelPaint)
        canvas.drawText(payment.studentName, cardMargin + 60f, curY + 80f, sValPaint)

        canvas.drawText("Payment Purpose:", cardMargin + 60f, curY + 125f, sLabelPaint)
        canvas.drawText(payment.purpose.replace("_", " "), cardMargin + 60f, curY + 155f, sValPaint)

        if (hasPeriod) {
            canvas.drawText("Deposit Period (From - To):", cardMargin + 60f, curY + 195f, sLabelPaint)
            canvas.drawText(payment.period, cardMargin + 60f, curY + 225f, sValPaint)
        }

        val sRightVal = Paint(sValPaint).apply { textAlign = Align.RIGHT }
        val sRightLabel = Paint(sLabelPaint).apply { textAlign = Align.RIGHT }

        canvas.drawText("Payment Mode:", width - cardMargin - 60f, curY + 45f, sRightLabel)
        canvas.drawText("${payment.paymentMode} (VERIFIED)", width - cardMargin - 60f, curY + 80f, sRightVal)

        canvas.drawText("Ref / Txn ID:", width - cardMargin - 60f, curY + 125f, sRightLabel)
        canvas.drawText(payment.referenceNumber.ifBlank { "N/A" }, width - cardMargin - 60f, curY + 155f, sRightVal)

        curY += (studentBoxHeight + 30f)

        
        val amountBoxRect = RectF(cardMargin + 30f, curY, width - cardMargin - 30f, curY + 160f)
        val amountBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        val amountBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#18181B")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(amountBoxRect, 20f, 20f, amountBoxPaint)
        canvas.drawRoundRect(amountBoxRect, 20f, 20f, amountBorderPaint)

        val amtLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#18181B")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amtValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#18181B")
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.RIGHT
        }
        canvas.drawText("TOTAL AMOUNT RECEIVED", cardMargin + 60f, curY + 65f, amtLabelPaint)
        canvas.drawText("STATUS: SUCCESSFUL", cardMargin + 60f, curY + 115f, Paint(amtLabelPaint).apply { textSize = 22f; color = AndroidColor.parseColor("#52525B") })

        canvas.drawText("₹${payment.amount.toInt()}", width - cardMargin - 60f, curY + 100f, amtValPaint)

        curY += 190f

        
        if (payment.dueBalance > 0) {
            val dueBgRect = RectF(cardMargin + 30f, curY, width - cardMargin - 30f, curY + 60f)
            val dueBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#FEF2F2")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(dueBgRect, 14f, 14f, dueBgPaint)
            val dueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#DC2626")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Outstanding Pending Due Balance:", cardMargin + 50f, curY + 40f, dueTextPaint)
            canvas.drawText("₹${payment.dueBalance.toInt()}", width - cardMargin - 50f, curY + 40f, Paint(dueTextPaint).apply { textAlign = Align.RIGHT })
            curY += 80f
        } else {
            val clearBgRect = RectF(cardMargin + 30f, curY, width - cardMargin - 30f, curY + 60f)
            val clearBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#F0FDF4")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(clearBgRect, 14f, 14f, clearBgPaint)
            val clearTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#16A34A")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Membership Dues Status:", cardMargin + 50f, curY + 40f, clearTextPaint)
            canvas.drawText("ALL DUES CLEARED ✓", width - cardMargin - 50f, curY + 40f, Paint(clearTextPaint).apply { textAlign = Align.RIGHT })
            curY += 80f
        }

        
        val qrVerificationData = "RECEIPT:${payment.receiptNumber}|AMT:${payment.amount}|STU:${payment.studentName}|LIB:${library?.name ?: "LibDesk"}"
        val qrBitmap = generateQrBitmap(qrVerificationData, 220)
        val qrLeft = cardMargin + 50f
        canvas.drawBitmap(qrBitmap, qrLeft, curY, null)

        val qrTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1E293B")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val qrSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 21f
        }
        canvas.drawText("Digital Verification QR", qrLeft + 250f, curY + 50f, qrTextPaint)
        canvas.drawText("Scan with any QR scanner to verify", qrLeft + 250f, curY + 85f, qrSubPaint)
        canvas.drawText("receipt authenticity on LibDesk System.", qrLeft + 250f, curY + 115f, qrSubPaint)

        
        val stampRect = RectF(width - cardMargin - 280f, curY + 10f, width - cardMargin - 40f, curY + 190f)
        val stampBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#2563EB")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
        }
        canvas.drawRoundRect(stampRect, 16f, 16f, stampBorderPaint)

        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#2563EB")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText("DIGITALLY SEALED", stampRect.centerX(), stampRect.top + 50f, stampTextPaint)
        canvas.drawText("AUTHORIZED", stampRect.centerX(), stampRect.top + 90f, Paint(stampTextPaint).apply { textSize = 24f })
        canvas.drawText(library?.name?.take(18) ?: "LIBDESK", stampRect.centerX(), stampRect.top + 130f, Paint(stampTextPaint).apply { textSize = 18f })

        
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 20f
            textAlign = Align.CENTER
        }
        canvas.drawText("This is an electronically generated receipt. No physical signature required.", width / 2f, height - cardMargin - 60f, footerPaint)
        canvas.drawText("Powered by LibDesk Library Management System", width / 2f, height - cardMargin - 30f, Paint(footerPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

        return bitmap
    }

    
    fun createFeeReminderBitmap(
        library: LibraryEntity?,
        student: StudentEntity
    ): Bitmap {
        val width = 1080
        val height = 1560
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        
        val bgPaint = Paint().apply {
            color = AndroidColor.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        
        val cardMargin = 36f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorderPaint)

        
        val headerHeight = 250f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardMargin, cardMargin,
                width - cardMargin, cardMargin + headerHeight,
                AndroidColor.parseColor("#0F172A"),
                AndroidColor.parseColor("#312E81"),
                Shader.TileMode.CLAMP
            )
        }
        val clipPath = Path().apply {
            addRoundRect(cardRect, 32f, 32f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawRect(RectF(cardMargin, cardMargin, width - cardMargin, cardMargin + headerHeight), headerPaint)
        canvas.restore()

        
        val libName = library?.name?.ifBlank { "LIBDESK DIGITAL LIBRARY" } ?: "LIBDESK DIGITAL LIBRARY"
        val libAddress = "${library?.address ?: "Main Study Campus"}, ${library?.city ?: ""}"

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText(libName.uppercase(), width / 2f, cardMargin + 72f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#CBD5E1")
            textSize = 24f
            textAlign = Align.CENTER
        }
        canvas.drawText(libAddress, width / 2f, cardMargin + 120f, subPaint)
        canvas.drawText("Support & Inquiries: ${library?.phone ?: ""}", width / 2f, cardMargin + 160f, subPaint)

        
        val badgeRect = RectF(width / 2f - 240f, cardMargin + headerHeight - 34f, width / 2f + 240f, cardMargin + headerHeight + 34f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#EA580C") 
            style = Paint.Style.FILL
        }
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(badgeRect, 34f, 34f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 34f, 34f, badgeBorderPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText("MEMBERSHIP FEE REMINDER", width / 2f, cardMargin + headerHeight + 10f, badgeTextPaint)

        var curY = cardMargin + headerHeight + 70f

        
        val greetingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Dear ${student.fullName},", cardMargin + 40f, curY, greetingPaint)
        curY += 40f

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#475569")
            textSize = 24f
        }
        canvas.drawText("This is a gentle reminder regarding your pending library subscription dues.", cardMargin + 40f, curY, bodyPaint)
        curY += 40f

        
        val infoRect = RectF(cardMargin + 30f, curY, width - cardMargin - 30f, curY + 160f)
        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(infoRect, 18f, 18f, infoPaint)

        val iLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 22f
        }
        val iValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Student ID:", cardMargin + 60f, curY + 45f, iLabelPaint)
        canvas.drawText(student.studentCode, cardMargin + 60f, curY + 80f, iValPaint)

        canvas.drawText("Seat Number:", cardMargin + 60f, curY + 120f, iLabelPaint)
        canvas.drawText(student.seatNumber.ifBlank { "Unassigned" }, cardMargin + 60f, curY + 150f, iValPaint)

        val iRightVal = Paint(iValPaint).apply { textAlign = Align.RIGHT }
        val iRightLabel = Paint(iLabelPaint).apply { textAlign = Align.RIGHT }

        canvas.drawText("Shift & Timing:", width - cardMargin - 60f, curY + 45f, iRightLabel)
        canvas.drawText(student.shiftName.ifBlank { "Full Day Shift" }, width - cardMargin - 60f, curY + 80f, iRightVal)

        canvas.drawText("Plan / Package:", width - cardMargin - 60f, curY + 120f, iRightLabel)
        canvas.drawText(student.planName.ifBlank { "Monthly Regular" }, width - cardMargin - 60f, curY + 150f, iRightVal)

        curY += 190f

        
        val dueBoxRect = RectF(cardMargin + 30f, curY, width - cardMargin - 30f, curY + 160f)
        val dueBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#FEF2F2")
            style = Paint.Style.FILL
        }
        val dueBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#EF4444")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(dueBoxRect, 20f, 20f, dueBoxPaint)
        canvas.drawRoundRect(dueBoxRect, 20f, 20f, dueBorderPaint)

        val dueLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#991B1B")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val dueValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#DC2626")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.RIGHT
        }
        canvas.drawText("PENDING DUE AMOUNT", cardMargin + 60f, curY + 65f, dueLabelPaint)
        canvas.drawText("Total Fee: ₹${student.totalFee.toInt()} • Paid: ₹${student.paidAmount.toInt()}", cardMargin + 60f, curY + 115f, Paint(bodyPaint).apply { textSize = 20f })

        canvas.drawText("₹${student.dueAmount.toInt()}", width - cardMargin - 60f, curY + 105f, dueValPaint)

        curY += 190f

        
        val upiId = library?.upiId?.ifBlank { "" } ?: ""
        val upiPayee = library?.upiPayeeName?.ifBlank { library?.name ?: "Library Admin" } ?: (library?.name ?: "Library Admin")
        val upiPaymentUri = "upi://pay?pa=$upiId&pn=${Uri.encode(upiPayee)}&am=${student.dueAmount.toInt()}&cu=INR&tn=LibraryFee_${student.studentCode}"

        val qrBitmap = generateQrBitmap(upiPaymentUri, 250)
        val qrLeft = cardMargin + 50f
        canvas.drawBitmap(qrBitmap, qrLeft, curY, null)

        val qrTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val qrDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#475569")
            textSize = 21f
        }
        val upiBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#2563EB")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Scan & Pay via UPI", qrLeft + 280f, curY + 45f, qrTitlePaint)
        canvas.drawText("GPay • PhonePe • Paytm • BHIM", qrLeft + 280f, curY + 80f, qrDescPaint)
        canvas.drawText("UPI ID: $upiId", qrLeft + 280f, curY + 125f, upiBadgePaint)
        canvas.drawText("Payee: $upiPayee", qrLeft + 280f, curY + 160f, qrDescPaint)
        canvas.drawText("Amount: ₹${student.dueAmount.toInt()}", qrLeft + 280f, curY + 195f, Paint(qrTitlePaint).apply { textSize = 22f; color = AndroidColor.parseColor("#059669") })

        curY += 280f

        
        val noticeBoxRect = RectF(cardMargin + 30f, curY, width - cardMargin - 30f, curY + 90f)
        val noticeBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#FFFBEB")
            style = Paint.Style.FILL
        }
        val noticeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#FDE68A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(noticeBoxRect, 16f, 16f, noticeBoxPaint)
        canvas.drawRoundRect(noticeBoxRect, 16f, 16f, noticeBorderPaint)

        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#B45309")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText("Please clear dues promptly to avoid seat allocation cancellation.", width / 2f, curY + 40f, notePaint)
        canvas.drawText("Share payment screenshot after transfer for instant receipt generation.", width / 2f, curY + 68f, Paint(notePaint).apply { textSize = 18f; typeface = Typeface.DEFAULT })

        
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 20f
            textAlign = Align.CENTER
        }
        canvas.drawText("Sent automatically from LibDesk Library System", width / 2f, height - cardMargin - 60f, footerPaint)
        canvas.drawText("Thank you for choosing ${library?.name ?: "our library"}!", width / 2f, height - cardMargin - 30f, Paint(footerPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

        return bitmap
    }

    
    /**
     * Function to generate a receipt image containing student fee details that can be shared via WhatsApp,
     * incorporating authentic layout, QR code verification, student details, and authorized branding.
     */
    fun generateReceiptImage(
        library: LibraryEntity?,
        payment: PaymentEntity
    ): Bitmap {
        return createReceiptBitmap(library, payment)
    }

    /**
     * Utility to trigger an SMS Intent with pre-filled text content formatted as a library fee receipt for mobile messaging.
     */
    fun triggerSmsReceiptIntent(
        context: Context,
        library: LibraryEntity?,
        payment: PaymentEntity,
        studentPhone: String? = null
    ) {
        shareReceiptViaSms(context, library, payment, studentPhone)
    }

    fun shareReceiptViaWhatsApp(
        context: Context,
        library: LibraryEntity?,
        payment: PaymentEntity,
        studentPhone: String? = null
    ) {
        val receiptBitmap = generateReceiptImage(library, payment)
        val formattedAmount = payment.amount.toInt()
        val dueStatus = if (payment.dueBalance > 0) {
            "• *Pending Due Balance:* ₹${payment.dueBalance.toInt()}"
        } else {
            "• *Account Status:* All Dues Cleared ✓"
        }
        val refInfo = if (payment.referenceNumber.isNotBlank()) {
            "• *Txn / Ref ID:* ${payment.referenceNumber}\n"
        } else ""
        val periodInfo = if (payment.period.isNotBlank()) {
            "• *Deposit Period:* ${payment.period}\n"
        } else ""

        val caption = "📄 *OFFICIAL FEE RECEIPT* — *${library?.name?.ifBlank { "LibDesk Study Hub" } ?: "LibDesk Study Hub"}*\n\n" +
            "Dear *${payment.studentName}*,\n" +
            "Your library membership fee payment has been successfully recorded.\n\n" +
            "• *Receipt No:* ${payment.receiptNumber}\n" +
            "• *Amount Paid:* ₹$formattedAmount\n" +
            "• *Payment Mode:* ${payment.paymentMode}\n" +
            "• *Purpose:* ${payment.purpose.replace("_", " ")}\n" +
            periodInfo +
            refInfo +
            "• *Date:* ${payment.date}\n" +
            "$dueStatus\n\n" +
            "High-resolution digital receipt card with QR verification is attached above.\n" +
            "Thank you for choosing ${library?.name ?: "our study hub"}!"

        sendImageToWhatsApp(
            context = context,
            bitmap = receiptBitmap,
            fileName = "receipt_${payment.receiptNumber}.png",
            captionText = caption,
            phoneNumber = studentPhone
        )
    }

    /**
     * Share fee receipt record directly to student via Mobile SMS in clean text format.
     */
    fun shareReceiptViaSms(
        context: Context,
        library: LibraryEntity?,
        payment: PaymentEntity,
        studentPhone: String? = null
    ) {
        val formattedAmount = payment.amount.toInt()
        val dueStatus = if (payment.dueBalance > 0) {
            "Pending Due: Rs.${payment.dueBalance.toInt()}."
        } else {
            "All dues cleared."
        }
        val refInfo = if (payment.referenceNumber.isNotBlank()) "Txn Ref: ${payment.referenceNumber}. " else ""
        val libTitle = library?.name?.ifBlank { "LibDesk Study Hub" } ?: "LibDesk Study Hub"
        val libContact = if (!library?.phone.isNullOrBlank()) " Helpline: ${library?.phone}." else ""

        val smsText = "FEE RECEIPT: Dear ${payment.studentName}, your fee payment of Rs.$formattedAmount for ${payment.purpose.replace("_", " ")} at $libTitle has been received successfully on ${payment.date}. Receipt No: ${payment.receiptNumber}. Mode: ${payment.paymentMode}. $refInfo$dueStatus Thank you!$libContact"

        val cleanPhone = studentPhone?.filter { it.isDigit() }?.let { raw ->
            if (raw.length == 10) raw else if (raw.length > 10) raw.takeLast(10) else raw
        } ?: ""

        try {
            val uri = if (cleanPhone.isNotBlank()) {
                Uri.parse("smsto:$cleanPhone")
            } else {
                Uri.parse("smsto:")
            }
            val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", smsText)
            }
            context.startActivity(smsIntent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:${if (cleanPhone.isNotBlank()) cleanPhone else ""}")
                    putExtra("sms_body", smsText)
                }
                context.startActivity(fallbackIntent)
            } catch (err: Exception) {
                // Toast.makeText(context, "Could not open SMS application.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Share fee receipt image via Email client (Gmail, Outlook, etc.)
     */
    fun shareReceiptViaEmail(
        context: Context,
        library: LibraryEntity?,
        payment: PaymentEntity,
        studentEmail: String? = null
    ) {
        try {
            val receiptBitmap = createReceiptBitmap(library, payment)
            val fileName = "receipt_${payment.receiptNumber}.png"
            val uri = saveBitmapToCache(context, receiptBitmap, fileName)
            if (uri == null) {
                // Toast.makeText(context, "Could not generate receipt image", Toast.LENGTH_SHORT).show()
                return
            }

            val subject = "Official Fee Receipt #${payment.receiptNumber} - ${library?.name ?: "LibDesk Library"}"
            val body = """
                Dear ${payment.studentName},

                We have successfully recorded your fee payment at ${library?.name ?: "our library"}.
                Attached is your official digital fee receipt in image format.

                Receipt Number: ${payment.receiptNumber}
                Amount Paid: ₹${payment.amount.toInt()}
                Payment Date: ${payment.date}
                Payment Mode: ${payment.paymentMode}
                Purpose: ${payment.purpose.replace("_", " ")}
                ${if (payment.period.isNotBlank()) "Deposit Period: ${payment.period}\n" else ""}${if (payment.referenceNumber.isNotBlank()) "Reference / Txn ID: ${payment.referenceNumber}\n" else ""}${if (payment.dueBalance > 0) "Pending Due: ₹${payment.dueBalance.toInt()}\n" else "Account Status: All dues cleared ✓\n"}
                Please keep this receipt for your records.

                Warm regards,
                ${library?.name ?: "Library Management"}
                Phone / Helpline: ${library?.phone ?: ""}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                if (!studentEmail.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(studentEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Send Receipt via Email"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Toast.makeText(context, "Error opening email app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share fee receipt image via generic Android Share sheet (Bluetooth, Drive, Telegram, etc.)
     */
    fun shareReceiptGeneric(
        context: Context,
        library: LibraryEntity?,
        payment: PaymentEntity
    ) {
        try {
            val receiptBitmap = createReceiptBitmap(library, payment)
            val fileName = "receipt_${payment.receiptNumber}.png"
            val uri = saveBitmapToCache(context, receiptBitmap, fileName)
            if (uri == null) {
                // Toast.makeText(context, "Could not render receipt image", Toast.LENGTH_SHORT).show()
                return
            }

            val text = "Official Fee Receipt #${payment.receiptNumber} for ${payment.studentName} (₹${payment.amount.toInt()}) - ${library?.name ?: "LibDesk"}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Receipt Image"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Save receipt image locally to device storage / Gallery so the student or admin can access it anytime offline.
     */
    fun saveReceiptLocally(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Uri? {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LibDeskReceipts")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                // Toast.makeText(context, "Receipt saved locally to Pictures/Gallery", Toast.LENGTH_LONG).show()
                imageUri
            } else {
                val fallbackUri = saveBitmapToCache(context, bitmap, fileName)
                // Toast.makeText(context, "Receipt saved to app storage", Toast.LENGTH_SHORT).show()
                fallbackUri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val fallbackUri = saveBitmapToCache(context, bitmap, fileName)
            // Toast.makeText(context, "Saved locally: $fileName", Toast.LENGTH_SHORT).show()
            fallbackUri
        }
    }

    /**
     * Generate exportable visual summary image for Library Admins
     */
    fun createPaymentSummaryBitmap(
        library: LibraryEntity?,
        dateRangeText: String,
        totalPaid: Double,
        totalPending: Double,
        totalRecords: Int,
        paidCount: Int,
        pendingCount: Int,
        highlights: List<String>
    ): Bitmap {
        val width = 1080
        val height = 1520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background - no saturated background color
        canvas.drawColor(AndroidColor.WHITE)

        val margin = 40f
        val cardRect = RectF(margin, margin, width - margin, height - margin)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E4E4E7")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        // Header Box
        val headerHeight = 220f
        val headerRect = RectF(margin, margin, width - margin, margin + headerHeight)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#18181B")
            style = Paint.Style.FILL
        }
        val clipPath = Path().apply {
            addRoundRect(cardRect, 28f, 28f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawRect(headerRect, headerPaint)
        canvas.restore()

        val libName = library?.name?.ifBlank { "LIBDESK DIGITAL LIBRARY" } ?: "LIBDESK DIGITAL LIBRARY"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText(libName.uppercase(), width / 2f, margin + 70f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#A1A1AA")
            textSize = 24f
            textAlign = Align.CENTER
        }
        canvas.drawText("EXECUTIVE FEE & PAYMENT COLLECTION REPORT", width / 2f, margin + 115f, subPaint)
        canvas.drawText("Period: $dateRangeText | Generated on: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}", width / 2f, margin + 155f, subPaint)

        var curY = margin + headerHeight + 50f

        // 2-Column KPI cards
        val colWidth = (width - margin * 2 - 30f) / 2f

        // Card 1: Total Paid
        val kpi1Rect = RectF(margin + 15f, curY, margin + 15f + colWidth, curY + 160f)
        val kpiBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E4E4E7")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val kpiBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#FAFAFA")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(kpi1Rect, 20f, 20f, kpiBg)
        canvas.drawRoundRect(kpi1Rect, 20f, 20f, kpiBorder)

        val kpiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#71717A")
            textSize = 22f
            textAlign = Align.CENTER
        }
        val kpiValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#18181B")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText("TOTAL COLLECTED (PAID)", kpi1Rect.centerX(), kpi1Rect.top + 50f, kpiLabelPaint)
        canvas.drawText("₹${totalPaid.toInt()}", kpi1Rect.centerX(), kpi1Rect.top + 105f, kpiValPaint)
        canvas.drawText("$paidCount transactions", kpi1Rect.centerX(), kpi1Rect.top + 140f, Paint(kpiLabelPaint).apply { textSize = 20f })

        // Card 2: Total Pending
        val kpi2Rect = RectF(margin + 15f + colWidth + 15f, curY, width - margin - 15f, curY + 160f)
        canvas.drawRoundRect(kpi2Rect, 20f, 20f, kpiBg)
        canvas.drawRoundRect(kpi2Rect, 20f, 20f, kpiBorder)

        canvas.drawText("TOTAL PENDING DUES", kpi2Rect.centerX(), kpi2Rect.top + 50f, kpiLabelPaint)
        canvas.drawText("₹${totalPending.toInt()}", kpi2Rect.centerX(), kpi2Rect.top + 105f, kpiValPaint)
        canvas.drawText("$pendingCount pending accounts", kpi2Rect.centerX(), kpi2Rect.top + 140f, Paint(kpiLabelPaint).apply { textSize = 20f })

        curY += 200f

        // Stats Row
        val statsRect = RectF(margin + 15f, curY, width - margin - 15f, curY + 110f)
        canvas.drawRoundRect(statsRect, 20f, 20f, kpiBg)
        canvas.drawRoundRect(statsRect, 20f, 20f, kpiBorder)

        val totalVolume = totalPaid + totalPending
        val collectionRate = if (totalVolume > 0) ((totalPaid / totalVolume) * 100).toInt() else 100
        val statTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#18181B")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val statSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#71717A")
            textSize = 20f
        }
        canvas.drawText("Collection Rate: $collectionRate%", statsRect.left + 30f, statsRect.top + 45f, statTextPaint)
        canvas.drawText("Total Turnover: ₹${totalVolume.toInt()} | Active Records: $totalRecords", statsRect.left + 30f, statsRect.top + 80f, statSubSubPaint(statSubPaint))

        curY += 140f

        // Section Title: Highlights & Recent Transactions
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#18181B")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("RECENT FEE RECORDS AUDIT", margin + 20f, curY, sectionTitlePaint)
        curY += 30f

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E4E4E7")
            strokeWidth = 2f
        }
        canvas.drawLine(margin + 20f, curY, width - margin - 20f, curY, linePaint)
        curY += 30f

        val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#27272A")
            textSize = 22f
        }
        val itemSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#71717A")
            textSize = 19f
        }

        val displayItems = highlights.take(6)
        if (displayItems.isEmpty()) {
            canvas.drawText("No fee payment records found for the selected period.", margin + 20f, curY + 40f, itemSubPaint)
            curY += 80f
        } else {
            for (item in displayItems) {
                val rowRect = RectF(margin + 15f, curY, width - margin - 15f, curY + 68f)
                canvas.drawRoundRect(rowRect, 12f, 12f, kpiBg)
                canvas.drawRoundRect(rowRect, 12f, 12f, kpiBorder)
                canvas.drawText(item, rowRect.left + 25f, rowRect.centerY() + 8f, itemTextPaint)
                curY += 80f
            }
        }

        // Footer & Seal
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#71717A")
            textSize = 20f
            textAlign = Align.CENTER
        }
        canvas.drawText("Official Financial Report for Library Administration & Audits", width / 2f, height - margin - 50f, footerPaint)
        canvas.drawText("Generated securely by LibDesk Management System", width / 2f, height - margin - 25f, footerPaint)

        return bitmap
    }

    private fun statSubSubPaint(paint: Paint): Paint = paint

    /**
     * Share exportable summary via WhatsApp
     */
    fun shareSummaryViaWhatsApp(
        context: Context,
        library: LibraryEntity?,
        summaryText: String,
        bitmap: Bitmap
    ) {
        val fileName = "finance_summary_${System.currentTimeMillis()}.png"
        sendImageToWhatsApp(
            context = context,
            bitmap = bitmap,
            fileName = fileName,
            captionText = summaryText
        )
    }

    /**
     * Share exportable summary via Email
     */
    fun shareSummaryViaEmail(
        context: Context,
        library: LibraryEntity?,
        summaryText: String,
        bitmap: Bitmap
    ) {
        try {
            val fileName = "finance_summary_${System.currentTimeMillis()}.png"
            val uri = saveBitmapToCache(context, bitmap, fileName)
            if (uri == null) {
                // Toast.makeText(context, "Could not render summary image", Toast.LENGTH_SHORT).show()
                return
            }

            val subject = "Payment History & Fee Summary Report - ${library?.name ?: "LibDesk"}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, summaryText)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Email Financial Summary"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Toast.makeText(context, "Email share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Export CSV summary
     */
    fun exportPaymentSummaryCsv(
        context: Context,
        library: LibraryEntity?,
        dateRangeText: String,
        csvContent: String
    ): Uri? {
        return try {
            val fileName = "payment_history_${System.currentTimeMillis()}.csv"
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeText(csvContent)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Payment History CSV Export - ${library?.name ?: "LibDesk"}")
                putExtra(Intent.EXTRA_TEXT, "Detailed payment records and fee history for period: $dateRangeText")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Payment History CSV"))
            // Toast.makeText(context, "Exported Payment History CSV", Toast.LENGTH_SHORT).show()
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            // Toast.makeText(context, "CSV Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    
    fun shareFeeReminderViaWhatsApp(
        context: Context,
        library: LibraryEntity?,
        student: StudentEntity
    ) {
        val reminderBitmap = createFeeReminderBitmap(library, student)
        val caption = "🔔 *FEE DUES REMINDER* — *${library?.name?.ifBlank { "LibDesk Study Hub" } ?: "LibDesk Study Hub"}*\n\n" +
            "Dear *${student.fullName}* (Student ID: ${student.studentCode}),\n" +
            "This is a gentle reminder regarding your library subscription dues.\n\n" +
            "• *Seat Number:* ${student.seatNumber.ifBlank { "Assigned Seat" }}\n" +
            "• *Shift:* ${student.shiftName.ifBlank { "Standard" }}\n" +
            "• *Pending Due Amount:* *₹${student.dueAmount.toInt()}*\n" +
            (if (!library?.upiId.isNullOrBlank()) "• *UPI ID for Payment:* ${library.upiId}\n" else "") +
            "\nPlease clear dues to ensure uninterrupted study seat allocation. High-resolution reminder with instant UPI QR is attached above.\n" +
            "Thank you!"

        sendImageToWhatsApp(
            context = context,
            bitmap = reminderBitmap,
            fileName = "fee_reminder_${student.studentCode}.png",
            captionText = caption,
            phoneNumber = student.mobile
        )
    }

    
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return try {
            val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(imagesDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    
    fun sendImageToWhatsApp(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        captionText: String,
        phoneNumber: String? = null
    ) {
        try {
            val uri = saveBitmapToCache(context, bitmap, fileName)
            if (uri == null) {
                // Toast.makeText(context, "Could not render image file for sharing.", Toast.LENGTH_SHORT).show()
                return
            }

            val cleanPhone = phoneNumber
                ?.replace(Regex("[^0-9]"), "")
                ?.let { raw ->
                    if (raw.length == 10) "91$raw" else raw
                }

            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("Fee Receipt", uri)
                putExtra(Intent.EXTRA_TEXT, captionText)
                if (!cleanPhone.isNullOrBlank()) {
                    putExtra("jid", "$cleanPhone@s.whatsapp.net")
                }
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                context.startActivity(whatsappIntent)
            } catch (_: Exception) {

                try {
                    val w4bIntent = Intent(whatsappIntent).apply { 
                        setPackage("com.whatsapp.w4b") 
                        clipData = android.content.ClipData.newRawUri("Fee Receipt", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(w4bIntent)
                } catch (_: Exception) {

                    val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = android.content.ClipData.newRawUri("Fee Receipt", uri)
                        putExtra(Intent.EXTRA_TEXT, captionText)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(chooserIntent, "Send via WhatsApp or other apps"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Toast.makeText(context, "Error launching WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendTextToWhatsApp(context: Context, mobile: String, message: String) {
        val cleanMobile = mobile.filter { it.isDigit() }.let {
            if (it.length == 10) "91$it" else it
        }
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanMobile&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(Intent.createChooser(genericIntent, "Send Message / Offer"))
            } catch (ex: Exception) {
                // Toast.makeText(context, "Could not open messaging app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    
    fun shareImageGeneric(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        title: String,
        captionText: String
    ) {
        try {
            val uri = saveBitmapToCache(context, bitmap, fileName)
            if (uri == null) {
                // Toast.makeText(context, "Could not render image for sharing.", Toast.LENGTH_SHORT).show()
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, captionText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            e.printStackTrace()
            // Toast.makeText(context, "Error sharing: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    
    fun createLibraryAttendancePosterBitmap(
        library: LibraryEntity?
    ): Bitmap {
        val width = 1200
        val height = 1800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        
        val bgPaint = Paint().apply {
            color = AndroidColor.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        
        val margin = 40f
        val posterRect = RectF(margin, margin, width - margin, height - margin)
        val posterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        val posterBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(posterRect, 40f, 40f, posterBgPaint)
        canvas.drawRoundRect(posterRect, 40f, 40f, posterBorderPaint)

        
        val headerHeight = 320f
        val headerRect = RectF(margin, margin, width - margin, margin + headerHeight)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                margin, margin,
                width - margin, margin + headerHeight,
                AndroidColor.parseColor("#0F172A"),
                AndroidColor.parseColor("#1E3A8A"),
                Shader.TileMode.CLAMP
            )
        }

        val clipPath = Path().apply {
            addRoundRect(posterRect, 40f, 40f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawRect(headerRect, headerPaint)
        canvas.restore()

        
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#38BDF8")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
            letterSpacing = 0.15f
        }
        canvas.drawText("LIBDESK SMART LIBRARY ACCESS", width / 2f, margin + 70f, brandPaint)

        
        val libName = library?.name?.ifBlank { "VANGUARD STUDY LIBRARY" } ?: "VANGUARD STUDY LIBRARY"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText(libName.uppercase(), width / 2f, margin + 140f, titlePaint)

        val addressText = "${library?.address ?: "Sector 14, Main Road"}, ${library?.city ?: "New Delhi"}"
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E2E8F0")
            textSize = 26f
            textAlign = Align.CENTER
        }
        canvas.drawText(addressText, width / 2f, margin + 195f, subPaint)

        
        val badgeRect = RectF(width / 2f - 260f, margin + headerHeight - 38f, width / 2f + 260f, margin + headerHeight + 38f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0284C7")
            style = Paint.Style.FILL
        }
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRoundRect(badgeRect, 38f, 38f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 38f, 38f, badgeBorderPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
            letterSpacing = 0.05f
        }
        canvas.drawText("GATE & RECEPTION SCANNER", width / 2f, margin + headerHeight + 12f, badgeTextPaint)

        
        val instrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0F172A")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText("SCAN HERE FOR IN / OUT ATTENDANCE", width / 2f, margin + headerHeight + 120f, instrPaint)

        val subInstrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#64748B")
            textSize = 24f
            textAlign = Align.CENTER
        }
        canvas.drawText("Open LibDesk App on your mobile • Scan code at Entry and Exit", width / 2f, margin + headerHeight + 165f, subInstrPaint)

        
        val qrPayload = "LIBDESK_GATE_ATTENDANCE:${library?.id ?: "LIB-001"}:${library?.code ?: "UNKNOWN-LIBRARY"}:${library?.name ?: "Your Library"}"
        val qrBitmap = generateQrBitmap(qrPayload, 620)

        val qrBoxSize = 660f
        val qrBoxLeft = (width - qrBoxSize) / 2f
        val qrBoxTop = margin + headerHeight + 210f
        val qrRect = RectF(qrBoxLeft, qrBoxTop, qrBoxLeft + qrBoxSize, qrBoxTop + qrBoxSize)

        val qrFrameBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        val qrFrameBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(qrRect, 28f, 28f, qrFrameBg)
        canvas.drawRoundRect(qrRect, 28f, 28f, qrFrameBorder)

        if (qrBitmap != null) {
            val qrLeft = qrBoxLeft + 20f
            val qrTop = qrBoxTop + 20f
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
        }

        
        val featTop = qrBoxTop + qrBoxSize + 40f
        val featRect = RectF(margin + 60f, featTop, width - margin - 60f, featTop + 220f)
        val featBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#F0FDF4")
            style = Paint.Style.FILL
        }
        val featBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#86EFAC")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(featRect, 24f, 24f, featBgPaint)
        canvas.drawRoundRect(featRect, 24f, 24f, featBorderPaint)

        val codeTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#15803D")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Align.CENTER
        }
        canvas.drawText("LIBRARY IDENTIFIER CODE: ${library?.code ?: "UNKNOWN-LIBRARY"}", width / 2f, featTop + 55f, codeTitlePaint)

        val verifyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1E293B")
            textSize = 22f
            textAlign = Align.CENTER
        }
        canvas.drawText("✓ GPS Location Verified Attendance Logging", width / 2f, featTop + 105f, verifyPaint)
        canvas.drawText("✓ Automatic Shift Duration & Time Tracking", width / 2f, featTop + 145f, verifyPaint)
        canvas.drawText("✓ Real-Time Sync to Manager & Parent Dashboard", width / 2f, featTop + 185f, verifyPaint)

        
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 20f
            textAlign = Align.CENTER
        }
        val phoneContact = library?.phone ?: ""
        canvas.drawText("Reception Desk Contact: $phoneContact | Powered by LibDesk Production OS", width / 2f, height - margin - 40f, footerPaint)

        return bitmap
    }
}
