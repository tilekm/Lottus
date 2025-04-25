package kz.tilek.lottus.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class User(
    @SerializedName("id") val id: String, // UUID -> String
    @SerializedName("username") val username: String, // name -> username
    @SerializedName("email") val email: String,
    @SerializedName("createdAt") val createdAt: String, // Instant -> String (ISO 8601)
    @SerializedName("updatedAt") val updatedAt: String, // Instant -> String (ISO 8601)
    @SerializedName("rating") val rating: BigDecimal,
    @SerializedName("verified") val isVerified: Boolean // isVerified -> verified (соответствует JSON)
    // passwordHash не нужен на клиенте
    // token удален из модели User
)
