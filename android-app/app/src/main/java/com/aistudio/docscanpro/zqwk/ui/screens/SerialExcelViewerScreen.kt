package com.aistudio.docscanpro.zqwk.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.docscanpro.zqwk.model.SerialBatch
import com.aistudio.docscanpro.zqwk.model.SerialItem
import com.aistudio.docscanpro.zqwk.ui.theme.AccentCyan
import com.aistudio.docscanpro.zqwk.ui.theme.AccentEmerald
import com.aistudio.docscanpro.zqwk.ui.theme.AccentTeal
import com.aistudio.docscanpro.zqwk.ui.theme.PrimaryBlue
import com.aistudio.docscanpro.zqwk.ui.theme.Slate800
import com.aistudio.docscanpro.zqwk.ui.theme.Slate900
import com.aistudio.docscanpro.zqwk.util.DocumentProcessor
import com.aistudio.docscanpro.zqwk.viewmodel.AppScreen
import com.aistudio.docscanpro.zqwk.viewmodel.ScannerViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerialExcelViewerScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeBatch by viewModel.activeSerialBatch.collectAsState()
    val isExporting by viewModel.isExcelExporting.collectAsState()
    val exportFile by viewModel.excelExportFile.collectAsState()

    BackHandler {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    if (activeBatch == null) {
        viewModel.navigateTo(AppScreen.DASHBOARD)
        return
    }

    val batch = activeBatch!!
    var exportAsXlsx by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = batch.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "Excel Spreadsheet (${batch.itemCount} serials)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AccentEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        modifier = Modifier.testTag("excel_viewer_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.SERIAL_SCANNER) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("scan_more_serials_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan More", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Excel Export CTA Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF107C41)), // Official Excel Green
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableView,
                                    contentDescription = "Excel",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Microsoft Excel Conversion",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Formatted worksheet ready for Excel, Sheets & ERP",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Format toggle (XLSX vs CSV)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = exportAsXlsx,
                                onClick = { exportAsXlsx = true },
                                label = { Text("Excel Workbook (.xlsx)") }
                            )
                            FilterChip(
                                selected = !exportAsXlsx,
                                onClick = { exportAsXlsx = false },
                                label = { Text("CSV Spreadsheet (.csv)") }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.exportCurrentBatchToExcel(asXlsx = exportAsXlsx) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_excel_file_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isExporting
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating Excel Spreadsheet...")
                            } else {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (exportAsXlsx) "Export to Excel (.xlsx)" else "Export to CSV (.csv)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Export Success Card (if exported)
            if (exportFile != null) {
                item {
                    val file = exportFile!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("excel_success_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6EE7B7))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Spreadsheet Generated!",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46),
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${file.name} • ${file.length() / 1024} KB",
                                        color = Color(0xFF047857),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val mime = if (file.name.endsWith(".xlsx")) {
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        } else "text/csv"
                                        DocumentProcessor.shareFile(context, file, mime, file.name)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_excel_file_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Excel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val mime = if (file.name.endsWith(".xlsx")) {
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        } else "text/csv"
                                        DocumentProcessor.openFile(context, file, mime)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_excel_file_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open in Excel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // In-App Excel Sheet Preview Table
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spreadsheet Table Preview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${batch.itemCount} rows",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            // Horizontally scrollable Excel table grid
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        // Table Header Row (Classic Excel Blue Header)
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF0284C7))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(36.dp))
                            Text("Serial Number / Barcode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(190.dp))
                            Text("Item Label", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(160.dp))
                            Text("Category", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                            Text("Scanned At", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(140.dp))
                            Text("Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                            Text("Notes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(180.dp))
                            Text("Action", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                        }

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                        if (batch.items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No serials recorded. Tap 'Scan More' to start scanning.", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            batch.items.forEachIndexed { index, item ->
                                val rowBg = if (index % 2 == 0) Color.Transparent else Color(0xFFF8FAFC)
                                val dateStr = dateFormat.format(Date(item.scannedAt))

                                Row(
                                    modifier = Modifier
                                        .background(rowBg)
                                        .padding(vertical = 8.dp, horizontal = 12.dp)
                                        .border(0.5.dp, Color(0xFFE2E8F0)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(36.dp))
                                    Text(
                                        text = item.serialNumber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = PrimaryBlue,
                                        modifier = Modifier.width(190.dp)
                                    )
                                    Text(item.label, fontSize = 12.sp, modifier = Modifier.width(160.dp))
                                    Text(item.category, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                    Text(dateStr, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(140.dp))

                                    Box(modifier = Modifier.width(90.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (item.status == "Duplicate") Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = item.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.status == "Duplicate") Color(0xFFB45309) else Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = item.notes.ifBlank { "-" },
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.width(180.dp)
                                    )

                                    IconButton(
                                        onClick = { viewModel.removeSerialItem(item.id) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .width(60.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
