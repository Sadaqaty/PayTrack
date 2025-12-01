package com.fixare.studio.paytrack.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.fixare.studio.paytrack.data.Client
import com.fixare.studio.paytrack.data.PaymentLog
import com.fixare.studio.paytrack.data.PaymentStatus
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfInvoiceGenerator(private val context: Context) {

    fun generateInvoice(
        payment: PaymentLog,
        client: Client,
        outputStream: OutputStream,
        userName: String,
        companyName: String
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in PostScript points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Colors
        val primaryColor = Color.rgb(46, 125, 50) // PrimaryGreen
        val darkGray = Color.rgb(60, 60, 60)
        val lightGray = Color.rgb(200, 200, 200)
        val warningColor = Color.rgb(255, 152, 0) // Orange
        val errorColor = Color.rgb(244, 67, 54) // Red
        val white = Color.WHITE

        // Margins
        val startX = 50f
        val endX = 545f
        var currentY = 50f

        // -----------------------------------------------------------------------
        // HEADER SECTION
        // -----------------------------------------------------------------------

        // App Name (PayTrack) - Prominent Green Header
        paint.color = primaryColor
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("PayTrack", startX, currentY + 20, paint)

        // INVOICE Title (Right aligned)
        paint.color = darkGray
        paint.textSize = 24f
        paint.textAlign = Paint.Align.RIGHT
        val title = if (payment.status == PaymentStatus.PAID) "INVOICE" else "INVOICE (PENDING)"
        canvas.drawText(title, endX, currentY + 20, paint)

        currentY += 50

        // Header Divider
        paint.color = primaryColor
        paint.strokeWidth = 2f
        canvas.drawLine(startX, currentY, endX, currentY, paint)
        
        currentY += 40

        // -----------------------------------------------------------------------
        // COMPANY / USER DETAILS SECTION
        // -----------------------------------------------------------------------

        val leftColumnX = startX
        val rightColumnX = 350f

        // FROM (User/Company Details)
        paint.color = lightGray
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FROM:", leftColumnX, currentY, paint)

        currentY += 15

        paint.color = darkGray
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        // Company Name takes precedence if available
        if (companyName.isNotBlank()) {
            canvas.drawText(companyName, leftColumnX, currentY, paint)
            currentY += 20
        }

        // User Name
        if (userName.isNotBlank()) {
            paint.textSize = 14f
            paint.color = if (companyName.isNotBlank()) Color.GRAY else darkGray
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (companyName.isNotBlank()) Typeface.NORMAL else Typeface.BOLD)
            canvas.drawText(userName, leftColumnX, currentY, paint)
            currentY += 20
        } else if (companyName.isBlank()) {
             // Fallback if absolutely nothing is set
             canvas.drawText("PayTrack Freelancer", leftColumnX, currentY, paint)
             currentY += 20
        }

        // Reset Y for Right Column (Invoice Meta)
        val metaStartY = 90f + 15f // Roughly where "FROM" started + offset
        var metaY = metaStartY
        
        // Invoice Details (Right side)
        paint.color = darkGray
        paint.textSize = 12f
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        // Labels
        val labelX = endX - 100f
        val valueX = endX

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Invoice #:", labelX, metaY, paint)
        canvas.drawText("Date:", labelX, metaY + 20, paint)
        canvas.drawText("Status:", labelX, metaY + 40, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.RIGHT
        
        // Values
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val dateStr = dateFormat.format(Date(payment.date))
        
        canvas.drawText(payment.id.toString().padStart(6, '0'), valueX, metaY, paint)
        canvas.drawText(dateStr, valueX, metaY + 20, paint)
        
        val statusColor = when(payment.status) {
            PaymentStatus.PAID -> primaryColor
            PaymentStatus.PENDING -> warningColor
            PaymentStatus.OVERDUE -> errorColor
        }
        paint.color = statusColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(payment.status.name, valueX, metaY + 40, paint)

        // Move currentY down to clear both columns
        currentY = maxOf(currentY, metaY + 60) + 30

        // -----------------------------------------------------------------------
        // BILL TO SECTION
        // -----------------------------------------------------------------------

        paint.color = lightGray
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILL TO:", startX, currentY, paint)
        
        currentY += 20
        paint.color = darkGray
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(client.name, startX, currentY, paint)
        
        currentY += 20
        paint.textSize = 14f
        paint.color = Color.GRAY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(client.projectName, startX, currentY, paint)

        currentY += 50

        // -----------------------------------------------------------------------
        // TABLE SECTION
        // -----------------------------------------------------------------------

        // Table Header Background
        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(startX, currentY, endX, currentY + 30, paint)
        
        // Table Header Text
        paint.color = white
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        canvas.drawText("DESCRIPTION", startX + 10, currentY + 20, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMOUNT", endX - 10, currentY + 20, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 30

        // Table Item
        paint.color = darkGray
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        val rowHeight = 40f
        currentY += 25
        
        val description = "Payment for ${client.projectName} (${client.paymentCycle.name.lowercase()})"
        canvas.drawText(description, startX + 10, currentY, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        
        // Use original currency and amount if available
        val displayAmount = payment.originalAmount ?: payment.amount
        val displayCurrency = payment.originalCurrency ?: client.currency
        
        val amountStr = String.format(Locale.US, "%s %.2f", displayCurrency, displayAmount)
        canvas.drawText(amountStr, endX - 10, currentY, paint)
        paint.textAlign = Paint.Align.LEFT

        // Line below item
        paint.color = lightGray
        paint.strokeWidth = 1f
        canvas.drawLine(startX, currentY + 15, endX, currentY + 15, paint)
        
        currentY += rowHeight + 20

        // -----------------------------------------------------------------------
        // TOTAL SECTION
        // -----------------------------------------------------------------------

        paint.color = darkGray
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        val totalLabel = "TOTAL"
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(totalLabel, endX - 150, currentY, paint)
        
        paint.color = primaryColor
        paint.textSize = 20f
        canvas.drawText(amountStr, endX - 10, currentY, paint)

        // -----------------------------------------------------------------------
        // FOOTER SECTION
        // -----------------------------------------------------------------------
        
        val footerY = 800f
        paint.color = lightGray
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Generated by PayTrack App", 595f / 2, footerY, paint)

        pdfDocument.finishPage(page)
        
        try {
            pdfDocument.writeTo(outputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }
    
    private fun maxOf(a: Float, b: Float): Float {
        return if (a > b) a else b
    }
}
