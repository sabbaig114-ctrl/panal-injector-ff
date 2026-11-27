package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ApkOrder
import com.example.data.model.ApkProduct
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentSettings
import com.example.data.model.formatBytes
import com.example.data.model.formatPkr
import com.example.ui.components.CopyableTerminalRow
import com.example.ui.components.CyberCard
import com.example.ui.components.SectionTerminalHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPending
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.CardSurface
import com.example.ui.theme.EasypaisaGreen
import com.example.ui.theme.JazzCashRed
import com.example.ui.theme.NeonGlowContainer
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TerminalSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCheckoutScreen(
    product: ApkProduct,
    defaultCustomerName: String,
    paymentSettings: PaymentSettings?,
    existingOrder: ApkOrder?,
    isSubmitting: Boolean,
    onCopyFeedback: (String) -> Unit,
    onSubmitOrder: (
        customerName: String,
        whatsappNumber: String,
        paymentMethod: PaymentMethod,
        transactionId: String
    ) -> Unit,
    onGoToOrders: () -> Unit,
    onBack: () -> Unit
) {
    var customerName by rememberSaveable { mutableStateOf(defaultCustomerName) }
    var whatsappNumber by rememberSaveable { mutableStateOf("") }
    var selectedMethod by rememberSaveable { mutableStateOf(PaymentMethod.EASYPAISA) }
    var transactionId by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "APK CHECKOUT",
                        style = MaterialTheme.typography.headlineSmall,
                        color = NeonGreen
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("checkout_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Store",
                            tint = NeonGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Specification Card
                CyberCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_apk_emblem),
                            contentDescription = "${product.name} APK",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${product.category} • Version ${product.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonGreen
                            )
                            Text(
                                text = formatPkr(product.pricePkr),
                                style = MaterialTheme.typography.titleLarge,
                                color = NeonGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SpecBadge(
                            icon = Icons.Filled.Android,
                            label = product.fileName,
                            modifier = Modifier.weight(1f)
                        )
                        SpecBadge(
                            icon = Icons.Filled.Storage,
                            label = formatBytes(product.fileSizeBytes)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (existingOrder != null) {
                    CyberCard(
                        highlighted = existingOrder.parsedStatus == OrderStatus.APPROVED,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_existing_order_banner")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(status = existingOrder.parsedStatus)
                            Text(
                                text = "TID: ${existingOrder.transactionId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (existingOrder.parsedStatus) {
                                OrderStatus.APPROVED ->
                                    "Your payment is verified! Access to the real .apk download is unlocked in My Orders."
                                OrderStatus.PENDING ->
                                    "Your order is currently pending admin payment verification. The download button stays locked until approved."
                                OrderStatus.REJECTED ->
                                    "Your previous order was rejected. You can submit a corrected payment below."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (existingOrder.parsedStatus != OrderStatus.REJECTED) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onGoToOrders,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = ObsidianBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("checkout_go_to_orders_btn")
                            ) {
                                Text(
                                    text = if (existingOrder.parsedStatus == OrderStatus.APPROVED) {
                                        "GO TO MY ORDERS TO DOWNLOAD .APK"
                                    } else {
                                        "VIEW ORDER STATUS IN MY ORDERS"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // Step 1: Payment Channel Selection & Live Account Details
                SectionTerminalHeader(
                    tag = "STEP 1",
                    title = "Select Payment Method"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaymentMethodButton(
                        method = PaymentMethod.EASYPAISA,
                        selected = selectedMethod == PaymentMethod.EASYPAISA,
                        accent = EasypaisaGreen,
                        onClick = { selectedMethod = PaymentMethod.EASYPAISA },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_easypaisa_btn")
                    )
                    PaymentMethodButton(
                        method = PaymentMethod.JAZZCASH,
                        selected = selectedMethod == PaymentMethod.JAZZCASH,
                        accent = JazzCashRed,
                        onClick = { selectedMethod = PaymentMethod.JAZZCASH },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_jazzcash_btn")
                    )
                }

                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${selectedMethod.label.uppercase()} ACCOUNT DETAILS",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (paymentSettings == null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = AmberContainer,
                            border = BorderStroke(1.dp, AmberPending.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Payment account details have not been configured by the Admin yet. Please ask the Admin to save Easypaisa/JazzCash details in Admin -> Payments.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberPending,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        val title = if (selectedMethod == PaymentMethod.EASYPAISA) {
                            paymentSettings.easypaisaTitle
                        } else {
                            paymentSettings.jazzcashTitle
                        }
                        val number = if (selectedMethod == PaymentMethod.EASYPAISA) {
                            paymentSettings.easypaisaNumber
                        } else {
                            paymentSettings.jazzcashNumber
                        }

                        CopyableTerminalRow(
                            label = "ACCOUNT TITLE",
                            value = title,
                            onCopied = onCopyFeedback
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CopyableTerminalRow(
                            label = "${selectedMethod.label.uppercase()} NUMBER",
                            value = number,
                            onCopied = onCopyFeedback
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CopyableTerminalRow(
                            label = "EXACT AMOUNT TO SEND",
                            value = formatPkr(product.pricePkr),
                            onCopied = onCopyFeedback
                        )
                        if (paymentSettings.instructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = paymentSettings.instructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Step 2: Customer Details & Transaction ID
                SectionTerminalHeader(
                    tag = "STEP 2",
                    title = "Submit Verification"
                )

                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderEmerald,
                        focusedContainerColor = TerminalSurface,
                        unfocusedContainerColor = TerminalSurface,
                        cursorColor = NeonGreen
                    )

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Your Full Name") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = NeonGreen)
                        },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = { Text("WhatsApp Number (e.g. +923001234567)") },
                        leadingIcon = {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = NeonGreen)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_whatsapp_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = transactionId,
                        onValueChange = { transactionId = it },
                        label = { Text("${selectedMethod.label} Transaction ID (TID)") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = NeonGreen)
                        },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_tid_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Real .apk download unlocks in My Orders immediately after Admin verifies your TID.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onSubmitOrder(
                                customerName,
                                whatsappNumber,
                                selectedMethod,
                                transactionId
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = ObsidianBlack
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("submit_order_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = ObsidianBlack,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SUBMITTING ORDER...", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SUBMIT PAYMENT & PLACE ORDER", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SpecBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = TerminalSurface,
        border = BorderStroke(1.dp, BorderEmerald)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PaymentMethodButton(
    method: PaymentMethod,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) NeonGlowContainer else CardSurface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) NeonGreen else BorderEmerald
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = accent,
                modifier = Modifier.size(10.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = method.label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) NeonGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
