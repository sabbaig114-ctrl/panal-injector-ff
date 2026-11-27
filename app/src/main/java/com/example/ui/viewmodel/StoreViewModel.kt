package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PickedApkInfo
import com.example.data.SavedApkResult
import com.example.data.StoreRepository
import com.example.data.model.AdminUser
import com.example.data.model.ApkOrder
import com.example.data.model.ApkProduct
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentSettings
import com.example.data.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UploadStatus {
    data object Idle : UploadStatus
    data class Uploading(val progress: Float) : UploadStatus
}

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data object Verifying : DownloadStatus
    data class Downloading(val bytesTransferred: Long, val totalBytes: Long) : DownloadStatus
    data class Completed(val result: SavedApkResult) : DownloadStatus
    data class Failed(val message: String) : DownloadStatus
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModel(
    private val repository: StoreRepository,
    val currentUserId: String,
    val userEmail: String,
    val emailVerified: Boolean,
    val displayName: String
) : ViewModel() {

    private companion object {
        const val TAG = "StoreViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private fun <T> Flow<T>.toUiStateFlow(tagLabel: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tagLabel", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $tagLabel"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val isAdmin: StateFlow<Boolean> = repository
        .observeIsAdmin(currentUserId, userEmail, emailVerified)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emailVerified && userEmail.equals(
                StoreRepository.BOOTSTRAPPED_ADMIN_EMAIL,
                ignoreCase = true
            )
        )

    val activeProducts: StateFlow<UiState<List<ApkProduct>>> = repository
        .observeActiveProducts()
        .toUiStateFlow("active products")

    val customerOrders: StateFlow<UiState<List<ApkOrder>>> = repository
        .observeCustomerOrders(currentUserId)
        .toUiStateFlow("customer orders")

    val paymentSettings: StateFlow<PaymentSettings?> = repository
        .observePaymentSettings()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing payment settings", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val adminProducts: StateFlow<UiState<List<ApkProduct>>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllProductsForAdmin() else flowOf(emptyList())
        }
        .toUiStateFlow("admin products")

    val adminOrders: StateFlow<UiState<List<ApkOrder>>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllOrdersForAdmin() else flowOf(emptyList())
        }
        .toUiStateFlow("admin orders")

    val adminUsers: StateFlow<UiState<List<AdminUser>>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAdmins() else flowOf(emptyList())
        }
        .toUiStateFlow("admin users")

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

    private val _pickedApk = MutableStateFlow<PickedApkInfo?>(null)
    val pickedApk: StateFlow<PickedApkInfo?> = _pickedApk.asStateFlow()

    private val _isSubmittingOrder = MutableStateFlow(false)
    val isSubmittingOrder: StateFlow<Boolean> = _isSubmittingOrder.asStateFlow()

    private val _isProcessingAdminAction = MutableStateFlow(false)
    val isProcessingAdminAction: StateFlow<Boolean> = _isProcessingAdminAction.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun inspectPickedApk(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = repository.inspectPickedApk(context, uri)
            result.onSuccess { info ->
                _pickedApk.value = info
                _events.tryEmit("Verified APK: ${info.fileName}")
            }.onFailure { err ->
                _pickedApk.value = null
                _events.tryEmit(err.localizedMessage ?: "Invalid APK file selected.")
            }
        }
    }

    fun clearPickedApk() {
        _pickedApk.value = null
    }

    fun uploadApkProduct(
        name: String,
        version: String,
        category: String,
        description: String,
        pricePkrText: String,
        active: Boolean,
        onSuccess: () -> Unit
    ) {
        val apkInfo = _pickedApk.value
        if (apkInfo == null) {
            _events.tryEmit("Please select a real .apk file first.")
            return
        }
        val price = pricePkrText.trim().toLongOrNull()
        if (price == null || price <= 0L) {
            _events.tryEmit("Enter a valid PKR price greater than 0.")
            return
        }

        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Uploading(0f)
            val result = repository.uploadApkAndCreateProduct(
                apkInfo = apkInfo,
                name = name,
                version = version,
                category = category,
                description = description,
                pricePkr = price,
                active = active,
                onProgress = { progress ->
                    _uploadStatus.value = UploadStatus.Uploading(progress)
                }
            )
            _uploadStatus.value = UploadStatus.Idle
            result.onSuccess {
                _pickedApk.value = null
                _events.tryEmit("APK uploaded to Firebase Storage and published.")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "APK upload failed.")
            }
        }
    }

    fun toggleProductActive(productId: String, active: Boolean) {
        viewModelScope.launch {
            _isProcessingAdminAction.value = true
            val res = repository.toggleProductActive(productId, active)
            _isProcessingAdminAction.value = false
            res.onSuccess {
                _events.tryEmit(if (active) "Product enabled in store." else "Product hidden from store.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update product visibility.")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _isProcessingAdminAction.value = true
            val res = repository.deleteProduct(productId)
            _isProcessingAdminAction.value = false
            res.onSuccess {
                _events.tryEmit("Product and APK file removed.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to delete product.")
            }
        }
    }

    fun placeOrder(
        product: ApkProduct,
        customerName: String,
        whatsappNumber: String,
        paymentMethod: PaymentMethod,
        transactionId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmittingOrder.value = true
            val result = repository.placeOrder(
                product = product,
                customerName = customerName,
                whatsappNumber = whatsappNumber,
                paymentMethod = paymentMethod,
                transactionId = transactionId
            )
            _isSubmittingOrder.value = false
            result.onSuccess {
                _events.tryEmit("Order submitted! Waiting for admin payment verification.")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not place order.")
            }
        }
    }

    fun approveOrder(order: ApkOrder, adminNote: String) {
        viewModelScope.launch {
            _isProcessingAdminAction.value = true
            val res = repository.approveOrder(order, adminNote)
            _isProcessingAdminAction.value = false
            res.onSuccess {
                _events.tryEmit("Order approved & real APK unlocked for ${order.customerName}.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Approval failed.")
            }
        }
    }

    fun rejectOrder(order: ApkOrder, adminNote: String) {
        viewModelScope.launch {
            _isProcessingAdminAction.value = true
            val res = repository.rejectOrder(order, adminNote)
            _isProcessingAdminAction.value = false
            res.onSuccess {
                _events.tryEmit("Order rejected & APK access locked.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Rejection failed.")
            }
        }
    }

    fun savePaymentSettings(
        easypaisaTitle: String,
        easypaisaNumber: String,
        jazzcashTitle: String,
        jazzcashNumber: String,
        instructions: String
    ) {
        viewModelScope.launch {
            _isProcessingAdminAction.value = true
            val exists = paymentSettings.value != null
            val res = repository.savePaymentSettings(
                easypaisaTitle = easypaisaTitle,
                easypaisaNumber = easypaisaNumber,
                jazzcashTitle = jazzcashTitle,
                jazzcashNumber = jazzcashNumber,
                instructions = instructions,
                alreadyExists = exists
            )
            _isProcessingAdminAction.value = false
            res.onSuccess {
                _events.tryEmit("Easypaisa & JazzCash details saved.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save payment details.")
            }
        }
    }

    fun grantAdminAccess(targetUid: String, targetEmail: String, targetName: String) {
        viewModelScope.launch {
            _isProcessingAdminAction.value = true
            val res = repository.grantAdminAccess(targetUid, targetEmail, targetName)
            _isProcessingAdminAction.value = false
            res.onSuccess {
                _events.tryEmit("Admin access granted to $targetEmail.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to grant admin access.")
            }
        }
    }

    fun revokeAdminAccess(targetUid: String) {
        viewModelScope.launch {
            _isProcessingAdminAction.value = true
            val res = repository.revokeAdminAccess(targetUid)
            _isProcessingAdminAction.value = false
            res.onSuccess {
                _events.tryEmit("Admin access revoked.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to revoke admin access.")
            }
        }
    }

    fun downloadApprovedApk(context: Context, order: ApkOrder, destinationUri: Uri? = null) {
        if (order.userId != currentUserId) {
            _events.tryEmit("Access denied: Order ownership check failed.")
            return
        }
        viewModelScope.launch {
            _downloadStates.update { it + (order.id to DownloadStatus.Verifying) }
            val result = repository.downloadApprovedApk(
                context = context,
                orderId = order.id,
                destinationUri = destinationUri,
                onVerified = { grant ->
                    _downloadStates.update {
                        it + (order.id to DownloadStatus.Downloading(0L, grant.fileSizeBytes))
                    }
                },
                onProgress = { transferred, total ->
                    _downloadStates.update {
                        it + (order.id to DownloadStatus.Downloading(transferred, total))
                    }
                }
            )
            result.onSuccess { saved ->
                _downloadStates.update { it + (order.id to DownloadStatus.Completed(saved)) }
                _events.tryEmit("Verified & downloaded real APK: ${saved.displayLocation}")
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Verification or download failed."
                _downloadStates.update { it + (order.id to DownloadStatus.Failed(msg)) }
                _events.tryEmit(msg)
            }
        }
    }
}
