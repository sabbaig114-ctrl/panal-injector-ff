package com.example.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ApkOrder
import com.example.data.model.OrderStatus
import com.example.data.model.UiState
import com.example.data.model.formatBytes
import com.example.data.model.formatPkr
import com.example.ui.components.CyberCard
import com.example.ui.components.SectionTerminalHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPending
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.NeonGlowContainer
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TerminalSurface
import com.example.ui.viewmodel.DownloadStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MyOrdersScreen(
    ordersState: UiState<List<ApkOrder>>,
    downloadStates: Map<String, DownloadStatus>,
    currentUserId: String,
    onDownloadApk: (order: ApkOrder, destinationUri: Uri?) -> Unit,
    onBrowseStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by rememberSaveable { mutableStateOf<OrderStatus?>(null) }
    var pendingSafOrder by remember { mutableStateOf<ApkOrder?>(null) }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        val order = pendingSafOrder
        pendingSafOrder = null
        if (uri != null && order != null) {
            onDownloadApk(order, uri)
        }
    }

    val filteredOrders = remember(ordersState, selectedFilter) {
        val list = (ordersState as? UiState.Success)?.data.orEmpty()
        if (selectedFilter == null) list else list.filter { it.parsedStatus == selectedFilter }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionTerminalHeader(
                tag = "ORDER TRACKER",
                title = "My APK Orders"
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters: List<Pair<OrderStatus?, String>> = listOf(
                    null to "ALL",
                    OrderStatus.PENDING to "PENDING",
                    OrderStatus.APPROVED to "APPROVED",
                    OrderStatus.REJECTED to "REJECTED"
                )
                filters.forEach { (status, label) ->
                    val selected = selectedFilter == status
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = status },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGlowContainer,
                            selectedLabelColor = NeonGreen,
                            containerColor = CardSurface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = BorderEmerald,
                            selectedBorderColor = NeonGreen
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
        }

        when (ordersState) {
            UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                }
            }

            is UiState.Error -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = CrimsonReject)
                            Text(
                                text = ordersState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonReject
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                if (filteredOrders.isEmpty()) {
                    item {
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_empty_vault),
                                    contentDescription = "No Orders Found",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                                Text(
                                    text = "No Orders Here Yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select an APK from the Store, pay via Easypaisa or JazzCash, and submit your TID to track verification here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onBrowseStore,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreen,
                                        contentColor = ObsidianBlack
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("BROWSE APK STORE", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            currentUserId = currentUserId,
                            downloadStatus = downloadStates[order.id] ?: DownloadStatus.Idle,
                            onDirectDownload = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    onDownloadApk(order, null)
                                } else {
                                    pendingSafOrder = order
                                    saveDocumentLauncher.launch(
                                        order.approvedFileName ?: "${order.productName}.apk"
                                    )
                                }
                            },
                            onSaveAsDownload = {
                                pendingSafOrder = order
                                saveDocumentLauncher.launch(
                                    order.approvedFileName ?: "${order.productName}.apk"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: ApkOrder,
    currentUserId: String,
    downloadStatus: DownloadStatus,
    onDirectDownload: () -> Unit,
    onSaveAsDownload: () -> Unit
) {
    val status = order.parsedStatus
    val canDownload = status == OrderStatus.APPROVED &&
        order.userId == currentUserId &&
        !order.approvedStoragePath.isNullOrBlank()

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }
    val dateLabel = order.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Just now"

    CyberCard(
        highlighted = status == OrderStatus.APPROVED,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(status = status)
            Text(
                text = formatPkr(order.pricePkr),
                style = MaterialTheme.typography.titleMedium,
                color = NeonGreen
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = order.productName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Version ${order.productVersion} • Ordered $dateLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = MaterialTheme.shapes.small,
            color = TerminalSurface,
            border = BorderStroke(1.dp, BorderEmerald),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "PAYMENT: ${order.parsedPaymentMethod.label.uppercase()} • TID: ${order.transactionId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "BUYER: ${order.customerName} (${order.whatsappNumber})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!order.adminNote.isNullOrBlank()) {
                    Text(
                        text = "ADMIN NOTE: ${order.adminNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status == OrderStatus.REJECTED) CrimsonReject else NeonGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            canDownload -> {
                val apkName = order.approvedFileName ?: "${order.productName}.apk"
                val apkSize = order.approvedFileSizeBytes ?: 0L
                Text(
                    text = "FILE READY: $apkName (${formatBytes(apkSize)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (downloadStatus) {
                    DownloadStatus.Verifying -> {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = NeonGlowContainer,
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = NeonGreen,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Verifying order approval & Storage grant with server...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    is DownloadStatus.Downloading -> {
                        val fraction = if (downloadStatus.totalBytes > 0L) {
                            (downloadStatus.bytesTransferred.toFloat() / downloadStatus.totalBytes.toFloat())
                                .coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        LinearProgressIndicator(
                            progress = { fraction },
                            color = NeonGreen,
                            trackColor = BorderEmerald,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Downloading real APK: ${formatBytes(downloadStatus.bytesTransferred)} / ${formatBytes(downloadStatus.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen
                        )
                    }

                    is DownloadStatus.Completed -> {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = NeonGlowContainer,
                            border = BorderStroke(1.dp, NeonGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Saved: ${downloadStatus.result.displayLocation} (${formatBytes(downloadStatus.result.bytesWritten)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onDirectDownload,
                            border = BorderStroke(1.dp, NeonGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DOWNLOAD AGAIN", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    is DownloadStatus.Failed -> {
                        Text(
                            text = downloadStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonReject
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DownloadActionRow(
                            orderId = order.id,
                            onDirectDownload = onDirectDownload,
                            onSaveAsDownload = onSaveAsDownload
                        )
                    }

                    DownloadStatus.Idle -> {
                        DownloadActionRow(
                            orderId = order.id,
                            onDirectDownload = onDirectDownload,
                            onSaveAsDownload = onSaveAsDownload
                        )
                    }
                }
            }

            status == OrderStatus.PENDING -> {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = AmberContainer,
                    border = BorderStroke(1.dp, AmberPending.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HourglassTop,
                            contentDescription = null,
                            tint = AmberPending,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "APK Locked: Admin is verifying your payment TID. Download unlocks automatically upon approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberPending
                        )
                    }
                }
            }

            else -> {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = CrimsonContainer,
                    border = BorderStroke(1.dp, CrimsonReject.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = CrimsonReject,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Access Denied: Payment verification failed or order rejected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonReject
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadActionRow(
    orderId: String,
    onDirectDownload: () -> Unit,
    onSaveAsDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onDirectDownload,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen,
                contentColor = ObsidianBlack
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .minimumInteractiveComponentSize()
                .testTag("download_apk_btn_$orderId")
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("VERIFY & DOWNLOAD .APK", style = MaterialTheme.typography.labelLarge)
        }
        IconButton(
            onClick = onSaveAsDownload,
            modifier = Modifier
                .size(48.dp)
                .minimumInteractiveComponentSize()
                .testTag("save_as_apk_btn_$orderId")
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = "Choose download folder",
                tint = NeonGreen
            )
        }
    }
}
