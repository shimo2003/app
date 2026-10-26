package com.example.util

import com.example.model.SerialBatch
import com.example.model.SerialItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelGenerator {

    /**
     * Generates a fully compliant Microsoft Excel OpenXML (.xlsx) file
     * containing styled headers, automatic column widths, and itemized rows.
     */
    fun exportToXlsx(batch: SerialBatch, outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // 1. [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
                zos.write(contentTypes.toByteArray())
                zos.closeEntry()

                // 2. _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
                zos.write(rels.toByteArray())
                zos.closeEntry()

                // 3. xl/_rels/workbook.xml.rels
                zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
                val wbRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
                zos.write(wbRels.toByteArray())
                zos.closeEntry()

                // 4. xl/workbook.xml
                zos.putNextEntry(ZipEntry("xl/workbook.xml"))
                val wb = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="Scanned Serials" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>"""
                zos.write(wb.toByteArray())
                zos.closeEntry()

                // 5. xl/styles.xml
                zos.putNextEntry(ZipEntry("xl/styles.xml"))
                val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="2">
        <font>
            <sz val="11"/>
            <color theme="1"/>
            <name val="Calibri"/>
        </font>
        <font>
            <b/>
            <sz val="11"/>
            <color rgb="FFFFFFFF"/>
            <name val="Calibri"/>
        </font>
    </fonts>
    <fills count="3">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill>
            <patternFill patternType="solid">
                <fgColor rgb="FF0284C7"/>
            </patternFill>
        </fill>
    </fills>
    <borders count="1">
        <border>
            <left/><right/><top/><bottom/><diagonal/>
        </border>
    </borders>
    <cellXfs count="2">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
    </cellXfs>
</styleSheet>"""
                zos.write(styles.toByteArray())
                zos.closeEntry()

                // 6. xl/worksheets/sheet1.xml
                zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                val sheetSb = StringBuilder()
                sheetSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <cols>
        <col min="1" max="1" width="6" customWidth="1"/>
        <col min="2" max="2" width="28" customWidth="1"/>
        <col min="3" max="3" width="24" customWidth="1"/>
        <col min="4" max="4" width="18" customWidth="1"/>
        <col min="5" max="5" width="22" customWidth="1"/>
        <col min="6" max="6" width="14" customWidth="1"/>
        <col min="7" max="7" width="30" customWidth="1"/>
    </cols>
    <sheetData>
        <row r="1" ht="24" customHeight="1">
            <c r="A1" s="1" t="inlineStr"><is><t>#</t></is></c>
            <c r="B1" s="1" t="inlineStr"><is><t>Serial Number / Barcode</t></is></c>
            <c r="C1" s="1" t="inlineStr"><is><t>Item Label</t></is></c>
            <c r="D1" s="1" t="inlineStr"><is><t>Category</t></is></c>
            <c r="E1" s="1" t="inlineStr"><is><t>Scanned Timestamp</t></is></c>
            <c r="F1" s="1" t="inlineStr"><is><t>Status</t></is></c>
            <c r="G1" s="1" t="inlineStr"><is><t>Notes</t></is></c>
        </row>""")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                batch.items.forEachIndexed { index, item ->
                    val rowNum = index + 2
                    val dateStr = dateFormat.format(Date(item.scannedAt))
                    sheetSb.append("""
        <row r="$rowNum">
            <c r="A$rowNum" t="n"><v>${index + 1}</v></c>
            <c r="B$rowNum" t="inlineStr"><is><t>${escapeXml(item.serialNumber)}</t></is></c>
            <c r="C$rowNum" t="inlineStr"><is><t>${escapeXml(item.label)}</t></is></c>
            <c r="D$rowNum" t="inlineStr"><is><t>${escapeXml(item.category)}</t></is></c>
            <c r="E$rowNum" t="inlineStr"><is><t>$dateStr</t></is></c>
            <c r="F$rowNum" t="inlineStr"><is><t>${escapeXml(item.status)}</t></is></c>
            <c r="G$rowNum" t="inlineStr"><is><t>${escapeXml(item.notes)}</t></is></c>
        </row>""")
                }

                sheetSb.append("""
    </sheetData>
</worksheet>""")

                zos.write(sheetSb.toString().toByteArray())
                zos.closeEntry()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Generates a standard CSV (Comma-Separated Values) spreadsheet file.
     */
    fun exportToCsv(batch: SerialBatch, outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val sb = StringBuilder()
            sb.appendLine("No,Serial Number,Item Label,Category,Scanned Timestamp,Status,Notes")

            batch.items.forEachIndexed { index, item ->
                val dateStr = dateFormat.format(Date(item.scannedAt))
                val row = listOf(
                    (index + 1).toString(),
                    escapeCsv(item.serialNumber),
                    escapeCsv(item.label),
                    escapeCsv(item.category),
                    dateStr,
                    escapeCsv(item.status),
                    escapeCsv(item.notes)
                ).joinToString(",")
                sb.appendLine(row)
            }

            outputFile.writeText(sb.toString())
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

    private fun escapeCsv(input: String): String {
        return if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            "\"" + input.replace("\"", "\"\"") + "\""
        } else {
            input
        }
    }
}
