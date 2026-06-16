@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.abhijit.docscanpro.ui.screens.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.abhijit.docscanpro.ui.navigation.Screen
import com.abhijit.docscanpro.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentViewerScreen(
    documentId: Long,
    navController: NavHostController,
    viewModel: DocumentViewerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(documentId) { viewModel.loadDocument(documentId) }

    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var renameInput by remember { mutableStateOf("") }
    var showActions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.document?.name ?: "Document",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (uiState.pages.isNotEmpty()) {
                            Text(
                                "Page ${pagerState.currentPage + 1} of ${uiState.pages.size}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (uiState.document?.isFavorite == true) Icons.Default.Star else Icons.Default.StarBorder,
                            "Favorite",
                            tint = if (uiState.document?.isFavorite == true) Color(0xFFFFB300) else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showActions = true }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                        DropdownMenuItem(
                            text = { Text("Share PDF") },
                            onClick = { viewModel.sharePdf(); showActions = false },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Page") },
                            onClick = { viewModel.shareCurrentPageAsImage(); showActions = false },
                            leadingIcon = { Icon(Icons.Default.Image, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Document Info") },
                            onClick = { showInfoSheet = true; showActions = false },
                            leadingIcon = { Icon(Icons.Default.Info, null) }
                        )
                        if (uiState.document?.documentType == "BUSINESS_CARD") {
                            DropdownMenuItem(
                                text = { Text("Export as Contact (.vcf)") },
                                onClick = { viewModel.exportVCard(); showActions = false },
                                leadingIcon = { Icon(Icons.Default.ContactPage, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Export as QR Code") },
                            onClick = { viewModel.generateQrForCurrentPage(); showActions = false },
                            leadingIcon = { Icon(Icons.Default.QrCode2, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("PDF Tools") },
                            onClick = {
                                showActions = false
                                uiState.document?.let { doc ->
                                    navController.navigate(Screen.PdfTools.createRoute(doc.id))
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Build, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Password Protect") },
                            onClick = { showPasswordDialog = true; showActions = false },
                            leadingIcon = { Icon(Icons.Default.Lock, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { showRenameDialog = true; showActions = false },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showDeleteDialog = true; showActions = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = viewModel::toggleOcrPanel,
                    icon = { Icon(Icons.Default.TextFields, null) },
                    label = { Text("OCR Text") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = viewModel::sharePdf,
                    icon = { Icon(Icons.Default.Share, null) },
                    label = { Text("Share") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = viewModel::copyOcrText,
                    icon = { Icon(Icons.Default.ContentCopy, null) },
                    label = { Text("Copy Text") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No pages in this document")
                }
                else -> {
                    // Page image pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val page = uiState.pages.getOrNull(pageIndex)
                        Box(Modifier.fillMaxSize().background(Color.Black)) {
                            if (page != null) {
                                AsyncImage(
                                    model = page.imagePath,
                                    contentDescription = "Page ${pageIndex + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Page indicator dots
                    if (uiState.pages.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(uiState.pages.size) { index ->
                                val isCurrentPage = index == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (isCurrentPage) 10.dp else 6.dp)
                                        .background(
                                            if (isCurrentPage) Color.White else Color.White.copy(alpha = 0.5f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // OCR panel slides up from bottom
            AnimatedVisibility(
                visible = uiState.showOcrPanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("OCR Text", fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = viewModel::copyOcrText) {
                                    Icon(Icons.Default.ContentCopy, "Copy")
                                }
                                IconButton(onClick = viewModel::toggleOcrPanel) {
                                    Icon(Icons.Default.Close, "Close")
                                }
                            }
                        }
                        val ocrText = viewModel.getCurrentOcrText()
                        if (ocrText.isEmpty()) {
                            Text("No text recognized on this page", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(
                                ocrText,
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Processing overlay
            if (uiState.isProcessing) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        LaunchedEffect(Unit) { renameInput = uiState.document?.name ?: "" }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Document name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameDocument(renameInput.ifBlank { "Document" })
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Move to Recycle Bin?") },
            text = { Text("This document will be moved to the recycle bin.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteDocument { navController.popBackStack() }
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Password dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Password Protect PDF") },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.protectWithPassword(passwordInput)
                    showPasswordDialog = false
                    passwordInput = ""
                }) { Text("Protect") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false; passwordInput = "" }) { Text("Cancel") }
            }
        )
    }

    // Document info bottom sheet
    if (showInfoSheet) {
        ModalBottomSheet(onDismissRequest = { showInfoSheet = false }) {
            val doc = uiState.document
            if (doc != null) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Text(doc.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    doc.documentType?.let { type ->
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(type.replace("_", " ")) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    InfoRow(Icons.Default.Collections, "Pages", "${uiState.pages.size}")
                    InfoRow(Icons.Default.Storage, "Total size", FileUtils.formatFileSize(doc.totalSizeBytes))
                    InfoRow(Icons.Default.CalendarToday, "Created", infoDate(doc.createdAt))
                    InfoRow(Icons.Default.Update, "Modified", infoDate(doc.updatedAt))
                    val wordCount = uiState.pages.sumOf { page ->
                        page.ocrText?.split("\\s+".toRegex())?.filter { it.isNotEmpty() }?.size ?: 0
                    }
                    if (wordCount > 0) {
                        InfoRow(Icons.Default.TextFields, "OCR words", "$wordCount")
                    }
                    if (doc.isFavorite) {
                        InfoRow(Icons.Default.Star, "Starred", "Yes")
                    }
                }
            }
        }
    }

    // QR code export dialog
    val qrBitmap = uiState.qrBitmap
    if (qrBitmap != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearQr,
            title = { Text("QR Code Export") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(240.dp)
                            .padding(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan to read the extracted text from this page",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.shareQrCode()
                    viewModel.clearQr()
                }) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearQr) { Text("Close") }
            }
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

private fun infoDate(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH).format(Date(timestamp))
