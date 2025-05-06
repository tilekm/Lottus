// ./app/src/main/java/kz/tilek/lottus/models/Review.kt
package kz.tilek.lottus.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class Review(
    @SerializedName("id") val id: String,
    // Используем LightUser для краткой информации
    @SerializedName("reviewer") val reviewer: LightUser,
    @SerializedName("reviewedUser") val reviewedUser: LightUser,
    @SerializedName("rating") val rating: BigDecimal,
    @SerializedName("comment") val comment: String?, // Комментарий может быть null
    @SerializedName("createdAt") val createdAt: String // Instant -> String
)
