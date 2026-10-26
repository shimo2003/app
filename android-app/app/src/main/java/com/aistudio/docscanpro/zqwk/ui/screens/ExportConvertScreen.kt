package com.aistudio.docscanpro.zqwk.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Web
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.docscanpro.zqwk.model.ExportFormat
import com.aistudio.docscanpro.zqwk.ui.theme.AccentCyan
import com.aistudio.docscanpro.zqwk.ui.theme.AccentEmerald
import com.aistudio.docscanpro.zqwk.ui.theme.AccentTeal
import com.aistudio.docscanpro.zqwk.ui.theme.PrimaryBlue
import com.aistudio.docscanpro.zqwk.ui.theme.Slate800
import com.aistudio.docscanpro.zqwk.ui.theme.Slate900
import com.aistudio.docscanpro.zqwk.util.DocumentProcessor
import com.aistudio.docscanpro.zqwk.viewmodel.AppScreen
import com.aistudio.docscanpro.zqwk.viewmodel.ExportState
import com.aistudio.docscanpro.zqwk.viewmodel.ScannerViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportConvertScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDoc by viewModel.activeDocument.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    BackHandler {
        viewModel.navigateTo(AppScreen.DOCUMENT_VIEWER)
    }

    if (activeDoc == null) {
        viewModel.navigateTo(AppScreen.DASHBOARD)
        return
    }

    val doc = activeDoc!!

    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedPageSize by remember { mutableStateOf("A4") } // "A4", "Letter", "Fit"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Export & Convert",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${doc.title} (${doc.pageCount} pages)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DOCUMENT_VIEWER) },
                        modifier = Modifier.testTag("export_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Target Extension Selection
            item {
                Text(
                    text = "Select Output Extension",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Convert your scanned pages to PDF or any extension of choice",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Grid / list of format cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExportFormat.values().forEach { format ->
                        val isSelected = selectedFormat == format
                        val icon = getFormatIcon(format)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFormat = format }
                                .testTag("format_card_${format.extension}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    PrimaryBlue.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(getFormatBadgeColor(format).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = format.extension,
                                        tint = getFormatBadgeColor(format),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ".${format.extension.uppercase()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE2E8F0))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = format.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Slate800
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = format.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PDF Options (shown only if PDF chosen)
            if (selectedFormat == ExportFormat.PDF) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PDF Page Layout",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("A4", "Letter", "Fit").forEach { size ->
                                    FilterChip(
                                        selected = selectedPageSize == size,
                                        onClick = { selectedPageSize = size },
                                        label = {
                                            Text(
                                                when (size) {
                                                    "A4" -> "A4 Standard (210×297mm)"
                                                    "Letter" -> "US Letter (8.5×11\")"
                                                    else -> "Auto Fit Page"
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Convert Action Button
            item {
                Button(
                    onClick = {
                        viewModel.convertCurrentDocument(selectedFormat, selectedPageSize)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("execute_convert_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp),
                    enabled = exportState !is ExportState.Converting
                ) {
                    if (exportState is ExportState.Converting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Converting to .${selectedFormat.extension.uppercase()}...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Convert to .${selectedFormat.extension.uppercase()} Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Conversion Result Card
            item {
                when (val state = exportState) {
                    is ExportState.Success -> {
                        ConversionSuccessCard(
                            file = state.file,
                            format = state.format,
                            onShare = {
                                DocumentProcessor.shareFile(
                                    context,
                                    state.file,
                                    state.format.mimeType,
                                    doc.title
                                )
                            },
                            onOpen = {
                                DocumentProcessor.openFile(
                                    context,
                                    state.file,
                                    state.format.mimeType
                                )
                            }
                        )
                    }
                    is ExportState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Error: ${state.message}",
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                    else -> {}
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ConversionSuccessCard(
    file: File,
    format: ExportFormat,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversion_success_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFECFDF5)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6EE7B7))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Converted Successfully!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${file.name} • ${formatFileSize(file.length())}",
                        color = Color(0xFF047857),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_converted_file_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_converted_file_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getFormatIcon(format: ExportFormat): ImageVector {
    return when (format) {
        ExportFormat.PDF -> Icons.Default.PictureAsPdf
        ExportFormat.PNG -> Icons.Default.Image
        ExportFormat.JPG -> Icons.Default.Photo
        ExportFormat.WEBP -> Icons.Default.Web
        ExportFormat.TXT -> Icons.Default.Description
        ExportFormat.DOCX -> Icons.Default.Article
        ExportFormat.HTML -> Icons.Default.Language
        ExportFormat.MD -> Icons.Default.Code
        ExportFormat.ZIP -> Icons.Default.FolderZip
    }
}

private fun getFormatBadgeColor(format: ExportFormat): Color {
    return when (format) {
        ExportFormat.PDF -> Color(0xFFDC2626) // Red
        ExportFormat.PNG -> Color(0xFF0284C7) // Blue
        ExportFormat.JPG -> Color(0xFFF59E0B) // Amber
        ExportFormat.WEBP -> Color(0xFF06B6D4) // Cyan
        ExportFormat.TXT -> Color(0xFF64748B) // Slate
        ExportFormat.DOCX -> Color(0xFF2563EB) // Word Blue
        ExportFormat.HTML -> Color(0xFFEA580C) // Orange
        ExportFormat.MD -> Color(0xFF8B5CF6) // Purple
        ExportFormat.ZIP -> Color(0xFF0D9488) // Teal
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }
}
