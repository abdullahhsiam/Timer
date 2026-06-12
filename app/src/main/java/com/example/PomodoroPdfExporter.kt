package com.example

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.persistence.PomodoroDailySummary
import com.example.persistence.PomodoroSessionLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PomodoroPdfExporter {

    fun exportToPdfAndShare(
        context: Context,
        summaries: List<PomodoroDailySummary>,
        sessionLogs: List<PomodoroSessionLog>
    ) {
        val pdfDocument = PdfDocument()

        // Page Configurations (A4: 595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val printableWidth = pageWidth - (margin * 2) // 515f
        val footerY = pageHeight - margin // 802f

        var pageNumber = 1
        var currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(currentPageInfo)
        var canvas = currentPage.canvas

        // Paint definitions
        val titlePaint = Paint().apply {
            color = Color.parseColor("#121212")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#5A5A5A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val h1Paint = Paint().apply {
            color = Color.parseColor("#4A154B") // Slate/accent color
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val basePaint = Paint().apply {
            color = Color.parseColor("#1E1E1E")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val headerTextPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#6C5B7B") // Muted Lavender slate purple
            style = Paint.Style.FILL
        }
        val rowBgAltPaint = Paint().apply {
            color = Color.parseColor("#F8F7FA")
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#DDDDDD")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        val footerPaint = Paint().apply {
            color = Color.parseColor("#888888")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        var currentY = margin + 10f

        // Helper to finalize a page and start a new one
        fun forceNewPage() {
            // Draw Footer before quitting page
            canvas.drawText("Luminous Pomodoro Summary Report — Generated Offline", margin, footerY - 5f, footerPaint)
            val pageStr = "Page $pageNumber"
            val textWidth = footerPaint.measureText(pageStr)
            canvas.drawText(pageStr, pageWidth - margin - textWidth, footerY - 5f, footerPaint)

            pdfDocument.finishPage(currentPage)
            pageNumber++
            currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(currentPageInfo)
            canvas = currentPage.canvas
            currentY = margin + 15f
        }

        // Helper to check vertical space
        fun checkSpace(heightNeeded: Float) {
            if (currentY + heightNeeded > footerY - 20f) {
                forceNewPage()
            }
        }

        // Format Helpers
        val sdfDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = sdfDate.format(Date())

        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun formatMs(ms: Long): String {
            val totalSec = ms / 1000
            val min = (totalSec / 60) % 60
            val hrs = totalSec / 3600
            if (hrs > 0) {
                return "${hrs}h ${min}m"
            }
            return "${min}m"
        }

        // --- TITLE ---
        checkSpace(60f)
        canvas.drawText("POMODORO REPORT", margin, currentY + 15f, titlePaint)
        canvas.drawText("Luminous Work & Focus Summary Engine", margin, currentY + 30f, subtitlePaint)
        canvas.drawText("Generated on: $dateStr (Local Offline Record)", margin, currentY + 43f, subtitlePaint)
        currentY += 55f

        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        currentY += 15f

        // --- SUMMARY CONTAINER ---
        checkSpace(90f)
        val totalDays = summaries.size
        val totalFocusSessions = summaries.sumOf { it.focusSessionsCompleted }
        val totalBreakSessions = summaries.sumOf { it.breakSessionsCompleted }
        val totalFocusMs = summaries.sumOf { it.focusTimeMs }
        val totalBreakMs = summaries.sumOf { it.breakTimeMs }
        val totalManualCount = summaries.sumOf { it.manualBreakCount }
        val lifetimeFocusHrs = totalFocusMs / 3600000.0

        val boxPaint = Paint().apply {
            color = Color.parseColor("#F3F1F6")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#D4CCD9")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 70f, 8f, 8f, boxPaint)
        canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 70f, 8f, 8f, borderPaint)

        val colW = printableWidth / 3
        
        // Col 1 Content
        canvas.drawText("FOCUS TIME", margin + 15f, currentY + 20f, subtitlePaint)
        canvas.drawText(formatMs(totalFocusMs), margin + 15f, currentY + 35f, h1Paint)
        canvas.drawText("Total Focus Hours: %.1f hrs".format(lifetimeFocusHrs), margin + 15f, currentY + 52f, basePaint)

        // Col 2 Content
        canvas.drawText("COMPLETED CYCLES", margin + colW + 15f, currentY + 20f, subtitlePaint)
        canvas.drawText("$totalFocusSessions Focus / $totalBreakSessions Breaks", margin + colW + 15f, currentY + 35f, h1Paint)
        canvas.drawText("Active Streak Days: $totalDays Days", margin + colW + 15f, currentY + 52f, basePaint)

        // Col 3 Content
        canvas.drawText("PAUSE & BREAKS", margin + (colW * 2) + 15f, currentY + 20f, subtitlePaint)
        canvas.drawText(formatMs(totalBreakMs), margin + (colW * 2) + 15f, currentY + 35f, h1Paint)
        canvas.drawText("Manual Interventions: $totalManualCount Breaks", margin + (colW * 2) + 15f, currentY + 52f, basePaint)

        currentY += 85f

        // --- DAILY SUMMARIES TABLE ---
        checkSpace(40f)
        canvas.drawText("DAILY PERFORMANCE HISTORIES", margin, currentY + 12f, h1Paint)
        currentY += 20f

        // Headers: Date, Focus Time, Break Time, Focus, Break, Manual, Total Focus Hours
        val thHeight = 16f
        val tdHeight = 15f
        checkSpace(thHeight + tdHeight)

        val dailyHeaders = arrayOf("Date", "Focus Time", "Break Time", "Focus", "Break", "Manual", "Focus Hrs")
        val dailyCols = floatArrayOf(120f, 100f, 100f, 50f, 50f, 50f, 45f) // Total: 515f

        // Draw headers bg
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + thHeight, headerBgPaint)
        var rowX = margin
        for (i in dailyHeaders.indices) {
            canvas.drawText(
                dailyHeaders[i],
                rowX + 6f,
                currentY + 11f,
                headerTextPaint
            )
            rowX += dailyCols[i]
        }
        currentY += thHeight

        // Fill summaries
        for ((idx, day) in summaries.withIndex()) {
            checkSpace(tdHeight)
            if (idx % 2 == 1) {
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + tdHeight, rowBgAltPaint)
            }
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)

            var cellX = margin
            
            // "Date"
            canvas.drawText(day.date, cellX + 6f, currentY + 10f, basePaint)
            cellX += dailyCols[0]

            // "Focus Time"
            canvas.drawText(formatMs(day.focusTimeMs), cellX + 6f, currentY + 10f, basePaint)
            cellX += dailyCols[1]

            // "Break Time"
            canvas.drawText(formatMs(day.breakTimeMs), cellX + 6f, currentY + 10f, basePaint)
            cellX += dailyCols[2]

            // "Focus"
            canvas.drawText(day.focusSessionsCompleted.toString(), cellX + 6f, currentY + 10f, basePaint)
            cellX += dailyCols[3]

            // "Break"
            canvas.drawText(day.breakSessionsCompleted.toString(), cellX + 6f, currentY + 10f, basePaint)
            cellX += dailyCols[4]

            // "Manual"
            canvas.drawText(day.manualBreakCount.toString(), cellX + 6f, currentY + 10f, basePaint)
            cellX += dailyCols[5]

            // "Focus Hrs"
            canvas.drawText("%.2f".format(day.totalFocusHours), cellX + 6f, currentY + 10f, basePaint)

            currentY += tdHeight
        }
        currentY += 15f

        // --- DETAILED CHRONOLOGICAL LOGS TABLE ---
        if (sessionLogs.isNotEmpty()) {
            checkSpace(40f)
            canvas.drawText("DETAILED FOCUS LOGS (CHRONOLOGICAL)", margin, currentY + 12f, h1Paint)
            currentY += 20f

            checkSpace(thHeight + tdHeight)
            val logHeaders = arrayOf("Date", "Session Type", "Time Interval (Start - End)", "Duration")
            val logCols = floatArrayOf(110f, 110f, 180f, 115f) // Total: 515f

            // Draw header
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + thHeight, headerBgPaint)
            var lRowX = margin
            for (i in logHeaders.indices) {
                canvas.drawText(
                    logHeaders[i],
                    lRowX + 6f,
                    currentY + 11f,
                    headerTextPaint
                )
                lRowX += logCols[i]
            }
            currentY += thHeight

            for ((idx, log) in sessionLogs.withIndex()) {
                checkSpace(tdHeight)
                if (idx % 2 == 1) {
                    canvas.drawRect(margin, currentY, pageWidth - margin, currentY + tdHeight, rowBgAltPaint)
                }
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)

                var cellX = margin
                
                // Date
                canvas.drawText(log.date, cellX + 6f, currentY + 10f, basePaint)
                cellX += logCols[0]

                // Session Type
                val typeDisp = when (log.sessionType) {
                    "FOCUS" -> "Focus Session"
                    "BREAK" -> "Standard Break"
                    "MANUAL_BREAK" -> "Manual Break"
                    else -> log.sessionType
                }
                canvas.drawText(typeDisp, cellX + 6f, currentY + 10f, basePaint)
                cellX += logCols[1]

                // Time Interval
                val startFormatted = sdfTime.format(Date(log.startTime))
                val endFormatted = sdfTime.format(Date(log.endTime))
                canvas.drawText("$startFormatted – $endFormatted", cellX + 6f, currentY + 10f, basePaint)
                cellX += logCols[2]

                // Duration
                canvas.drawText(formatMs(log.durationMs), cellX + 6f, currentY + 10f, basePaint)

                currentY += tdHeight
            }
        }

        // Draw Final Footer
        canvas.drawText("Luminous Pomodoro Summary Report — Generated Offline", margin, footerY - 5f, footerPaint)
        val finalPageStr = "Page $pageNumber"
        val textWidth = footerPaint.measureText(finalPageStr)
        canvas.drawText(finalPageStr, pageWidth - margin - textWidth, footerY - 5f, footerPaint)

        pdfDocument.finishPage(currentPage)

        // Write to Cache Directory for Sharing
        val cacheFile = File(context.cacheDir, "Pomodoro_Focus_Report.pdf")
        try {
            val fos = FileOutputStream(cacheFile)
            pdfDocument.writeTo(fos)
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        // Shared Intent Launching
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, cacheFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Luminous Pomodoro Focus Performance Report")
            putExtra(Intent.EXTRA_TEXT, "Here is my Pomodoro focus history and performance statistics generated from my Luminous Timer app!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Share Performance Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
