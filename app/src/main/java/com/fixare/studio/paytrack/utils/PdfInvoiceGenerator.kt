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

        // Margins
        val startX = 50f
        val endX = 545f
        var currentY = 50f

        // Header: INVOICE
        paint.color = primaryColor
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        val title = if (payment.status == PaymentStatus.PAID) "INVOICE" else "INVOICE (PENDING)"
        canvas.drawText(title, endX, currentY + 20, paint)
        
        // App Name
        paint.color = lightGray
        paint.textSize = 12f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("PayTrack", startX, currentY, paint)
        currentY += 20

        // Company Name (Freelancer)
        paint.color = darkGray
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val displayCompanyName = if (companyName.isNotBlank()) companyName else "PayTrack Freelancer"
        canvas.drawText(displayCompanyName, startX, currentY + 20, paint)
        
        currentY += 25
        if (userName.isNotBlank()) {
            paint.textSize = 14f
            paint.color = Color.GRAY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(userName, startX, currentY, paint)
        }
        
        currentY += 60

        // Separator
        paint.color = primaryColor
        paint.strokeWidth = 2f
        canvas.drawLine(startX, currentY, endX, currentY, paint)
        
        currentY += 40

        // Invoice Details
        paint.color = darkGray
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val dateStr = dateFormat.format(Date(payment.date))
        
        // Bill To
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILL TO:", startX, currentY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        currentY += 20
        canvas.drawText(client.name, startX, currentY, paint)
        currentY += 20
        canvas.drawText(client.projectName, startX, currentY, paint)
        
        // Invoice Meta (Right side)
        val metaX = 400f
        val metaY = currentY - 40
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Invoice #:", metaX, metaY, paint)
        canvas.drawText("Date:", metaX, metaY + 20, paint)
        canvas.drawText("Status:", metaX, metaY + 40, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(payment.id.toString().padStart(6, '0'), endX, metaY, paint)
        canvas.drawText(dateStr, endX, metaY + 20, paint)
        
        // Status Color
        val statusColor = when(payment.status) {
            PaymentStatus.PAID -> primaryColor
            PaymentStatus.PENDING -> warningColor
            PaymentStatus.OVERDUE -> errorColor
        }
        paint.color = statusColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(payment.status.name, endX, metaY + 40, paint)
        paint.color = darkGray
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT

        currentY += 80

        // Table Header
        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(startX, currentY, endX, currentY + 30, paint)
        
        paint.color = Color.WHITE
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
        
        // Use original currency and amount if available, otherwise fallback to local converted amount
        val displayAmount = payment.originalAmount ?: payment.amount
        val displayCurrency = payment.originalCurrency ?: client.currency // Fallback to client default currency if original not stored yet (older records)
        
        val amountStr = String.format(Locale.US, "%s %.2f", displayCurrency, displayAmount)
        canvas.drawText(amountStr, endX - 10, currentY, paint)
        paint.textAlign = Paint.Align.LEFT

        // Line below item
        paint.color = lightGray
        paint.strokeWidth = 1f
        canvas.drawLine(startX, currentY + 15, endX, currentY + 15, paint)
        
        currentY += rowHeight

        // Total
        currentY += 20
        paint.color = darkGray
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        val totalLabel = "TOTAL"
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(totalLabel, endX - 120, currentY, paint)
        
        paint.color = primaryColor
        paint.textSize = 18f
        canvas.drawText(amountStr, endX - 10, currentY, paint)

        // Footer
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
}
