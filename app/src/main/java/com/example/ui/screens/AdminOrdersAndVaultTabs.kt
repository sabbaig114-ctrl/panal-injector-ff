package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.PickedApkInfo
import com.example.data.model.ApkOrder
import com.example.data.model.ApkProduct
import com.example.data.model.OrderStatus
import com.example.data.model.UiState
import com.example.data.model.formatBytes
import com.example.data.model.formatPkr
import com.example.ui.components.CopyableTerminalRow
import com.example.ui.components.CyberCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.NeonGlowContainer
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TerminalSurface
import com.example.ui.viewmodel.UploadStatus

@Composable
fun AdminOrdersTab(
    ordersState: UiState<List<ApkOrder>>,
    isProcessing: Boolean,
    onCopyFeedback: (String) -> Unit,
    onApprove: (ApkOrder, String) -> Unit,
    onReject: (ApkOrder, String) -> Unit
) {
    var pendingOnly by rememberSaveable { mutableStateOf(true) }

    val displayedOrders = remember(ordersState, pendingOnly) {
        val list = (ordersState as? UiState.Success)?.data.orEmpty()
        if (pendingOnly) list.filter { it.parsedStatus == OrderStatus.PENDING } else list
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = pendingOnly,
                    onClick = { pendingOnly = true },
                    label = { Text("PENDING QUEUE", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGlowContainer,
                        selectedLabelColor = NeonGreen,
                        containerColor = CardSurface
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
                FilterChip(
                    selected = !pendingOnly,
                    onClick = { pendingOnly = false },
                    label = { Text("ALL ORDERS", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGlowContainer,
                        selectedLabelColor = NeonGreen,
                        containerColor = CardSurface
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }

        when (ordersState) {
            UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                }
            }

            is UiState.Error -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = ordersState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonReject
                        )
                    }
                }
            }

            is UiState.Success -> {
                if (displayedOrders.isEmpty()) {
                    item {
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (pendingOnly) {
                                    "No pending payment verifications right now."
                                } else {
                                    "No customer orders recorded yet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(displayedOrders, key = { it.id }) { order ->
                        AdminOrderReviewCard(
                            order = order,
                            isProcessing = isProcessing,
                            onCopyFeedback = onCopyFeedback,
                            onApprove = { note -> onApprove(order, note) },
                            onReject = { note -> onReject(order, note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrderReviewCard(
    order: ApkOrder,
    isProcessing: Boolean,
    onCopyFeedback: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var note by rememberSaveable(order.id) { mutableStateOf(order.adminNote.orEmpty()) }

    CyberCard(
        highlighted = order.parsedStatus == OrderStatus.PENDING,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_order_card_${order.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(status = order.parsedStatus)
            Text(
                text = formatPkr(order.pricePkr),
                style = MaterialTheme.typography.titleMedium,
                color = NeonGreen
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${order.productName} (v${order.productVersion})",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Customer: ${order.customerName} • ${order.userEmail}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))
        CopyableTerminalRow(
            label = "${order.parsedPaymentMethod.label.uppercase()} TRANSACTION ID",
            value = order.transactionId,
            onCopied = onCopyFeedback
        )
        Spacer(modifier = Modifier.height(8.dp))
        CopyableTerminalRow(
            label = "WHATSAPP NUMBER",
            value = order.whatsappNumber,
            onCopied = onCopyFeedback
        )

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Verification Note (Optional)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = BorderEmerald,
                focusedContainerColor = TerminalSurface,
                unfocusedContainerColor = TerminalSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onReject(note) },
                enabled = !isProcessing,
                border = BorderStroke(1.dp, CrimsonReject),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CrimsonContainer,
                    contentColor = CrimsonReject
                ),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("admin_reject_btn_${order.id}")
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("REJECT", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = { onApprove(note) },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = ObsidianBlack
                ),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("admin_approve_btn_${order.id}")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("APPROVE", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun AdminApkVaultTab(
    productsState: UiState<List<ApkProduct>>,
    pickedApk: PickedApkInfo?,
    uploadStatus: UploadStatus,
    isProcessing: Boolean,
    onPickApkUri: (Uri) -> Unit,
    onUploadProduct: (
        name: String,
        version: String,
        category: String,
        description: String,
        pricePkrText: String,
        active: Boolean,
        onClearForm: () -> Unit
    ) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    val context = LocalContext.current
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onPickApkUri(uri)
        }
    }

    var name by rememberSaveable { mutableStateOf("") }
    var version by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Injector") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonGreen,
        unfocusedBorderColor = BorderEmerald,
        focusedContainerColor = TerminalSurface,
        unfocusedContainerColor = TerminalSurface,
        cursorColor = NeonGreen
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "UPLOAD REAL .APK FILE",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select a compiled .apk package from your device. It is uploaded directly to Firebase Storage and unlocked only for approved orders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        apkPickerLauncher.launch(
                            arrayOf(
                                "application/vnd.android.package-archive",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    enabled = uploadStatus is UploadStatus.Idle,
                    border = BorderStroke(1.dp, NeonGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("pick_apk_file_btn")
                ) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pickedApk == null) "SELECT .APK FILE" else "CHANGE .APK FILE",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (pickedApk != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = NeonGlowContainer,
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Android, contentDescription = null, tint = NeonGreen)
                            Column {
                                Text(
                                    text = pickedApk.fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NeonGreen
                                )
                                Text(
                                    text = "Verified APK • ${formatBytes(pickedApk.fileSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("APK Display Name") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_apk_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Version (e.g. 2.4)") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_apk_version_input")
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_apk_price_input")
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Injector, HUD Tool)") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Release Notes & Features") },
                    minLines = 3,
                    maxLines = 5,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_apk_desc_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Publish Immediately in Store",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = NeonGreen
                        )
                    )
                }

                if (uploadStatus is UploadStatus.Uploading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { uploadStatus.progress },
                        color = NeonGreen,
                        trackColor = BorderEmerald,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Uploading to Firebase Storage: ${(uploadStatus.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonGreen
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onUploadProduct(
                            name,
                            version,
                            category,
                            description,
                            priceText,
                            active
                        ) {
                            name = ""
                            version = ""
                            priceText = ""
                            description = ""
                        }
                    },
                    enabled = uploadStatus is UploadStatus.Idle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("upload_publish_apk_btn")
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UPLOAD & PUBLISH .APK", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            Text(
                text = "PUBLISHED & DRAFT PACKAGES",
                style = MaterialTheme.typography.labelLarge,
                color = NeonGreen
            )
        }

        when (productsState) {
            UiState.Loading -> {
                item { CircularProgressIndicator(color = NeonGreen) }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = productsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonReject
                    )
                }
            }
            is UiState.Success -> {
                if (productsState.data.isEmpty()) {
                    item {
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No APK files uploaded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(productsState.data, key = { it.id }) { prod ->
                        CyberCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${prod.name} (v${prod.version})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${prod.fileName} • ${formatBytes(prod.fileSizeBytes)} • ${formatPkr(prod.pricePkr)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonGreen
                                    )
                                }
                                Switch(
                                    checked = prod.active,
                                    onCheckedChange = { onToggleActive(prod.id, it) },
                                    enabled = !isProcessing,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ObsidianBlack,
                                        checkedTrackColor = NeonGreen
                                    )
                                )
                                IconButton(
                                    onClick = { onDeleteProduct(prod.id) },
                                    enabled = !isProcessing,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete ${prod.name}",
                                        tint = CrimsonReject
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
