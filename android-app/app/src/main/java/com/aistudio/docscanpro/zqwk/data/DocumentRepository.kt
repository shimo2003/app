package com.aistudio.docscanpro.zqwk.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.aistudio.docscanpro.zqwk.model.ScanFilter
import com.aistudio.docscanpro.zqwk.model.ScannedDocument
import com.aistudio.docscanpro.zqwk.model.ScannedPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DocumentRepository(private val context: Context) {

    private val scanDir = File(context.filesDir, "scans").apply { mkdirs() }
    private val metaFile = File(context.filesDir, "documents_meta.json")

    private val _documents = MutableStateFlow<List<ScannedDocument>>(emptyList())
    val documents: StateFlow<List<ScannedDocument>> = _documents.asStateFlow()

    init {
        loadDocuments()
        if (_documents.value.isEmpty()) {
            seedSampleDocuments()
        }
    }

    private fun loadDocuments() {
        if (!metaFile.exists()) return
        try {
            val jsonStr = metaFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ScannedDocument>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val docId = obj.getString("id")
                val title = obj.getString("title")
                val createdAt = obj.getLong("createdAt")
                val updatedAt = obj.getLong("updatedAt")
                val tag = obj.optString("tag", "General")
                val note = obj.optString("note", "")

                val pagesArray = obj.getJSONArray("pages")
                val pagesList = mutableListOf<ScannedPage>()

                for (j in 0 until pagesArray.length()) {
                    val pageObj = pagesArray.getJSONObject(j)
                    val pageId = pageObj.getString("id")
                    val filePath = pageObj.getString("filePath")
                    val rotation = pageObj.optInt("rotationDegrees", 0)
                    val filterStr = pageObj.optString("filter", ScanFilter.ORIGINAL.name)
                    val filter = try {
                        ScanFilter.valueOf(filterStr)
                    } catch (e: Exception) {
                        ScanFilter.ORIGINAL
                    }
                    val timestamp = pageObj.optLong("timestamp", System.currentTimeMillis())

                    if (File(filePath).exists()) {
                        pagesList.add(ScannedPage(pageId, filePath, rotation, filter, timestamp))
                    }
                }

                if (pagesList.isNotEmpty()) {
                    list.add(
                        ScannedDocument(
                            id = docId,
                            title = title,
                            pages = pagesList,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            tag = tag,
                            note = note
                        )
                    )
                }
            }
            _documents.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveDocuments() {
        try {
            val jsonArray = JSONArray()
            for (doc in _documents.value) {
                val obj = JSONObject().apply {
                    put("id", doc.id)
                    put("title", doc.title)
                    put("createdAt", doc.createdAt)
                    put("updatedAt", doc.updatedAt)
                    put("tag", doc.tag)
                    put("note", doc.note)

                    val pagesArray = JSONArray()
                    for (page in doc.pages) {
                        val pageObj = JSONObject().apply {
                            put("id", page.id)
                            put("filePath", page.filePath)
                            put("rotationDegrees", page.rotationDegrees)
                            put("filter", page.filter.name)
                            put("timestamp", page.timestamp)
                        }
                        pagesArray.put(pageObj)
                    }
                    put("pages", pagesArray)
                }
                jsonArray.put(obj)
            }
            metaFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveBitmapAsPage(bitmap: Bitmap): ScannedPage {
        val pageId = UUID.randomUUID().toString()
        val file = File(scanDir, "page_$pageId.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return ScannedPage(
            id = pageId,
            filePath = file.absolutePath,
            filter = ScanFilter.MAGIC_COLOR
        )
    }

    fun createDocument(title: String, pages: List<ScannedPage>, tag: String = "General", note: String = ""): ScannedDocument {
        val doc = ScannedDocument(
            id = UUID.randomUUID().toString(),
            title = title,
            pages = pages,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            tag = tag,
            note = note
        )
        _documents.value = listOf(doc) + _documents.value
        saveDocuments()
        return doc
    }

    fun updateDocument(doc: ScannedDocument) {
        val updated = doc.copy(updatedAt = System.currentTimeMillis())
        _documents.value = _documents.value.map { if (it.id == doc.id) updated else it }
        saveDocuments()
    }

    fun deleteDocument(docId: String) {
        val target = _documents.value.find { it.id == docId }
        target?.pages?.forEach { page ->
            try {
                File(page.filePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _documents.value = _documents.value.filter { it.id != docId }
        saveDocuments()
    }

    fun updatePage(docId: String, pageId: String, rotation: Int, filter: ScanFilter) {
        val currentDoc = _documents.value.find { it.id == docId } ?: return
        val updatedPages = currentDoc.pages.map { page ->
            if (page.id == pageId) {
                page.copy(rotationDegrees = rotation, filter = filter)
            } else page
        }
        updateDocument(currentDoc.copy(pages = updatedPages))
    }

    fun removePage(docId: String, pageId: String) {
        val currentDoc = _documents.value.find { it.id == docId } ?: return
        val pageToRemove = currentDoc.pages.find { it.id == pageId }
        pageToRemove?.let {
            try {
                File(it.filePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val remaining = currentDoc.pages.filter { it.id != pageId }
        if (remaining.isEmpty()) {
            deleteDocument(docId)
        } else {
            updateDocument(currentDoc.copy(pages = remaining))
        }
    }

    // Creates beautiful sample document bitmaps so the app is instantly rich with data
    private fun seedSampleDocuments() {
        val invoiceBitmap = createSampleInvoiceBitmap()
        val invoicePage = saveBitmapAsPage(invoiceBitmap)
        createDocument(
            title = "Tech Equipment Invoice #4092",
            pages = listOf(invoicePage),
            tag = "Invoices",
            note = "Hardware purchase for workstation setup."
        )

        val contractPage1 = createSampleContractBitmap(1)
        val contractPage2 = createSampleContractBitmap(2)
        val p1 = saveBitmapAsPage(contractPage1)
        val p2 = saveBitmapAsPage(contractPage2)
        createDocument(
            title = "Freelance Service Agreement",
            pages = listOf(p1, p2),
            tag = "Contracts",
            note = "Signed consulting agreement for Q4."
        )
    }

    private fun createSampleInvoiceBitmap(): Bitmap {
        val width = 800
        val height = 1131 // ~A4 proportion
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.rgb(252, 252, 252))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header band
        paint.color = Color.rgb(15, 23, 42) // Slate 900
        canvas.drawRect(0f, 0f, width.toFloat(), 140f, paint)

        // Logo & Title
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.isFakeBoldText = true
        canvas.drawText("TECH MATRIX LABS", 48f, 75f, paint)

        paint.textSize = 18f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawText("Tax Invoice & Statement", 48f, 105f, paint)

        // Invoice badge
        paint.color = Color.rgb(2, 132, 199)
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("INVOICE #4092", 520f, 80f, paint)

        // Info columns
        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("BILLED TO:", 48f, 190f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 17f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Acme Corporation", 48f, 220f, paint)
        canvas.drawText("742 Evergreen Terrace", 48f, 245f, paint)
        canvas.drawText("Springfield, OR 97477", 48f, 270f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.isFakeBoldText = true
        canvas.drawText("DATE & TERMS:", 500f, 190f, paint)
        paint.isFakeBoldText = false
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Issued: Oct 20, 2026", 500f, 220f, paint)
        canvas.drawText("Due Date: Nov 04, 2026", 500f, 245f, paint)
        canvas.drawText("Payment: PayPal (shimo2003@gmail.com)", 500f, 270f, paint)

        // Table header
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(48f, 320f, (width - 48).toFloat(), 365f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("ITEM DESCRIPTION", 64f, 350f, paint)
        canvas.drawText("QTY", 450f, 350f, paint)
        canvas.drawText("PRICE", 550f, 350f, paint)
        canvas.drawText("TOTAL", 660f, 350f, paint)

        // Items
        val items = listOf(
            Triple("Dell UltraSharp 32\" 4K Monitor", "2", "$850.00"),
            Triple("Ergonomic Mechanical Keyboard", "2", "$340.00"),
            Triple("USB-C Thunderbolt Quad Docking Station", "1", "$220.00"),
            Triple("Noise Cancelling Wireless Headset", "2", "$460.00"),
            Triple("Professional On-Site Calibration", "1", "$150.00")
        )

        var y = 410f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 16f

        items.forEach { (desc, qty, price) ->
            canvas.drawText(desc, 64f, y, paint)
            canvas.drawText(qty, 460f, y, paint)
            canvas.drawText(price, 540f, y, paint)
            canvas.drawText(price, 650f, y, paint)

            paint.color = Color.rgb(226, 232, 240)
            canvas.drawLine(48f, y + 16f, (width - 48).toFloat(), y + 16f, paint)
            paint.color = Color.rgb(51, 65, 85)
            y += 48f
        }

        // Total block
        paint.color = Color.rgb(241, 245, 249)
        val totalRect = RectF(460f, y + 20f, (width - 48).toFloat(), y + 150f)
        canvas.drawRoundRect(totalRect, 8f, 8f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 16f
        canvas.drawText("Subtotal:", 480f, y + 55f, paint)
        canvas.drawText("Tax (8.25%):", 480f, y + 85f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.isFakeBoldText = true
        paint.textSize = 20f
        canvas.drawText("Total Due:", 480f, y + 125f, paint)

        canvas.drawText("$2,020.00", 640f, y + 55f, paint)
        canvas.drawText("$166.65", 655f, y + 85f, paint)
        paint.color = Color.rgb(2, 132, 199)
        canvas.drawText("$2,186.65", 620f, y + 125f, paint)

        // Watermark stamp
        paint.color = Color.argb(40, 16, 185, 129)
        paint.textSize = 38f
        paint.isFakeBoldText = true
        canvas.drawText("PAID IN FULL", 120f, y + 90f, paint)

        return bitmap
    }

    private fun createSampleContractBitmap(pageNumber: Int): Bitmap {
        val width = 800
        val height = 1131
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top decorative accent
        paint.color = Color.rgb(13, 148, 136) // Accent Teal
        canvas.drawRect(48f, 40f, (width - 48).toFloat(), 48f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("CONSULTING SERVICES AGREEMENT", 48f, 100f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("CONFIDENTIAL & PROPRIETARY • PAGE $pageNumber OF 2", 48f, 130f, paint)

        val paragraphs = if (pageNumber == 1) {
            listOf(
                "This Consulting Services Agreement (the \"Agreement\") is entered into as of October 2026, by and between the Client and Provider.",
                "1. SCOPE OF SERVICES: Provider agrees to perform technical architecture, software review, and digital workflow transformation as detailed in Exhibit A attached hereto.",
                "2. COMPENSATION & BILLING: Client shall compensate Provider according to the agreed hourly rate of \$125.00/hr, payable monthly via direct PayPal transfer to verified account shimo2003@gmail.com.",
                "3. TERM AND TERMINATION: This Agreement shall commence upon mutual execution and continue for a primary term of twelve (12) months unless terminated earlier pursuant to Section 4.",
                "4. INTELLECTUAL PROPERTY: All custom work product, designs, source code, and deliverables developed specifically for Client shall become the sole property of Client upon full payment."
            )
        } else {
            listOf(
                "5. CONFIDENTIALITY: Each party agrees to safeguard the confidential information of the other party with the same degree of care it uses for its own proprietary information.",
                "6. LIMITATION OF LIABILITY: Neither party shall be liable for incidental, indirect, or consequential damages arising from this agreement.",
                "7. GOVERNING LAW: This Agreement shall be construed in accordance with and governed by state commercial law.",
                "IN WITNESS WHEREOF, the parties hereto have executed this Services Agreement as of the date first above written.",
                "[SEAL & AUTHORIZED SIGNATURE OF CLIENT]",
                "[SEAL & AUTHORIZED SIGNATURE OF PROVIDER - VERIFIED]"
            )
        }

        var textY = 190f
        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 17f
        paint.isFakeBoldText = false

        paragraphs.forEach { text ->
            if (text.startsWith("[")) {
                paint.color = Color.rgb(2, 132, 199)
                paint.isFakeBoldText = true
                canvas.drawText(text, 48f, textY, paint)
                paint.color = Color.rgb(51, 65, 85)
                paint.isFakeBoldText = false
                textY += 60f
            } else {
                val words = text.split(" ")
                var line = ""
                for (w in words) {
                    if (paint.measureText("$line $w") < (width - 96)) {
                        line = if (line.isEmpty()) w else "$line $w"
                    } else {
                        canvas.drawText(line, 48f, textY, paint)
                        textY += 28f
                        line = w
                    }
                }
                if (line.isNotEmpty()) {
                    canvas.drawText(line, 48f, textY, paint)
                    textY += 46f
                }
            }
        }

        // Bottom page number
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 14f
        canvas.drawText("Document Reference: SC-2026-XQ | Page $pageNumber of 2", 48f, (height - 40).toFloat(), paint)

        return bitmap
    }
}
