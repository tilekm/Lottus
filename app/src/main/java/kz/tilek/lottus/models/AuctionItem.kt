// ./app/src/main/java/kz/tilek/lottus/models/AuctionItem.kt
// Переименовали из AuctionItem.kt для ясности
package kz.tilek.lottus.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class AuctionItem(
    @SerializedName("id") val id: String, // UUID -> String
    @SerializedName("seller") val seller: User, // sellerId -> seller: User
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?, // Nullable
    @SerializedName("startPrice") val startPrice: BigDecimal, // startingPrice: Double -> startPrice: BigDecimal
    @SerializedName("buyNowPrice") val buyNowPrice: BigDecimal?, // Добавлено, Nullable
    @SerializedName("minBidStep") val minBidStep: BigDecimal, // Добавлено
    @SerializedName("startTime") val startTime: String, // Добавлено, Instant -> String
    @SerializedName("endTime") val endTime: String, // endTime: Long -> endTime: String
    @SerializedName("status") val status: String, // Добавлено
    @SerializedName("createdAt") val createdAt: String, // Добавлено, Instant -> String
    @SerializedName("updatedAt") val updatedAt: String, // Добавлено, Instant -> String

    // currentBid удален, будет загружаться отдельно
    // imageUrl удален, т.к. нет в Item.java (можно добавить, если API будет возвращать)
)
