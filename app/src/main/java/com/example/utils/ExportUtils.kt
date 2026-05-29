package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object ExportUtils {
    
    /**
     * Generates a beautifully typeset multi-page PDF document
     */
    fun generatePdf(context: Context, title: String, contentText: String): File {
        val pdfDocument = PdfDocument()
        
        // A4 Paper Dimensions: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        
        val paint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val titlePaint = Paint().apply {
            color = Color.rgb(139, 90, 43) // Warm bronze ink
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val subtitlePaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.8f
        }

        val cleanText = cleanMarkdownForPrint(contentText)
        val paragraphs = cleanText.split("\n\n")
        
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val leftMargin = 54f
        val rightMargin = 54f
        val topMargin = 54f
        val bottomMargin = 60f
        
        val contentWidth = pageWidth - leftMargin - rightMargin
        
        var currentY = topMargin + 40f
        
        // Render Title Block on Page 1
        canvas.drawText(title, leftMargin, currentY, titlePaint)
        currentY += 18f
        canvas.drawText("Generated via MuseWriter Editorial Suite", leftMargin, currentY, subtitlePaint)
        currentY += 30f
        canvas.drawLine(leftMargin, currentY, pageWidth - rightMargin, currentY, linePaint)
        currentY += 35f

        // Text wrapping helper
        fun drawWrappedParagraph(para: String) {
            val words = para.split(Regex("\\s+"))
            var lineString = ""
            val lineHeight = 19f
            
            for (word in words) {
                val testLine = if (lineString.isEmpty()) word else "$lineString $word"
                val textWidth = paint.measureText(testLine)
                if (textWidth <= contentWidth) {
                    lineString = testLine
                } else {
                    // Draw Line
                    if (currentY + lineHeight > pageHeight - bottomMargin) {
                        drawFooter(canvas, pageNum, pageWidth, pageHeight, bottomMargin)
                        pdfDocument.finishPage(page)
                        
                        pageNum++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = topMargin
                        
                        // Header
                        canvas.drawText(title.uppercase(), leftMargin, currentY - 14f, headerPaint)
                        canvas.drawLine(leftMargin, currentY - 8f, pageWidth - rightMargin, currentY - 8f, linePaint)
                        currentY += 15f
                    }
                    canvas.drawText(lineString, leftMargin, currentY, paint)
                    currentY += lineHeight
                    lineString = word
                }
            }
            
            if (lineString.isNotEmpty()) {
                if (currentY + lineHeight > pageHeight - bottomMargin) {
                    drawFooter(canvas, pageNum, pageWidth, pageHeight, bottomMargin)
                    pdfDocument.finishPage(page)
                    
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = topMargin
                    
                    // Header
                    canvas.drawText(title.uppercase(), leftMargin, currentY - 14f, headerPaint)
                    canvas.drawLine(leftMargin, currentY - 8f, pageWidth - rightMargin, currentY - 8f, linePaint)
                    currentY += 15f
                }
                canvas.drawText(lineString, leftMargin, currentY, paint)
                currentY += lineHeight
            }
        }
        
        for (p in paragraphs) {
            if (p.trim().isEmpty()) continue
            
            // Render Headings slightly larger
            if (p.trim().startsWith("#")) {
                val depth = p.takeWhile { it == '#' }.length
                val text = p.dropWhile { it == '#' || it == ' ' }.trim()
                
                val hPaint = Paint().apply {
                    color = Color.rgb(139, 90, 43)
                    textSize = if (depth == 1) 18f else 14f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                
                if (currentY + 30f > pageHeight - bottomMargin) {
                    drawFooter(canvas, pageNum, pageWidth, pageHeight, bottomMargin)
                    pdfDocument.finishPage(page)
                    
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = topMargin
                    
                    canvas.drawText(title.uppercase(), leftMargin, currentY - 14f, headerPaint)
                    canvas.drawLine(leftMargin, currentY - 8f, pageWidth - rightMargin, currentY - 8f, linePaint)
                    currentY += 15f
                }
                currentY += 15f
                canvas.drawText(text, leftMargin, currentY, hPaint)
                currentY += 22f
            } else {
                drawWrappedParagraph(p.trim())
                currentY += 12f // Extra spacing between paragraph blocks
            }
        }
        
        drawFooter(canvas, pageNum, pageWidth, pageHeight, bottomMargin)
        pdfDocument.finishPage(page)
        
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_")
        val tempFile = File(context.cacheDir, "$sanitizedTitle.pdf")
        val outputStream = FileOutputStream(tempFile)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()
        
        return tempFile
    }
    
    private fun drawFooter(canvas: Canvas, pageNum: Int, pageWidth: Int, pageHeight: Int, bottomMargin: Float) {
        val paint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Page $pageNum", pageWidth / 2f, pageHeight - bottomMargin + 25f, paint)
    }
    
    /**
     * Generates a fully compliant formatted DOCX file representation via HTML template layout.
     * Ready for styling, paragraph spacing, indentations, and Georgia font setting.
     */
    fun generateDocx(context: Context, title: String, contentText: String): File {
        val cleanText = cleanMarkdownForPrint(contentText)
        val paragraphs = cleanText.split("\n\n")
        
        val htmlContent = StringBuilder()
        htmlContent.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>$title</title>
                <style>
                    body {
                        font-family: 'Times New Roman', Times, serif;
                        line-height: 1.6;
                        margin: 1.2in;
                        color: #111111;
                        font-size: 11pt;
                    }
                    h1 {
                        color: #8B5A2B;
                        font-family: Georgia, serif;
                        font-size: 24pt;
                        text-align: center;
                        margin-bottom: 24pt;
                    }
                    h2 {
                        color: #8B5A2B;
                        font-family: Georgia, serif;
                        font-size: 16pt;
                        margin-top: 20pt;
                        margin-bottom: 10pt;
                        border-bottom: 1px solid #EAEAEA;
                        padding-bottom: 4px;
                    }
                    h3 {
                        font-family: Georgia, serif;
                        font-size: 13pt;
                        margin-top: 15pt;
                        margin-bottom: 8pt;
                    }
                    p {
                        margin-top: 0;
                        margin-bottom: 12pt;
                        text-align: justify;
                        text-indent: 0.5in;
                    }
                    p.meta {
                        text-indent: 0 !important;
                        text-align: center;
                        color: #555555;
                        font-style: italic;
                        font-size: 10pt;
                        margin-bottom: 30pt;
                    }
                    hr {
                        border: 0;
                        border-top: 1px solid #CCCCCC;
                        margin: 30pt 0;
                    }
                </style>
            </head>
            <body>
                <h1>$title</h1>
                <p class="meta">Generated via MuseWriter Editorial Suite</p>
                <hr/>
        """.trimIndent())
        
        for (para in paragraphs) {
            val trimmed = para.trim()
            if (trimmed.isEmpty()) continue
            
            if (trimmed.startsWith("#")) {
                val depth = trimmed.takeWhile { it == '#' }.length
                val text = trimmed.dropWhile { it == '#' || it == ' ' }.trim()
                if (depth == 1) {
                    htmlContent.append("<h2>$text</h2>\n")
                } else {
                    htmlContent.append("<h3>$text</h3>\n")
                }
            } else {
                htmlContent.append("<p>$trimmed</p>\n")
            }
        }
        
        htmlContent.append("""
            </body>
            </html>
        """.trimIndent())
        
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_")
        val tempFile = File(context.cacheDir, "$sanitizedTitle.docx")
        tempFile.writeText(htmlContent.toString())
        return tempFile
    }
    
    private fun cleanMarkdownForPrint(text: String): String {
        return text
            // Strip structural markdown tags to look clean on sheets
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
    }
}
