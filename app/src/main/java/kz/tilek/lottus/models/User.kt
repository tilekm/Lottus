package kz.tilek.lottus.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String, // Было правильно
    @SerializedName("email") val email: String,       // Было правильно
    @SerializedName("createdAt") val createdAt: String, // Было правильно
    @SerializedName("updatedAt") val updatedAt: String, // Было правильно
    @SerializedName("rating") val rating: BigDecimal,   // Было правильно
    // ИСПРАВЛЕНО: 'verified' -> 'isVerified', чтобы соответствовать UserProfileDto
    @SerializedName("isVerified") val isVerified: Boolean,
    // ДОБАВЛЕНО: Новое поле для URL аватара
    @SerializedName("profilePictureUrl") val profilePictureUrl: String? // Nullable, т.к. может отсутствовать
)
