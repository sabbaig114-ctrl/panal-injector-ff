package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ApkProduct
import com.example.data.model.UiState
import com.example.data.model.formatBytes
import com.example.data.model.formatPkr
import com.example.ui.components.CyberCard
import com.example.ui.components.SectionTerminalHeader
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.NeonGlowContainer
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack

@Composable
fun StorefrontScreen(
    productsState: UiState<List<ApkProduct>>,
    isAdmin: Boolean,
    onSelectProduct: (ApkProduct) -> Unit,
    onGoToAdminUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val categories = remember(productsState) {
        val dynamic = (productsState as? UiState.Success)?.data
            ?.map { it.category.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
        listOf("All") + dynamic
    }

    val filteredProducts = remember(productsState, searchQuery, selectedCategory) {
        val list = (productsState as? UiState.Success)?.data.orEmpty()
        list.filter { item ->
            val matchesCat = selectedCategory == "All" ||
                item.category.equals(selectedCategory, ignoreCase = true)
            val query = searchQuery.trim()
            val matchesQuery = query.isEmpty() ||
                item.name.contains(query, ignoreCase = true) ||
                item.fileName.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StoreHeroBanner(isAdmin = isAdmin, onGoToAdminUpload = onGoToAdminUpload)
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search .apk packages, version, or tool...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search APKs",
                            tint = NeonGreen
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderEmerald,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        cursorColor = NeonGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_search_input")
                )
            }

            if (categories.size > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = cat == selectedCategory
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        text = cat.uppercase(),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
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
            }

            item {
                SectionTerminalHeader(
                    tag = "REAL .APK RELEASES",
                    title = "Available Packages"
                )
            }

            when (productsState) {
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = CrimsonReject
                                )
                                Text(
                                    text = productsState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CrimsonReject
                                )
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    if (filteredProducts.isEmpty()) {
                        item {
                            EmptyStoreVaultCard(
                                hasQuery = searchQuery.isNotBlank() || selectedCategory != "All",
                                isAdmin = isAdmin,
                                onGoToAdminUpload = onGoToAdminUpload
                            )
                        }
                    } else if (isWide) {
                        val rows = filteredProducts.chunked(2)
                        items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                pair.forEach { product ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ApkProductCard(
                                            product = product,
                                            onSelect = { onSelectProduct(product) }
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            AnimatedVisibility(visible = true, enter = fadeIn()) {
                                ApkProductCard(
                                    product = product,
                                    onSelect = { onSelectProduct(product) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreHeroBanner(
    isAdmin: Boolean,
    onGoToAdminUpload: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.55f)),
        color = CardSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "Panel Injector FF Store Hero",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ObsidianBlack.copy(alpha = 0.94f),
                                ObsidianBlack.copy(alpha = 0.72f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NeonGlowContainer,
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "FIREBASE STORAGE • REAL .APK FILES",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "PANEL INJECTOR FF",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NeonGreen
                    )
                    Text(
                        text = "Pay via Easypaisa or JazzCash. Unlock verified .apk downloads once approved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onGoToAdminUpload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = ObsidianBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("hero_admin_upload_btn")
                        ) {
                            Text(
                                text = "+ UPLOAD REAL APK",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkProductCard(
    product: ApkProduct,
    onSelect: () -> Unit
) {
    CyberCard(
        highlighted = false,
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_apk_emblem),
                contentDescription = "${product.name} APK icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonGlowContainer
                    ) {
                        Text(
                            text = product.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "v${product.version.removePrefix("v")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonGreen.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = product.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatBytes(product.fileSizeBytes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatPkr(product.pricePkr),
                    style = MaterialTheme.typography.headlineSmall,
                    color = NeonGreen
                )
            }

            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = ObsidianBlack
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("buy_product_btn_${product.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCartCheckout,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GET APK",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun EmptyStoreVaultCard(
    hasQuery: Boolean,
    isAdmin: Boolean,
    onGoToAdminUpload: () -> Unit
) {
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
                contentDescription = "Empty APK Vault",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Text(
                text = if (hasQuery) "No Matching APK Releases" else "APK Vault Is Empty",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (hasQuery) {
                    "Try clearing your search filter or switching category."
                } else if (isAdmin) {
                    "Upload your first real .apk file in the Admin Vault tab so customers can browse and purchase it."
                } else {
                    "No active .apk builds are published yet. Check back once an admin uploads a release."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isAdmin && !hasQuery) {
                Button(
                    onClick = onGoToAdminUpload,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Android,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("UPLOAD FIRST .APK", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050807, widthDp = 412, heightDp = 892)
@Composable
private fun StorefrontScreenMobilePreview() {
    com.example.ui.theme.PanelInjectorTheme {
        StorefrontScreen(
            productsState = UiState.Success(
                listOf(
                    ApkProduct(
                        id = "preview_1",
                        name = "FF Injector Pro",
                        version = "4.2",
                        category = "Injector",
                        description = "Verified Android APK package delivered after payment approval.",
                        pricePkr = 1500L,
                        fileName = "ff-injector-pro-v4.2.apk",
                        fileSizeBytes = 14_680_064L
                    )
                )
            ),
            isAdmin = false,
            onSelectProduct = {},
            onGoToAdminUpload = {}
        )
    }
}
