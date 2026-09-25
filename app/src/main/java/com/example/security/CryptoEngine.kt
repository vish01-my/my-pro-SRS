package com.example.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class ParsedCardPayload(
    val version: String,
    val cardId: String,
    val nonce: String,
    val timestamp: Long,
    val signature: String,
    val rawPayload: String
)

sealed class TokenValidationResult {
    data class Success(val parsed: ParsedCardPayload) : TokenValidationResult()
    data class Failure(val errorCode: String, val message: String) : TokenValidationResult()
}

object CryptoEngine {
    // Government Master Signing Key for Prototype (In production, stored in HSM / Android Keystore)
    private const val MASTER_PDS_HMAC_SECRET = "GOVT_INDIA_PDS_SECURE_HMAC_KEY_2026_NFSA_SALT"

    fun generateSignedPayload(cardId: String, nonce: String, timestamp: Long): String {
        val dataToSign = "v1:$cardId:$nonce:$timestamp"
        val signature = computeHmacSha256(dataToSign, MASTER_PDS_HMAC_SECRET)
        return "SRC:v1:$cardId:$nonce:$timestamp:$signature"
    }

    fun parseAndValidatePayload(payload: String): TokenValidationResult {
        if (!payload.startsWith("SRC:")) {
            return TokenValidationResult.Failure(
                "MALFORMED_TOKEN",
                "Card token header does not match Government PDS Smart Card format"
            )
        }

        val parts = payload.split(":")
        // Format: SRC:version:cardId:nonce:timestamp:signature
        if (parts.size < 6) {
            return TokenValidationResult.Failure(
                "INVALID_TOKEN_STRUCTURE",
                "Incomplete cryptographic token payload"
            )
        }

        val version = parts[1]
        val cardId = parts[2]
        val nonce = parts[3]
        val timestamp = parts[4].toLongOrNull() ?: 0L
        val signature = parts[5]

        // 1. Verify Digital Signature
        val expectedDataToSign = "v1:$cardId:$nonce:$timestamp"
        val expectedSignature = computeHmacSha256(expectedDataToSign, MASTER_PDS_HMAC_SECRET)

        if (!signature.equals(expectedSignature, ignoreCase = true)) {
            return TokenValidationResult.Failure(
                "INVALID_SIGNATURE",
                "Cryptographic signature verification failed! Possible counterfeit or tampered card."
            )
        }

        return TokenValidationResult.Success(
            ParsedCardPayload(
                version = version,
                cardId = cardId,
                nonce = nonce,
                timestamp = timestamp,
                signature = signature,
                rawPayload = payload
            )
        )
    }

    fun computeHmacSha256(data: String, key: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        sha256Hmac.init(secretKey)
        val signedBytes = sha256Hmac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(signedBytes).take(24) // 24-char hex compact token signature
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(hash)
    }

    fun generateIdempotencyKey(cardId: String, dealerId: String, period: String, timestamp: Long): String {
        return "IDEMP-" + sha256("$cardId:$dealerId:$period:${timestamp / 30000}").take(16).uppercase()
    }

    fun calculateAuditHash(prevHash: String, eventId: String, timestamp: Long, dealerId: String, action: String, result: String): String {
        return sha256("$prevHash|$eventId|$timestamp|$dealerId|$action|$result")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt()
            result.append(hexChars[(i shr 4) and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }
}
