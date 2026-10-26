package com.aistudio.docscanpro.zqwk.model

enum class ScanFilter(val label: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    GRAYSCALE("Grayscale"),
    BW_DOCUMENT("B&W Doc"),
    INVERTED("Inverted")
}

data class ScannedPage(
    val id: String,
    val filePath: String,
    val rotationDegrees: Int = 0,
    val filter: ScanFilter = ScanFilter.MAGIC_COLOR,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScannedDocument(
    val id: String,
    val title: String,
    val pages: List<ScannedPage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tag: String = "General",
    val note: String = ""
) {
    val pageCount: Int get() = pages.size
    val firstPageThumbnail: String? get() = pages.firstOrNull()?.filePath
}

enum class ExportFormat(
    val extension: String,
    val label: String,
    val mimeType: String,
    val description: String,
    val iconName: String
) {
    PDF("pdf", "PDF Document", "application/pdf", "Industry standard multi-page vector PDF", "picture_as_pdf"),
    PNG("png", "PNG Image (Lossless)", "image/png", "Lossless crisp PNG with maximum clarity", "image"),
    JPG("jpg", "JPEG Image", "image/jpeg", "Compact photo & document format", "photo"),
    WEBP("webp", "WebP Image", "image/webp", "Ultra-compact modern web image format", "web"),
    TXT("txt", "Text Summary", "text/plain", "Plain text summary and document metadata", "description"),
    DOCX("docx", "Word Document", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Microsoft Word OpenXML document with embedded pages", "article"),
    HTML("html", "Web HTML5 Page", "text/html", "Interactive standalone web document viewable in any browser", "language"),
    MD("md", "Markdown Note", "text/markdown", "Markdown document with embedded page metadata", "code"),
    ZIP("zip", "ZIP Archive", "application/zip", "Compressed archive of all high-res scanned pages", "folder_zip")
}

data class TrialState(
    val daysRemaining: Int,
    val hoursRemaining: Int,
    val isTrialActive: Boolean,
    val isProUnlocked: Boolean,
    val trialStartEpochMs: Long,
    val trialEndEpochMs: Long
)

enum class PlanDuration(val id: String, val title: String, val price: String, val period: String, val badge: String?) {
    MONTHLY("monthly", "Monthly Pro", "$4.99", "/ month", null),
    ANNUAL("annual", "Annual VIP", "$29.99", "/ year", "Save 50%"),
    LIFETIME("lifetime", "Lifetime Unlimited", "$49.99", "one-time", "Best Value")
}
