// ./app/src/main/java/kz/tilek/lottus/models/Bid.kt
// Новый файл
package kz.tilek.lottus.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class Bid(
    @SerializedName("id") val id: String, // UUID -> String
    @SerializedName("item") val item: AuctionItem, // Связь с лотом
    @SerializedName("bidder") val bidder: User, // Связь с пользователем
    @SerializedName("bidAmount") val bidAmount: BigDecimal,
    @SerializedName("createdAt") val createdAt: String // Instant -> String
)
