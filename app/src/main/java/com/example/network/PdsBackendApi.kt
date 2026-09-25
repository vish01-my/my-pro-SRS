package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Network DTOs for Idempotent & Atomic Transaction Reconciliation
// ---------------------------------------------------------------------------

data class ReconcileTransactionItemDto(
    val transaction_id: String,
    val idempotency_key: String,
    val invoice_number: String,
    val card_id: String,
    val beneficiary_name: String,
    val fps_id: String,
    val period_mode: String,
    val period_description: String,
    val total_quantity_kg: Double,
    val total_amount_paid: Double,
    val auth_method_used: String,
    val digital_bill_hash: String,
    val verification_token_signature: String,
    val created_at_timestamp: Long,
    val items_json: String? = null
)

data class ReconcileBatchRequestDto(
    val dealer_id: String,
    val pos_device_id: String,
    val batch_id: String,
    val transactions: List<ReconcileTransactionItemDto>
)

data class TransactionReconcileResultDto(
    val transaction_id: String,
    val idempotency_key: String,
    val status: String, // "RECONCILED", "DUPLICATE_IDEMPOTENT_IGNORED", "CONFLICT_FLAGGED"
    val message: String,
    val synced_timestamp: Long
)

data class ReconcileBatchResponseDto(
    val batch_id: String,
    val total_received: Int,
    val reconciled_count: Int,
    val duplicate_count: Int,
    val conflict_count: Int,
    val results: List<TransactionReconcileResultDto>,
    val audit_event_id: String,
    val atomicity_guarantee: String = "Batch processed in single transaction isolation"
)

interface PdsBackendApi {
    @POST("api/v1/transactions/reconcile")
    suspend fun reconcileTransactions(
        @Header("Authorization") authHeader: String? = null,
        @Body request: ReconcileBatchRequestDto
    ): Response<ReconcileBatchResponseDto>
}

object PdsNetworkClient {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/" // Android Emulator loopback to host

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: PdsBackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PdsBackendApi::class.java)
    }
}
