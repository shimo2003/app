package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.SubscriptionManager
import com.example.data.DocumentRepository
import com.example.data.SerialRepository
import com.example.model.ExportFormat
import com.example.model.ScanFilter
import com.example.model.ScannedDocument
import com.example.model.ScannedPage
import com.example.model.SerialBatch
import com.example.model.SerialItem
import com.example.model.TrialState
import com.example.util.DocumentProcessor
import com.example.util.ExcelGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    DASHBOARD,
    CAMERA_SCANNER,
    DOCUMENT_VIEWER,
    EXPORT_CONVERT,
    SUBSCRIPTION,
    SERIAL_SCANNER,
    SERIAL_EXCEL_VIEWER
}

sealed class ExportState {
    object Idle : ExportState()
    object Converting : ExportState()
    data class Success(val file: File, val format: ExportFormat) : ExportState()
    data class Error(val message: String) : ExportState()
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application)
    private val serialRepository = SerialRepository(application)
    val subscriptionManager = SubscriptionManager(application)

    private val _screen = MutableStateFlow(AppScreen.DASHBOARD)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    val documents: StateFlow<List<ScannedDocument>> = repository.documents
    val serialBatches: StateFlow<List<SerialBatch>> = serialRepository.batches

    private val _activeDocument = MutableStateFlow<ScannedDocument?>(null)
    val activeDocument: StateFlow<ScannedDocument?> = _activeDocument.asStateFlow()

    private val _activeSerialBatch = MutableStateFlow<SerialBatch?>(null)
    val activeSerialBatch: StateFlow<SerialBatch?> = _activeSerialBatch.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _sessionPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val sessionPages: StateFlow<List<ScannedPage>> = _sessionPages.asStateFlow()

    private val _trialState = MutableStateFlow(subscriptionManager.getTrialState())
    val trialState: StateFlow<TrialState> = _trialState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _excelExportFile = MutableStateFlow<File?>(null)
    val excelExportFile: StateFlow<File?> = _excelExportFile.asStateFlow()

    private val _isExcelExporting = MutableStateFlow(false)
    val isExcelExporting: StateFlow<Boolean> = _isExcelExporting.asStateFlow()

    fun refreshTrialState() {
        _trialState.value = subscriptionManager.getTrialState()
    }

    fun navigateTo(screen: AppScreen) {
        _screen.value = screen
        refreshTrialState()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tag: String) {
        _selectedTag.value = tag
    }

    fun openDocument(doc: ScannedDocument) {
        _activeDocument.value = doc
        _currentPageIndex.value = 0
        _screen.value = AppScreen.DOCUMENT_VIEWER
    }

    fun startNewScan() {
        _sessionPages.value = emptyList()
        _screen.value = AppScreen.CAMERA_SCANNER
    }

    fun addPageToSession(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val page = repository.saveBitmapAsPage(bitmap)
            _sessionPages.value = _sessionPages.value + page
        }
    }

    fun finishSessionAndCreateDocument(titleInput: String? = null, tag: String = "General") {
        viewModelScope.launch(Dispatchers.IO) {
            val pages = _sessionPages.value
            if (pages.isEmpty()) return@launch

            val title = if (!titleInput.isNullOrBlank()) {
                titleInput
            } else {
                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
                "Scanned Doc - $dateStr"
            }

            val doc = repository.createDocument(title, pages, tag)
            _sessionPages.value = emptyList()
            _activeDocument.value = doc
            _currentPageIndex.value = 0
            withContext(Dispatchers.Main) {
                _screen.value = AppScreen.DOCUMENT_VIEWER
            }
        }
    }

    fun cancelScanSession() {
        _sessionPages.value.forEach { page ->
            try {
                File(page.filePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _sessionPages.value = emptyList()
        _screen.value = AppScreen.DASHBOARD
    }

    fun selectPage(index: Int) {
        val count = _activeDocument.value?.pageCount ?: 0
        if (index in 0 until count) {
            _currentPageIndex.value = index
        }
    }

    fun applyFilterToCurrentPage(filter: ScanFilter) {
        val doc = _activeDocument.value ?: return
        val page = doc.pages.getOrNull(_currentPageIndex.value) ?: return

        repository.updatePage(doc.id, page.id, page.rotationDegrees, filter)
        val updatedPages = doc.pages.map {
            if (it.id == page.id) it.copy(filter = filter) else it
        }
        _activeDocument.value = doc.copy(pages = updatedPages)
    }

    fun rotateCurrentPage() {
        val doc = _activeDocument.value ?: return
        val page = doc.pages.getOrNull(_currentPageIndex.value) ?: return

        val newRotation = (page.rotationDegrees + 90) % 360
        repository.updatePage(doc.id, page.id, newRotation, page.filter)
        val updatedPages = doc.pages.map {
            if (it.id == page.id) it.copy(rotationDegrees = newRotation) else it
        }
        _activeDocument.value = doc.copy(pages = updatedPages)
    }

    fun deleteCurrentPage() {
        val doc = _activeDocument.value ?: return
        val page = doc.pages.getOrNull(_currentPageIndex.value) ?: return

        repository.removePage(doc.id, page.id)
        val remainingPages = doc.pages.filter { it.id != page.id }
        if (remainingPages.isEmpty()) {
            _activeDocument.value = null
            _screen.value = AppScreen.DASHBOARD
        } else {
            _activeDocument.value = doc.copy(pages = remainingPages)
            _currentPageIndex.value = (_currentPageIndex.value - 1).coerceAtLeast(0)
        }
    }

    fun updateDocumentDetails(title: String, tag: String, note: String) {
        val doc = _activeDocument.value ?: return
        val updated = doc.copy(title = title, tag = tag, note = note)
        repository.updateDocument(updated)
        _activeDocument.value = updated
    }

    fun deleteDocument(docId: String) {
        repository.deleteDocument(docId)
        if (_activeDocument.value?.id == docId) {
            _activeDocument.value = null
            _screen.value = AppScreen.DASHBOARD
        }
    }

    fun startExport() {
        _exportState.value = ExportState.Idle
        _screen.value = AppScreen.EXPORT_CONVERT
    }

    fun convertCurrentDocument(format: ExportFormat, pageSize: String = "A4") {
        val doc = _activeDocument.value ?: return
        _exportState.value = ExportState.Converting

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = DocumentProcessor.convertDocument(
                    getApplication(),
                    doc,
                    format,
                    pageSize
                )
                withContext(Dispatchers.Main) {
                    if (file != null && file.exists()) {
                        _exportState.value = ExportState.Success(file, format)
                    } else {
                        _exportState.value = ExportState.Error("Failed to convert document to ${format.extension.uppercase()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _exportState.value = ExportState.Error(e.localizedMessage ?: "Unknown conversion error")
                }
            }
        }
    }

    // ==========================================
    // Serial Scanner & Excel Spreadsheet Methods
    // ==========================================

    fun startNewSerialScan() {
        val defaultBatch = _activeSerialBatch.value ?: serialBatches.value.firstOrNull() ?: run {
            val newB = serialRepository.createBatch("Serial Inventory Scan - ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())}")
            newB
        }
        _activeSerialBatch.value = defaultBatch
        _screen.value = AppScreen.SERIAL_SCANNER
    }

    fun openSerialBatch(batch: SerialBatch) {
        _activeSerialBatch.value = batch
        _screen.value = AppScreen.SERIAL_EXCEL_VIEWER
    }

    fun createNewSerialBatch(title: String) {
        val newBatch = serialRepository.createBatch(title)
        _activeSerialBatch.value = newBatch
        _screen.value = AppScreen.SERIAL_EXCEL_VIEWER
    }

    fun addScannedSerial(serialNumber: String, label: String = "Asset", category: String = "Hardware", notes: String = ""): Boolean {
        val currentBatch = _activeSerialBatch.value ?: return false
        val trimmed = serialNumber.trim()
        if (trimmed.isEmpty()) return false

        // Check for duplicate in this batch
        val exists = currentBatch.items.any { it.serialNumber.equals(trimmed, ignoreCase = true) }

        val newItem = SerialItem(
            serialNumber = trimmed,
            label = label,
            category = category,
            notes = notes,
            status = if (exists) "Duplicate" else "Verified"
        )

        serialRepository.addSerialItem(currentBatch.id, newItem)
        val updatedList = currentBatch.items + newItem
        _activeSerialBatch.value = currentBatch.copy(items = updatedList)
        return true
    }

    fun removeSerialItem(itemId: String) {
        val currentBatch = _activeSerialBatch.value ?: return
        serialRepository.removeSerialItem(currentBatch.id, itemId)
        val updatedList = currentBatch.items.filter { it.id != itemId }
        _activeSerialBatch.value = currentBatch.copy(items = updatedList)
    }

    fun deleteSerialBatch(batchId: String) {
        serialRepository.deleteBatch(batchId)
        if (_activeSerialBatch.value?.id == batchId) {
            _activeSerialBatch.value = null
            _screen.value = AppScreen.DASHBOARD
        }
    }

    fun exportCurrentBatchToExcel(asXlsx: Boolean = true) {
        val batch = _activeSerialBatch.value ?: return
        _isExcelExporting.value = true
        _excelExportFile.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val safeTitle = batch.title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
            val exportDir = File(getApplication<Application>().cacheDir, "excel_exports").apply { mkdirs() }
            val ext = if (asXlsx) "xlsx" else "csv"
            val targetFile = File(exportDir, "${safeTitle}.$ext")

            val success = if (asXlsx) {
                ExcelGenerator.exportToXlsx(batch, targetFile)
            } else {
                ExcelGenerator.exportToCsv(batch, targetFile)
            }

            withContext(Dispatchers.Main) {
                _isExcelExporting.value = false
                if (success && targetFile.exists()) {
                    _excelExportFile.value = targetFile
                }
            }
        }
    }

    // ==========================================
    // Subscription & Pro Status Methods
    // ==========================================

    fun unlockProWithCode(code: String): Boolean {
        val success = subscriptionManager.unlockPro(code)
        refreshTrialState()
        return success
    }

    fun resetTrial() {
        subscriptionManager.resetTrial()
        refreshTrialState()
    }

    fun expireTrialForTesting() {
        subscriptionManager.expireTrialForTesting()
        refreshTrialState()
    }
}
