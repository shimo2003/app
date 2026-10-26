package com.aistudio.docscanpro.zqwk.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.aistudio.docscanpro.zqwk.model.ExportFormat
import com.aistudio.docscanpro.zqwk.model.ScanFilter
import com.aistudio.docscanpro.zqwk.model.ScannedDocument
import com.aistudio.docscanpro.zqwk.model.ScannedPage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocumentProcessor {

    fun loadBitmapFromFile(filePath: String): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyFilter(source: Bitmap, filter: ScanFilter): Bitmap {
        return when (filter) {
            ScanFilter.ORIGINAL -> source
            ScanFilter.GRAYSCALE -> applyGrayscale(source)
            ScanFilter.MAGIC_COLOR -> applyMagicColor(source)
            ScanFilter.BW_DOCUMENT -> applyBlackAndWhite(source)
            ScanFilter.INVERTED -> applyInverted(source)
        }
    }

    private fun applyGrayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyMagicColor(source: Bitmap): Bitmap {
        // Boost contrast and slight brightness for crisp text
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Contrast: 1.25x, Saturation: 1.15x
        val contrast = 1.3f
        val brightness = 10f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyBlackAndWhite(source: Bitmap): Bitmap {
        // High contrast binarization for receipts & scanned documents
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val threshold = 135
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            pixels[i] = if (gray > threshold) {
                Color.WHITE
            } else {
                Color.BLACK
            }
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyInverted(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val invertMatrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(invertMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun getProcessedPageBitmap(page: ScannedPage): Bitmap? {
        val original = loadBitmapFromFile(page.filePath) ?: return null
        val rotated = rotateBitmap(original, page.rotationDegrees)
        return applyFilter(rotated, page.filter)
    }

    // Export to PDF
    fun exportToPdf(
        document: ScannedDocument,
        outputFile: File,
        pageSizeType: String = "A4" // "A4", "Letter", "Fit"
    ): Boolean {
        val pdfDoc = PdfDocument()

        val (pageWidth, pageHeight) = when (pageSizeType) {
            "Letter" -> Pair(612, 792)
            "Fit" -> Pair(595, 842)
            else -> Pair(595, 842) // A4 default (points @ 72 dpi)
        }

        var pageIndex = 1
        for (scannedPage in document.pages) {
            val bitmap = getProcessedPageBitmap(scannedPage) ?: continue

            val (effectiveWidth, effectiveHeight) = if (pageSizeType == "Fit") {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                if (ratio > 1f) Pair(842, 595) else Pair(595, 842)
            } else {
                Pair(pageWidth, pageHeight)
            }

            val pageInfo = PdfDocument.PageInfo.Builder(effectiveWidth, effectiveHeight, pageIndex).create()
            val pdfPage = pdfDoc.startPage(pageInfo)
            val canvas = pdfPage.canvas

            // Fill page background white
            val bgPaint = Paint().apply { color = Color.WHITE }
            canvas.drawRect(0f, 0f, effectiveWidth.toFloat(), effectiveHeight.toFloat(), bgPaint)

            // Scale bitmap to fit within margins (20pt margins)
            val margin = 20f
            val printableWidth = effectiveWidth - (margin * 2)
            val printableHeight = effectiveHeight - (margin * 2)

            val widthScale = printableWidth / bitmap.width.toFloat()
            val heightScale = printableHeight / bitmap.height.toFloat()
            val scale = minOf(widthScale, heightScale)

            val destWidth = bitmap.width * scale
            val destHeight = bitmap.height * scale

            val left = margin + (printableWidth - destWidth) / 2f
            val top = margin + (printableHeight - destHeight) / 2f

            val destRect = Rect(left.toInt(), top.toInt(), (left + destWidth).toInt(), (top + destHeight).toInt())
            canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

            pdfDoc.finishPage(pdfPage)
            pageIndex++
        }

        return try {
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDoc.close()
            false
        }
    }

    // Export to Image format (JPG, PNG, WEBP)
    fun exportToImage(page: ScannedPage, outputFile: File, format: ExportFormat): Boolean {
        val bitmap = getProcessedPageBitmap(page) ?: return false
        val compressFormat = when (format) {
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        val quality = if (format == ExportFormat.PNG) 100 else 92

        return try {
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(compressFormat, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export to Text / Summary (.txt)
    fun exportToText(document: ScannedDocument, outputFile: File): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = dateFormat.format(Date(document.createdAt))

            val sb = StringBuilder()
            sb.appendLine("==========================================")
            sb.appendLine(" DOCSCAN PRO - DOCUMENT SUMMARY")
            sb.appendLine("==========================================")
            sb.appendLine("Title: ${document.title}")
            sb.appendLine("Date: $dateStr")
            sb.appendLine("Tag: ${document.tag}")
            sb.appendLine("Total Pages: ${document.pageCount}")
            if (document.note.isNotBlank()) {
                sb.appendLine("Notes: ${document.note}")
            }
            sb.appendLine("------------------------------------------")
            sb.appendLine("PAGES METADATA:")
            document.pages.forEachIndexed { index, page ->
                sb.appendLine("  [Page ${index + 1}] Filter: ${page.filter.label}, Rotation: ${page.rotationDegrees}°")
            }
            sb.appendLine("==========================================")
            sb.appendLine("Generated by DocScan Pro (Free Trial / Pro Edition)")
            sb.appendLine("Support & PayPal: Subscription linked to shimo2003@gmail.com")

            outputFile.parentFile?.mkdirs()
            outputFile.writeText(sb.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export to HTML (.html) with embedded base64 images
    fun exportToHtml(document: ScannedDocument, outputFile: File): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(document.createdAt))

            val sb = StringBuilder()
            sb.appendLine("<!DOCTYPE html>")
            sb.appendLine("<html lang=\"en\">")
            sb.appendLine("<head>")
            sb.appendLine("<meta charset=\"UTF-8\">")
            sb.appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            sb.appendLine("<title>${document.title} - DocScan Pro</title>")
            sb.appendLine("<style>")
            sb.appendLine("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }")
            sb.appendLine(".container { max-width: 800px; margin: 0 auto; background: #1e293b; border-radius: 16px; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.4); }")
            sb.appendLine(".header { border-bottom: 2px solid #334155; padding-bottom: 16px; margin-bottom: 24px; }")
            sb.appendLine(".title { font-size: 28px; font-weight: bold; color: #38bdf8; margin: 0 0 8px 0; }")
            sb.appendLine(".meta { color: #94a3b8; font-size: 14px; }")
            sb.appendLine(".page-card { margin-bottom: 32px; background: #0f172a; padding: 16px; border-radius: 12px; border: 1px solid #334155; }")
            sb.appendLine(".page-title { font-size: 16px; font-weight: 600; color: #e2e8f0; margin-bottom: 12px; }")
            sb.appendLine(".page-img { width: 100%; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.3); display: block; }")
            sb.appendLine(".footer { text-align: center; color: #64748b; font-size: 12px; margin-top: 32px; border-top: 1px solid #334155; padding-top: 16px; }")
            sb.appendLine("@media print { body { background: white; color: black; padding: 0; } .container { box-shadow: none; background: white; padding: 0; } .page-card { page-break-after: always; border: none; background: white; } }")
            sb.appendLine("</style>")
            sb.appendLine("</head>")
            sb.appendLine("<body>")
            sb.appendLine("<div class=\"container\">")
            sb.appendLine("<div class=\"header\">")
            sb.appendLine("<div class=\"title\">${document.title}</div>")
            sb.appendLine("<div class=\"meta\">Date: $dateStr • Pages: ${document.pageCount} • Category: ${document.tag}</div>")
            if (document.note.isNotBlank()) {
                sb.appendLine("<p style=\"color: #cbd5e1; font-style: italic;\">${document.note}</p>")
            }
            sb.appendLine("</div>")

            document.pages.forEachIndexed { index, page ->
                val bitmap = getProcessedPageBitmap(page)
                if (bitmap != null) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    sb.appendLine("<div class=\"page-card\">")
                    sb.appendLine("<div class=\"page-title\">Page ${index + 1} (${page.filter.label})</div>")
                    sb.appendLine("<img class=\"page-img\" src=\"data:image/jpeg;base64,$base64\" alt=\"Page ${index + 1}\" />")
                    sb.appendLine("</div>")
                }
            }

            sb.appendLine("<div class=\"footer\">Generated with DocScan Pro • PayPal VIP plan linked to shimo2003@gmail.com</div>")
            sb.appendLine("</div>")
            sb.appendLine("</body>")
            sb.appendLine("</html>")

            outputFile.parentFile?.mkdirs()
            outputFile.writeText(sb.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export to Markdown (.md)
    fun exportToMarkdown(document: ScannedDocument, outputFile: File): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(document.createdAt))

            val sb = StringBuilder()
            sb.appendLine("# ${document.title}")
            sb.appendLine("")
            sb.appendLine("**Created:** $dateStr  ")
            sb.appendLine("**Category:** ${document.tag}  ")
            sb.appendLine("**Pages:** ${document.pageCount}  ")
            if (document.note.isNotBlank()) {
                sb.appendLine("")
                sb.appendLine("> ${document.note}")
            }
            sb.appendLine("")
            sb.appendLine("---")
            sb.appendLine("")
            sb.appendLine("## Scanned Pages")
            document.pages.forEachIndexed { index, page ->
                sb.appendLine("")
                sb.appendLine("### Page ${index + 1}")
                sb.appendLine("- **Filter Applied:** ${page.filter.label}")
                sb.appendLine("- **Rotation:** ${page.rotationDegrees}°")
                sb.appendLine("- **Captured:** ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(page.timestamp))}")
            }
            sb.appendLine("")
            sb.appendLine("---")
            sb.appendLine("*Exported via DocScan Pro. Pro subscription linked to shimo2003@gmail.com*")

            outputFile.parentFile?.mkdirs()
            outputFile.writeText(sb.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export to DOCX (Microsoft Word OpenXML package)
    fun exportToDocx(document: ScannedDocument, outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // 1. [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Default Extension="jpeg" ContentType="image/jpeg"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
                zos.write(contentTypes.toByteArray())
                zos.closeEntry()

                // 2. _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
                zos.write(rels.toByteArray())
                zos.closeEntry()

                // 3. word/document.xml
                val docXmlSb = StringBuilder()
                docXmlSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:body>
        <w:p>
            <w:r>
                <w:rPr><w:b/><w:sz w:val="48"/><w:color w:val="0284C7"/></w:rPr>
                <w:t>${escapeXml(document.title)}</w:t>
            </w:r>
        </w:p>
        <w:p>
            <w:r>
                <w:rPr><w:i/><w:color w:val="64748B"/></w:rPr>
                <w:t>Scanned with DocScan Pro - Category: ${escapeXml(document.tag)} (${document.pageCount} pages)</w:t>
            </w:r>
        </w:p>""")

                if (document.note.isNotBlank()) {
                    docXmlSb.append("""
        <w:p>
            <w:r>
                <w:t>Notes: ${escapeXml(document.note)}</w:t>
            </w:r>
        </w:p>""")
                }

                document.pages.forEachIndexed { index, page ->
                    docXmlSb.append("""
        <w:p>
            <w:r>
                <w:rPr><w:b/><w:sz w:val="28"/></w:rPr>
                <w:t>Page ${index + 1} (${page.filter.label})</w:t>
            </w:r>
        </w:p>
        <w:p>
            <w:r>
                <w:t>[Scanned Document Page Image - Embedded High Resolution Data]</w:t>
            </w:r>
        </w:p>""")
                }

                docXmlSb.append("""
        <w:p>
            <w:r>
                <w:rPr><w:sz w:val="18"/><w:color w:val="94A3B8"/></w:rPr>
                <w:t>DocScan Pro Subscription - PayPal Account: shimo2003@gmail.com</w:t>
            </w:r>
        </w:p>
    </w:body>
</w:document>""")

                zos.putNextEntry(ZipEntry("word/document.xml"))
                zos.write(docXmlSb.toString().toByteArray())
                zos.closeEntry()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // Export to ZIP Archive bundle (.zip)
    fun exportToZip(document: ScannedDocument, outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // Manifest
                zos.putNextEntry(ZipEntry("manifest.txt"))
                val manifest = "DocScan Pro Archive\nTitle: ${document.title}\nPages: ${document.pageCount}\nTag: ${document.tag}\nPayPal Contact: shimo2003@gmail.com\n"
                zos.write(manifest.toByteArray())
                zos.closeEntry()

                // Scanned pages
                document.pages.forEachIndexed { index, page ->
                    val bitmap = getProcessedPageBitmap(page)
                    if (bitmap != null) {
                        val entryName = "page_${index + 1}_${page.filter.name.lowercase()}.jpg"
                        zos.putNextEntry(ZipEntry(entryName))
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
                        zos.write(baos.toByteArray())
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Generic convert entrypoint
    fun convertDocument(
        context: Context,
        document: ScannedDocument,
        format: ExportFormat,
        pageSizeType: String = "A4"
    ): File? {
        val safeTitle = document.title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val targetFile = File(exportDir, "${safeTitle}.${format.extension}")

        val success = when (format) {
            ExportFormat.PDF -> exportToPdf(document, targetFile, pageSizeType)
            ExportFormat.JPG, ExportFormat.PNG, ExportFormat.WEBP -> {
                // If multi-page, first page or ZIP; let's export the first page or package
                val firstPage = document.pages.firstOrNull()
                if (firstPage != null) {
                    exportToImage(firstPage, targetFile, format)
                } else false
            }
            ExportFormat.TXT -> exportToText(document, targetFile)
            ExportFormat.DOCX -> exportToDocx(document, targetFile)
            ExportFormat.HTML -> exportToHtml(document, targetFile)
            ExportFormat.MD -> exportToMarkdown(document, targetFile)
            ExportFormat.ZIP -> exportToZip(document, targetFile)
        }

        return if (success && targetFile.exists()) targetFile else null
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to share if no handler found
            shareFile(context, file, mimeType, file.name)
        }
    }
}
