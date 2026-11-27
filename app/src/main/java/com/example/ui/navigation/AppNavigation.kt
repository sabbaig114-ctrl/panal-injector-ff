package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.StoreRepository
import com.example.data.model.OrderStatus
import com.example.data.model.UiState
import com.example.ui.components.CyberMatrixBackground
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MyOrdersScreen
import com.example.ui.screens.ProductCheckoutScreen
import com.example.ui.screens.StorefrontScreen
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.CardSurface
import com.example.ui.theme.NeonGlowContainer
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TerminalSurface
import com.example.ui.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

object StoreRoutes {
    const val ROUTE_STORE = "store"
    const val ROUTE_ORDERS = "orders"
    const val ROUTE_ADMIN = "admin"
    const val ROUTE_ACCOUNT = "account"
    const val ROUTE_CHECKOUT = "checkout/{productId}"

    fun checkoutRoute(productId: String): String = "checkout/$productId"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedStoreShell(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedStoreShell(user: FirebaseUser) {
    val uid = user.uid
    val email = user.email.orEmpty()
    val emailVerified = user.isEmailVerified
    val displayName = user.displayName.orEmpty()

    val viewModel: StoreViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = StoreRepository(
                    db = db,
                    storage = Firebase.storage
                )
                StoreViewModel(
                    repository = repo,
                    currentUserId = uid,
                    userEmail = email,
                    emailVerified = emailVerified,
                    displayName = displayName
                )
            }
        }
    )

    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val activeProducts by viewModel.activeProducts.collectAsStateWithLifecycle()
    val customerOrders by viewModel.customerOrders.collectAsStateWithLifecycle()
    val paymentSettings by viewModel.paymentSettings.collectAsStateWithLifecycle()
    val adminProducts by viewModel.adminProducts.collectAsStateWithLifecycle()
    val adminOrders by viewModel.adminOrders.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val pickedApk by viewModel.pickedApk.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val isSubmittingOrder by viewModel.isSubmittingOrder.collectAsStateWithLifecycle()
    val isProcessingAdminAction by viewModel.isProcessingAdminAction.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()

    var adminInitialSubTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val pendingAdminOrdersCount = remember(adminOrders, isAdmin) {
        if (!isAdmin) 0
        else (adminOrders as? UiState.Success)?.data
            ?.count { it.parsedStatus == OrderStatus.PENDING } ?: 0
    }

    val approvedOrdersCount = remember(customerOrders) {
        (customerOrders as? UiState.Success)?.data
            ?.count { it.parsedStatus == OrderStatus.APPROVED } ?: 0
    }

    val navItems = remember(isAdmin, pendingAdminOrdersCount, approvedOrdersCount) {
        buildList {
            add(
                NavTabItem(
                    route = StoreRoutes.ROUTE_STORE,
                    label = "Store",
                    selectedIcon = Icons.Filled.Storefront,
                    unselectedIcon = Icons.Outlined.Storefront
                )
            )
            add(
                NavTabItem(
                    route = StoreRoutes.ROUTE_ORDERS,
                    label = "My Orders",
                    selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    badgeCount = approvedOrdersCount
                )
            )
            if (isAdmin) {
                add(
                    NavTabItem(
                        route = StoreRoutes.ROUTE_ADMIN,
                        label = "Admin",
                        selectedIcon = Icons.Filled.AdminPanelSettings,
                        unselectedIcon = Icons.Outlined.AdminPanelSettings,
                        badgeCount = pendingAdminOrdersCount
                    )
                )
            }
            add(
                NavTabItem(
                    route = StoreRoutes.ROUTE_ACCOUNT,
                    label = "Account",
                    selectedIcon = Icons.Filled.Person,
                    unselectedIcon = Icons.Outlined.Person
                )
            )
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: StoreRoutes.ROUTE_STORE
    val isCheckoutScreen = currentRoute.startsWith("checkout/")

    val navigateToTab: (String) -> Unit = { targetRoute ->
        navController.navigate(targetRoute) {
            popUpTo(StoreRoutes.ROUTE_STORE) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showToastMessage: (String) -> Unit = { msg ->
        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = CardSurface,
                        contentColor = NeonGreen,
                        actionColor = NeonGreen
                    )
                }
            },
            topBar = {
                if (!isCheckoutScreen) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "PANEL INJECTOR FF",
                                style = MaterialTheme.typography.headlineSmall,
                                color = NeonGreen
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = TerminalSurface
                        )
                    )
                }
            },
            bottomBar = {
                if (!useRail && !isCheckoutScreen) {
                    NavigationBar(
                        containerColor = TerminalSurface,
                        tonalElevation = 4.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge(
                                                    containerColor = NeonGreen,
                                                    contentColor = ObsidianBlack
                                                ) {
                                                    Text(item.badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonGreen,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGlowContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            CyberMatrixBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail && !isCheckoutScreen) {
                        NavigationRail(
                            containerColor = TerminalSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(item.route) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (item.badgeCount > 0) {
                                                    Badge(
                                                        containerColor = NeonGreen,
                                                        contentColor = ObsidianBlack
                                                    ) {
                                                        Text(item.badgeCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = NeonGreen,
                                        selectedTextColor = NeonGreen,
                                        indicatorColor = NeonGlowContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("rail_${item.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = StoreRoutes.ROUTE_STORE
                        ) {
                            composable(StoreRoutes.ROUTE_STORE) {
                                StorefrontScreen(
                                    productsState = activeProducts,
                                    isAdmin = isAdmin,
                                    onSelectProduct = { product ->
                                        navController.navigate(StoreRoutes.checkoutRoute(product.id))
                                    },
                                    onGoToAdminUpload = {
                                        adminInitialSubTab = 1
                                        navigateToTab(StoreRoutes.ROUTE_ADMIN)
                                    }
                                )
                            }

                            composable(StoreRoutes.ROUTE_ORDERS) {
                                MyOrdersScreen(
                                    ordersState = customerOrders,
                                    downloadStates = downloadStates,
                                    currentUserId = uid,
                                    onDownloadApk = { order, uri ->
                                        viewModel.downloadApprovedApk(context, order, uri)
                                    },
                                    onBrowseStore = {
                                        navigateToTab(StoreRoutes.ROUTE_STORE)
                                    }
                                )
                            }

                            composable(StoreRoutes.ROUTE_ADMIN) {
                                if (isAdmin) {
                                    AdminScreen(
                                        ordersState = adminOrders,
                                        productsState = adminProducts,
                                        adminsState = adminUsers,
                                        paymentSettings = paymentSettings,
                                        pickedApk = pickedApk,
                                        uploadStatus = uploadStatus,
                                        isProcessing = isProcessingAdminAction,
                                        initialSubTab = adminInitialSubTab,
                                        onCopyFeedback = showToastMessage,
                                        onApproveOrder = { order, note ->
                                            viewModel.approveOrder(order, note)
                                        },
                                        onRejectOrder = { order, note ->
                                            viewModel.rejectOrder(order, note)
                                        },
                                        onPickApkUri = { uri ->
                                            viewModel.inspectPickedApk(context, uri)
                                        },
                                        onUploadProduct = { name, ver, cat, desc, price, active, clear ->
                                            viewModel.uploadApkProduct(name, ver, cat, desc, price, active, clear)
                                        },
                                        onToggleProductActive = { id, active ->
                                            viewModel.toggleProductActive(id, active)
                                        },
                                        onDeleteProduct = { id ->
                                            viewModel.deleteProduct(id)
                                        },
                                        onSavePaymentSettings = { epT, epN, jcT, jcN, instr ->
                                            viewModel.savePaymentSettings(epT, epN, jcT, jcN, instr)
                                        },
                                        onGrantAdmin = { targetUid, targetEmail, targetName ->
                                            viewModel.grantAdminAccess(targetUid, targetEmail, targetName)
                                        },
                                        onRevokeAdmin = { targetUid ->
                                            viewModel.revokeAdminAccess(targetUid)
                                        }
                                    )
                                } else {
                                    StorefrontScreen(
                                        productsState = activeProducts,
                                        isAdmin = false,
                                        onSelectProduct = { product ->
                                            navController.navigate(StoreRoutes.checkoutRoute(product.id))
                                        },
                                        onGoToAdminUpload = {}
                                    )
                                }
                            }

                            composable(StoreRoutes.ROUTE_ACCOUNT) {
                                AccountScreen(
                                    uid = uid,
                                    email = email,
                                    displayName = displayName,
                                    isAdmin = isAdmin,
                                    onCopyFeedback = showToastMessage
                                )
                            }

                            composable(
                                route = StoreRoutes.ROUTE_CHECKOUT,
                                arguments = listOf(navArgument("productId") { type = NavType.StringType })
                            ) { entry ->
                                val productId = entry.arguments?.getString("productId").orEmpty()
                                val product = (activeProducts as? UiState.Success)?.data
                                    ?.firstOrNull { it.id == productId }
                                    ?: (adminProducts as? UiState.Success)?.data
                                        ?.firstOrNull { it.id == productId }

                                if (product != null) {
                                    val existingOrder = (customerOrders as? UiState.Success)?.data
                                        ?.firstOrNull { it.productId == product.id }

                                    ProductCheckoutScreen(
                                        product = product,
                                        defaultCustomerName = displayName,
                                        paymentSettings = paymentSettings,
                                        existingOrder = existingOrder,
                                        isSubmitting = isSubmittingOrder,
                                        onCopyFeedback = showToastMessage,
                                        onSubmitOrder = { custName, whatsapp, method, tid ->
                                            viewModel.placeOrder(
                                                product = product,
                                                customerName = custName,
                                                whatsappNumber = whatsapp,
                                                paymentMethod = method,
                                                transactionId = tid,
                                                onSuccess = {
                                                    navController.popBackStack()
                                                    navigateToTab(StoreRoutes.ROUTE_ORDERS)
                                                }
                                            )
                                        },
                                        onGoToOrders = {
                                            navController.popBackStack()
                                            navigateToTab(StoreRoutes.ROUTE_ORDERS)
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                } else {
                                    LaunchedEffect(productId) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
