package com.example.data

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.R
import com.example.data.model.AdminUser
import com.example.data.model.ApkOrder
import com.example.data.model.ApkProduct
import com.example.data.model.DownloadGrant
import com.example.data.model.DownloadVerificationResult
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class PickedApkInfo(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long
)

data class SavedApkResult(
    val fileName: String,
    val bytesWritten: Long,
    val displayLocation: String,
    val contentUri: Uri?
)

class StoreRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        storage = Firebase.storage
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "bkainat419@gmail.com"
        const val MAX_APK_SIZE_BYTES = 536_870_912L // 512 MB
        private const val PAYMENT_SETTINGS_DOC = "default"
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing data.")

    fun observeIsAdmin(uid: String, email: String?, emailVerified: Boolean): Flow<Boolean> {
        if (uid.isBlank()) return flowOf(false)
        if (emailVerified && email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)) {
            return flowOf(true)
        }
        val path = "admins/$uid"
        return db.collection("admins").document(uid)
            .snapshots()
            .map { snapshot -> snapshot.exists() }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.GET, path)
                }
                emit(false)
            }
    }

    fun observeActiveProducts(): Flow<List<ApkProduct>> = flow {
        val uid = requireUserId()
        val path = "products"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ApkProduct::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllProductsForAdmin(): Flow<List<ApkProduct>> = flow {
        val uid = requireUserId()
        val path = "products"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ApkProduct::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCustomerOrders(userId: String): Flow<List<ApkOrder>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for order stream." }
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ApkOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllOrdersForAdmin(): Flow<List<ApkOrder>> = flow {
        val uid = requireUserId()
        val path = "orders"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ApkOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePaymentSettings(): Flow<PaymentSettings?> = flow {
        val uid = requireUserId()
        val path = "payment_settings/$PAYMENT_SETTINGS_DOC"
        emitAll(
            db.collection("payment_settings").document(PAYMENT_SETTINGS_DOC)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PaymentSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeAdmins(): Flow<List<AdminUser>> = flow {
        val uid = requireUserId()
        val path = "admins"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminUser::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.email }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun inspectPickedApk(context: Context, uri: Uri): Result<PickedApkInfo> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                var displayName = "package.apk"
                var fileSize = -1L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) {
                            displayName = cursor.getString(nameIdx) ?: displayName
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            fileSize = cursor.getLong(sizeIdx)
                        }
                    }
                }

                if (!displayName.trim().lowercase().endsWith(".apk")) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Only real .apk files are allowed (selected: $displayName).")
                    )
                }

                // Verify genuine ZIP/APK magic bytes (PK\x03\x04) and compute size if unknown
                val header = ByteArray(4)
                var measuredSize = 0L
                resolver.openInputStream(uri)?.use { input ->
                    val firstRead = input.read(header)
                    if (firstRead < 4 ||
                        header[0] != 0x50.toByte() ||
                        header[1] != 0x4B.toByte() ||
                        header[2] != 0x03.toByte() ||
                        header[3] != 0x04.toByte()
                    ) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Invalid APK binary signature. Select a compiled .apk file.")
                        )
                    }
                    measuredSize += firstRead
                    if (fileSize <= 0L) {
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            measuredSize += read
                        }
                        fileSize = measuredSize
                    }
                } ?: return@withContext Result.failure(
                    IllegalStateException("Unable to read the selected .apk file.")
                )

                if (fileSize <= 0L || fileSize > MAX_APK_SIZE_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("APK size must be between 1 byte and 512 MB.")
                    )
                }

                Result.success(PickedApkInfo(uri = uri, fileName = displayName.trim(), fileSizeBytes = fileSize))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun uploadApkAndCreateProduct(
        apkInfo: PickedApkInfo,
        name: String,
        version: String,
        category: String,
        description: String,
        pricePkr: Long,
        active: Boolean,
        onProgress: (Float) -> Unit
    ): Result<String> {
        val productId = "apk_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val productPath = "products/$productId"
        val assetPath = "product_assets/$productId"

        val cleanName = name.trim()
        val cleanVersion = version.trim()
        val cleanCategory = category.trim()
        val cleanDesc = description.trim()
        val cleanFileName = apkInfo.fileName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .let { if (it.lowercase().endsWith(".apk")) it else "$it.apk" }
            .take(150)

        return try {
            val uid = requireUserId()
            require(cleanName.length in 2..80) { "Product name must be 2 to 80 characters." }
            require(cleanVersion.length in 1..32) { "Version must be 1 to 32 characters." }
            require(cleanCategory.length in 2..40) { "Category must be 2 to 40 characters." }
            require(cleanDesc.length in 5..1000) { "Description must be 5 to 1000 characters." }
            require(pricePkr in 1L..1_000_000L) { "Price must be between PKR 1 and 1,000,000." }
            require(cleanFileName.length in 5..150 && cleanFileName.lowercase().endsWith(".apk")) {
                "File must be a valid .apk filename."
            }
            require(apkInfo.fileSizeBytes in 1L..MAX_APK_SIZE_BYTES) {
                "APK file size must be under 512 MB."
            }

            val storagePath = "apks/$productId/$cleanFileName"
            val storageRef = storage.reference.child(storagePath)
            val metadata = StorageMetadata.Builder()
                .setContentType("application/vnd.android.package-archive")
                .build()

            val uploadTask = storageRef.putFile(apkInfo.uri, metadata)
            uploadTask.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                if (total > 0L) {
                    onProgress((snapshot.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                }
            }
            uploadTask.await()
            onProgress(1f)

            val productPayload = mapOf(
                "name" to cleanName,
                "version" to cleanVersion,
                "category" to cleanCategory,
                "description" to cleanDesc,
                "pricePkr" to pricePkr,
                "fileName" to cleanFileName,
                "fileSizeBytes" to apkInfo.fileSizeBytes,
                "active" to active,
                "ownerUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val assetPayload = mapOf(
                "productId" to productId,
                "storagePath" to storagePath,
                "fileName" to cleanFileName,
                "fileSizeBytes" to apkInfo.fileSizeBytes,
                "ownerUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            try {
                db.batch().apply {
                    set(db.document(productPath), productPayload)
                    set(db.document(assetPath), assetPayload)
                }.commit().await()
            } catch (firestoreEx: Exception) {
                runCatching { storageRef.delete().await() }
                throw firestoreEx
            }

            Result.success(productId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            val friendly = when (e.errorCode) {
                StorageException.ERROR_BUCKET_NOT_FOUND,
                StorageException.ERROR_PROJECT_NOT_FOUND,
                StorageException.ERROR_NOT_AUTHORIZED ->
                    "Firebase Storage is not enabled or rules block uploads. Enable Firebase Storage in Firebase Console for this project and deploy storage.rules."
                else -> e.localizedMessage ?: "Firebase Storage upload failed."
            }
            Result.failure(IllegalStateException(friendly, e))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, productPath)
            Result.failure(e)
        }
    }

    suspend fun toggleProductActive(productId: String, active: Boolean): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank()) { "Product ID required." }
            db.document(docPath).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val productPath = "products/$productId"
        val assetPath = "product_assets/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank()) { "Product ID required." }
            val assetSnap = db.document(assetPath).get().await()
            val storagePath = assetSnap.getString("storagePath")

            db.batch().apply {
                delete(db.document(productPath))
                delete(db.document(assetPath))
            }.commit().await()

            if (!storagePath.isNullOrBlank()) {
                runCatching { storage.reference.child(storagePath).delete().await() }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, productPath)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        product: ApkProduct,
        customerName: String,
        whatsappNumber: String,
        paymentMethod: PaymentMethod,
        transactionId: String
    ): Result<String> {
        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docPath = "orders/$orderId"
        val cleanName = customerName.trim()
        val cleanWhatsapp = whatsappNumber.trim()
        val cleanTid = transactionId.trim()

        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("Please sign in with Google before placing an order.")
            val uid = user.uid
            val email = user.email?.takeIf { it.length >= 3 } ?: "$uid@google-user.local"

            require(product.id.isNotBlank()) { "Invalid product selection." }
            require(cleanName.length in 2..80) { "Name must be 2 to 80 characters." }
            require(cleanWhatsapp.length in 7..25) { "Enter a valid WhatsApp number (7–25 chars)." }
            require(cleanTid.length in 4..64) { "Transaction ID must be 4 to 64 characters." }

            val payload = mapOf(
                "userId" to uid,
                "userEmail" to email,
                "productId" to product.id,
                "productName" to product.name,
                "productVersion" to product.version,
                "pricePkr" to product.pricePkr,
                "customerName" to cleanName,
                "whatsappNumber" to cleanWhatsapp,
                "paymentMethod" to paymentMethod.name,
                "transactionId" to cleanTid,
                "status" to OrderStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(docPath).set(payload).await()
            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun approveOrder(order: ApkOrder, adminNote: String): Result<Unit> {
        val orderPath = "orders/${order.id}"
        val assetPath = "product_assets/${order.productId}"
        val grantPath = "products/${order.productId}/grants/${order.userId}"
        val cleanNote = adminNote.trim().take(300)
        return try {
            val adminUid = requireUserId()
            require(order.id.isNotBlank()) { "Order ID missing." }
            require(order.userId.isNotBlank()) { "Order user ID missing." }
            require(order.productId.isNotBlank()) { "Order product ID missing." }

            val assetSnap = db.document(assetPath).get(Source.SERVER).await()
            val storagePath = assetSnap.getString("storagePath")
                ?: throw IllegalStateException("APK asset file record missing for this product.")
            val fileName = assetSnap.getString("fileName")
                ?: throw IllegalStateException("APK file name missing for this product.")
            val fileSize = assetSnap.getLong("fileSizeBytes")
                ?: throw IllegalStateException("APK file size missing for this product.")

            val orderUpdatePayload = mutableMapOf<String, Any>(
                "status" to OrderStatus.APPROVED.name,
                "approvedStoragePath" to storagePath,
                "approvedFileName" to fileName,
                "approvedFileSizeBytes" to fileSize,
                "reviewedBy" to adminUid,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "adminNote" to if (cleanNote.isNotEmpty()) cleanNote else FieldValue.delete()
            )

            val grantPayload = mapOf(
                "userId" to order.userId,
                "orderId" to order.id,
                "productId" to order.productId,
                "storagePath" to storagePath,
                "fileName" to fileName,
                "fileSizeBytes" to fileSize,
                "status" to OrderStatus.APPROVED.name,
                "grantedBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.batch().apply {
                update(db.document(orderPath), orderUpdatePayload)
                set(db.document(grantPath), grantPayload)
            }.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderPath)
            Result.failure(e)
        }
    }

    suspend fun rejectOrder(order: ApkOrder, adminNote: String): Result<Unit> {
        val orderPath = "orders/${order.id}"
        val grantPath = "products/${order.productId}/grants/${order.userId}"
        val cleanNote = adminNote.trim().take(300)
        return try {
            val adminUid = requireUserId()
            require(order.id.isNotBlank()) { "Order ID missing." }

            val orderUpdatePayload = mapOf(
                "status" to OrderStatus.REJECTED.name,
                "approvedStoragePath" to FieldValue.delete(),
                "approvedFileName" to FieldValue.delete(),
                "approvedFileSizeBytes" to FieldValue.delete(),
                "reviewedBy" to adminUid,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "adminNote" to if (cleanNote.isNotEmpty()) cleanNote else FieldValue.delete()
            )

            db.batch().apply {
                update(db.document(orderPath), orderUpdatePayload)
                if (order.productId.isNotBlank() && order.userId.isNotBlank()) {
                    delete(db.document(grantPath))
                }
            }.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderPath)
            Result.failure(e)
        }
    }

    suspend fun savePaymentSettings(
        easypaisaTitle: String,
        easypaisaNumber: String,
        jazzcashTitle: String,
        jazzcashNumber: String,
        instructions: String,
        alreadyExists: Boolean
    ): Result<Unit> {
        val docPath = "payment_settings/$PAYMENT_SETTINGS_DOC"
        val epTitle = easypaisaTitle.trim()
        val epNum = easypaisaNumber.trim()
        val jcTitle = jazzcashTitle.trim()
        val jcNum = jazzcashNumber.trim()
        val cleanInstructions = instructions.trim()

        return try {
            val uid = requireUserId()
            require(epTitle.length in 2..80) { "Easypaisa title must be 2–80 characters." }
            require(epNum.length in 7..25) { "Easypaisa number must be 7–25 characters." }
            require(jcTitle.length in 2..80) { "JazzCash title must be 2–80 characters." }
            require(jcNum.length in 7..25) { "JazzCash number must be 7–25 characters." }
            require(cleanInstructions.length <= 500) { "Instructions must be 500 characters or less." }

            val docRef = db.document(docPath)
            if (alreadyExists) {
                docRef.update(
                    mapOf(
                        "easypaisaTitle" to epTitle,
                        "easypaisaNumber" to epNum,
                        "jazzcashTitle" to jcTitle,
                        "jazzcashNumber" to jcNum,
                        "instructions" to cleanInstructions,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "easypaisaTitle" to epTitle,
                        "easypaisaNumber" to epNum,
                        "jazzcashTitle" to jcTitle,
                        "jazzcashNumber" to jcNum,
                        "instructions" to cleanInstructions,
                        "updatedBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun grantAdminAccess(targetUid: String, targetEmail: String, targetName: String): Result<Unit> {
        val cleanUid = targetUid.trim()
        val cleanEmail = targetEmail.trim()
        val cleanName = targetName.trim().ifBlank { "Admin" }
        val docPath = "admins/$cleanUid"
        return try {
            val adminUid = requireUserId()
            require(cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]+$")) && cleanUid.length in 1..128) {
                "Enter a valid Firebase UID."
            }
            require(cleanEmail.length in 3..150) { "Enter a valid email address." }
            require(cleanName.length in 1..100) { "Display name must be 1–100 characters." }

            db.document(docPath).set(
                mapOf(
                    "uid" to cleanUid,
                    "email" to cleanEmail,
                    "displayName" to cleanName,
                    "grantedBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun revokeAdminAccess(targetUid: String): Result<Unit> {
        val docPath = "admins/$targetUid"
        return try {
            val currentUid = requireUserId()
            require(targetUid != currentUid) { "You cannot revoke your own active admin access." }
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Authoritatively validates a buyer's order status, ownership, DownloadGrant entitlement,
     * and Firebase Storage .apk presence directly against the server before granting download access.
     */
    suspend fun verifyOrderDownloadAccess(orderId: String): Result<DownloadVerificationResult> =
        withContext(Dispatchers.IO) {
            val orderPath = "orders/$orderId"
            try {
                val uid = requireUserId()
                require(orderId.isNotBlank()) { "Order ID must not be blank." }

                val serverOrderSnap = db.document(orderPath).get(Source.SERVER).await()
                if (!serverOrderSnap.exists()) {
                    return@withContext Result.success(
                        DownloadVerificationResult.Locked(
                            status = OrderStatus.PENDING,
                            reason = "Order record was not found on the server."
                        )
                    )
                }

                val serverOrder = serverOrderSnap.toObject(
                    ApkOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: return@withContext Result.failure(
                    IllegalStateException("Unable to deserialize server order record.")
                )

                if (serverOrder.userId != uid) {
                    return@withContext Result.failure(
                        SecurityException("Access denied: Order ownership verification failed.")
                    )
                }

                when (serverOrder.parsedStatus) {
                    OrderStatus.PENDING -> return@withContext Result.success(
                        DownloadVerificationResult.Locked(
                            status = OrderStatus.PENDING,
                            reason = "Payment is still awaiting admin review. Download remains locked."
                        )
                    )
                    OrderStatus.REJECTED -> return@withContext Result.success(
                        DownloadVerificationResult.Locked(
                            status = OrderStatus.REJECTED,
                            reason = serverOrder.adminNote?.takeIf { it.isNotBlank() }
                                ?: "Payment was rejected by admin. APK access is denied."
                        )
                    )
                    OrderStatus.APPROVED -> Unit
                }

                val reviewerUid = serverOrder.reviewedBy
                val storagePath = serverOrder.approvedStoragePath
                val fileName = serverOrder.approvedFileName
                if (reviewerUid.isNullOrBlank() || storagePath.isNullOrBlank() || fileName.isNullOrBlank()) {
                    return@withContext Result.success(
                        DownloadVerificationResult.Locked(
                            status = OrderStatus.PENDING,
                            reason = "Order approval metadata is incomplete."
                        )
                    )
                }

                val expectedPrefix = "apks/${serverOrder.productId}/"
                if (!storagePath.startsWith(expectedPrefix) || !fileName.lowercase().endsWith(".apk")) {
                    return@withContext Result.failure(
                        SecurityException("Invalid or unconfined APK storage path for product.")
                    )
                }

                // Verify server-side DownloadGrant document enforced by Firestore & Storage rules
                val grantPath = "products/${serverOrder.productId}/grants/$uid"
                val grantSnap = db.document(grantPath).get(Source.SERVER).await()
                if (!grantSnap.exists()) {
                    return@withContext Result.success(
                        DownloadVerificationResult.Locked(
                            status = OrderStatus.PENDING,
                            reason = "Download grant is missing. Ask admin to approve this order."
                        )
                    )
                }
                val grant = grantSnap.toObject(
                    DownloadGrant::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (grant == null ||
                    grant.userId != uid ||
                    grant.orderId != serverOrder.id ||
                    grant.status != OrderStatus.APPROVED.name ||
                    grant.storagePath != storagePath
                ) {
                    return@withContext Result.failure(
                        SecurityException("Download grant mismatch detected.")
                    )
                }

                // Confirm the real .apk binary exists in Firebase Storage
                val storageMetadata = storage.reference.child(storagePath).metadata.await()
                val remoteSize = storageMetadata.sizeBytes
                if (remoteSize <= 0L) {
                    return@withContext Result.failure(
                        IllegalStateException("Remote .apk file in Firebase Storage is empty.")
                    )
                }

                Result.success(
                    DownloadVerificationResult.Verified(
                        orderId = serverOrder.id,
                        productId = serverOrder.productId,
                        storagePath = storagePath,
                        fileName = fileName,
                        fileSizeBytes = serverOrder.approvedFileSizeBytes ?: remoteSize,
                        verifiedByAdminUid = reviewerUid
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: StorageException) {
                val friendly = when (e.errorCode) {
                    StorageException.ERROR_OBJECT_NOT_FOUND ->
                        "APK binary was not found in Firebase Storage."
                    StorageException.ERROR_BUCKET_NOT_FOUND,
                    StorageException.ERROR_NOT_AUTHORIZED ->
                        "Firebase Storage denied access or is not enabled for this project."
                    else -> e.localizedMessage ?: "Failed to verify APK binary in Firebase Storage."
                }
                Result.failure(IllegalStateException(friendly, e))
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, orderPath)
                Result.failure(e)
            }
        }

    /**
     * Runs verifyOrderDownloadAccess(orderId) first, then streams the real .apk binary
     * from Firebase Storage to Downloads or a user-selected SAF URI.
     */
    suspend fun downloadApprovedApk(
        context: Context,
        orderId: String,
        destinationUri: Uri?,
        onVerified: (DownloadVerificationResult.Verified) -> Unit = {},
        onProgress: (Long, Long) -> Unit
    ): Result<SavedApkResult> = withContext(Dispatchers.IO) {
        val orderPath = "orders/$orderId"
        var tempFile: File? = null
        try {
            val verification = verifyOrderDownloadAccess(orderId).getOrThrow()
            val grant = when (verification) {
                is DownloadVerificationResult.Locked -> {
                    return@withContext Result.failure(SecurityException(verification.reason))
                }
                is DownloadVerificationResult.Verified -> verification
            }
            onVerified(grant)

            val fileName = grant.fileName
            tempFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}_$fileName")
            val storageRef = storage.reference.child(grant.storagePath)
            val downloadTask = storageRef.getFile(tempFile)
            downloadTask.addOnProgressListener { snap ->
                onProgress(snap.bytesTransferred, snap.totalByteCount)
            }
            downloadTask.await()

            val bytesWritten = tempFile.length()
            if (bytesWritten <= 0L) {
                return@withContext Result.failure(IllegalStateException("Downloaded APK file was empty."))
            }

            if (destinationUri != null) {
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    tempFile.inputStream().use { input -> input.copyTo(out) }
                } ?: return@withContext Result.failure(
                    IllegalStateException("Could not open selected destination file.")
                )
                return@withContext Result.success(
                    SavedApkResult(
                        fileName = fileName,
                        bytesWritten = bytesWritten,
                        displayLocation = "Saved to chosen folder ($fileName)",
                        contentUri = destinationUri
                    )
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/PanelInjectorFF"
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Unable to create file in Downloads.")
                    )
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        tempFile.inputStream().use { input -> input.copyTo(out) }
                    } ?: throw IllegalStateException("Unable to write APK to Downloads.")

                    val finishedValues = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(itemUri, finishedValues, null, null)
                } catch (writeEx: Exception) {
                    runCatching { resolver.delete(itemUri, null, null) }
                    throw writeEx
                }

                Result.success(
                    SavedApkResult(
                        fileName = fileName,
                        bytesWritten = bytesWritten,
                        displayLocation = "Downloads/PanelInjectorFF/$fileName",
                        contentUri = itemUri
                    )
                )
            } else {
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)
                tempFile.copyTo(targetFile, overwrite = true)
                Result.success(
                    SavedApkResult(
                        fileName = fileName,
                        bytesWritten = bytesWritten,
                        displayLocation = targetFile.absolutePath,
                        contentUri = Uri.fromFile(targetFile)
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            val friendly = when (e.errorCode) {
                StorageException.ERROR_OBJECT_NOT_FOUND ->
                    "APK binary was not found in Firebase Storage."
                StorageException.ERROR_BUCKET_NOT_FOUND,
                StorageException.ERROR_NOT_AUTHORIZED ->
                    "Firebase Storage is not accessible. Verify Firebase Storage is enabled in Firebase Console."
                else -> e.localizedMessage ?: "Failed to download APK from Firebase Storage."
            }
            Result.failure(IllegalStateException(friendly, e))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, orderPath)
            Result.failure(e)
        } finally {
            runCatching { tempFile?.delete() }
        }
    }
}
