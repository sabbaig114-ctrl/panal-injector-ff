package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PickedApkInfo
import com.example.data.model.AdminUser
import com.example.data.model.ApkOrder
import com.example.data.model.ApkProduct
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentSettings
import com.example.data.model.UiState
import com.example.ui.components.CyberCard
import com.example.ui.components.SectionTerminalHeader
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TerminalSurface
import com.example.ui.viewmodel.UploadStatus

@Composable
fun AdminScreen(
    ordersState: UiState<List<ApkOrder>>,
    productsState: UiState<List<ApkProduct>>,
    adminsState: UiState<List<AdminUser>>,
    paymentSettings: PaymentSettings?,
    pickedApk: PickedApkInfo?,
    uploadStatus: UploadStatus,
    isProcessing: Boolean,
    initialSubTab: Int,
    onCopyFeedback: (String) -> Unit,
    onApproveOrder: (ApkOrder, String) -> Unit,
    onRejectOrder: (ApkOrder, String) -> Unit,
    onPickApkUri: (Uri) -> Unit,
    onUploadProduct: (String, String, String, String, String, Boolean, () -> Unit) -> Unit,
    onToggleProductActive: (String, Boolean) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSavePaymentSettings: (String, String, String, String, String) -> Unit,
    onGrantAdmin: (String, String, String) -> Unit,
    onRevokeAdmin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialSubTab.coerceIn(0, 2)) }
    LaunchedEffect(initialSubTab) {
        if (initialSubTab in 0..2) {
            selectedTab = initialSubTab
        }
    }

    val pendingCount = (ordersState as? UiState.Success)?.data
        ?.count { it.parsedStatus == OrderStatus.PENDING } ?: 0

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            SectionTerminalHeader(
                tag = "ADMIN CONSOLE",
                title = "Store Control Center"
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TerminalSurface,
            contentColor = NeonGreen,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonGreen
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = if (pendingCount > 0) "ORDERS ($pendingCount)" else "ORDERS",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier.testTag("admin_tab_orders")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("APK VAULT", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.testTag("admin_tab_vault")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("PAYMENTS", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.testTag("admin_tab_payments")
            )
        }

        when (selectedTab) {
            0 -> AdminOrdersTab(
                ordersState = ordersState,
                isProcessing = isProcessing,
                onCopyFeedback = onCopyFeedback,
                onApprove = onApproveOrder,
                onReject = onRejectOrder
            )
            1 -> AdminApkVaultTab(
                productsState = productsState,
                pickedApk = pickedApk,
                uploadStatus = uploadStatus,
                isProcessing = isProcessing,
                onPickApkUri = onPickApkUri,
                onUploadProduct = onUploadProduct,
                onToggleActive = onToggleProductActive,
                onDeleteProduct = onDeleteProduct
            )
            else -> AdminPaymentsAndRolesTab(
                paymentSettings = paymentSettings,
                adminsState = adminsState,
                isProcessing = isProcessing,
                onSavePaymentSettings = onSavePaymentSettings,
                onGrantAdmin = onGrantAdmin,
                onRevokeAdmin = onRevokeAdmin
            )
        }
    }
}

@Composable
private fun AdminPaymentsAndRolesTab(
    paymentSettings: PaymentSettings?,
    adminsState: UiState<List<AdminUser>>,
    isProcessing: Boolean,
    onSavePaymentSettings: (String, String, String, String, String) -> Unit,
    onGrantAdmin: (String, String, String) -> Unit,
    onRevokeAdmin: (String) -> Unit
) {
    var epTitle by rememberSaveable(paymentSettings?.easypaisaTitle) {
        mutableStateOf(paymentSettings?.easypaisaTitle.orEmpty())
    }
    var epNumber by rememberSaveable(paymentSettings?.easypaisaNumber) {
        mutableStateOf(paymentSettings?.easypaisaNumber.orEmpty())
    }
    var jcTitle by rememberSaveable(paymentSettings?.jazzcashTitle) {
        mutableStateOf(paymentSettings?.jazzcashTitle.orEmpty())
    }
    var jcNumber by rememberSaveable(paymentSettings?.jazzcashNumber) {
        mutableStateOf(paymentSettings?.jazzcashNumber.orEmpty())
    }
    var instructions by rememberSaveable(paymentSettings?.instructions) {
        mutableStateOf(
            paymentSettings?.instructions
                ?: "Send exact PKR total and submit your Transaction ID (TID) for fast manual verification."
        )
    }

    var newAdminUid by rememberSaveable { mutableStateOf("") }
    var newAdminEmail by rememberSaveable { mutableStateOf("") }
    var newAdminName by rememberSaveable { mutableStateOf("") }

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
                    text = "EASYPAISA & JAZZCASH ACCOUNTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = epTitle,
                    onValueChange = { epTitle = it },
                    label = { Text("Easypaisa Account Title") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_ep_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = epNumber,
                    onValueChange = { epNumber = it },
                    label = { Text("Easypaisa Mobile Number") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_ep_number_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = jcTitle,
                    onValueChange = { jcTitle = it },
                    label = { Text("JazzCash Account Title") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_jc_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jcNumber,
                    onValueChange = { jcNumber = it },
                    label = { Text("JazzCash Mobile Number") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_jc_number_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Checkout Payment Instructions") },
                    minLines = 2,
                    maxLines = 4,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onSavePaymentSettings(epTitle, epNumber, jcTitle, jcNumber, instructions)
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("save_payment_settings_btn")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE PAYMENT DETAILS", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = NeonGreen)
                    Text(
                        text = "GRANT ADMIN ROLE",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonGreen
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newAdminUid,
                    onValueChange = { newAdminUid = it },
                    label = { Text("User Firebase UID") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newAdminEmail,
                    onValueChange = { newAdminEmail = it },
                    label = { Text("Google Email") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newAdminName,
                    onValueChange = { newAdminName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onGrantAdmin(newAdminUid, newAdminEmail, newAdminName)
                        newAdminUid = ""
                        newAdminEmail = ""
                        newAdminName = ""
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Text("GRANT ADMIN ACCESS", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (adminsState is UiState.Success && adminsState.data.isNotEmpty()) {
            items(adminsState.data, key = { it.uid }) { admin ->
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = admin.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${admin.email} • ${admin.uid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onRevokeAdmin(admin.uid) },
                            enabled = !isProcessing,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Revoke ${admin.email}",
                                tint = CrimsonReject
                            )
                        }
                    }
                }
            }
        }
    }
}
