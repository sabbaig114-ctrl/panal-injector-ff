package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class OrderStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

enum class PaymentMethod(val label: String) {
    EASYPAISA("Easypaisa"),
    JAZZCASH("JazzCash");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: EASYPAISA
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class ApkProduct(
    @DocumentId val id: String = "",
    val name: String = "",
    val version: String = "",
    val category: String = "",
    val description: String = "",
    val pricePkr: Long = 0L,
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val active: Boolean = true,
    val ownerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductAsset(
    @DocumentId val id: String = "",
    val productId: String = "",
    val storagePath: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val ownerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ApkOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val productId: String = "",
    val productName: String = "",
    val productVersion: String = "",
    val pricePkr: Long = 0L,
    val customerName: String = "",
    val whatsappNumber: String = "",
    val paymentMethod: String = PaymentMethod.EASYPAISA.name,
    val transactionId: String = "",
    val status: String = OrderStatus.PENDING.name,
    val adminNote: String? = null,
    val approvedStoragePath: String? = null,
    val approvedFileName: String? = null,
    val approvedFileSizeBytes: Long? = null,
    val reviewedBy: String? = null,
    val reviewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: OrderStatus
        get() = OrderStatus.fromRaw(status)

    val parsedPaymentMethod: PaymentMethod
        get() = PaymentMethod.fromRaw(paymentMethod)
}

@Keep
@IgnoreExtraProperties
data class PaymentSettings(
    @DocumentId val id: String = "",
    val easypaisaTitle: String = "",
    val easypaisaNumber: String = "",
    val jazzcashTitle: String = "",
    val jazzcashNumber: String = "",
    val instructions: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DownloadGrant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val productId: String = "",
    val storagePath: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val status: String = OrderStatus.APPROVED.name,
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface DownloadVerificationResult {
    data class Verified(
        val orderId: String,
        val productId: String,
        val storagePath: String,
        val fileName: String,
        val fileSizeBytes: Long,
        val verifiedByAdminUid: String
    ) : DownloadVerificationResult

    data class Locked(
        val status: OrderStatus,
        val reason: String
    ) : DownloadVerificationResult
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.2f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

fun formatPkr(amount: Long): String {
    return "PKR %,d".format(java.util.Locale.US, amount)
}
