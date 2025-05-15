package kz.tilek.lottus.models
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
data class AuctionItem(
    @SerializedName("id") val id: String,
    @SerializedName("seller") val seller: User,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("startPrice") val startPrice: BigDecimal,
    @SerializedName("buyNowPrice") val buyNowPrice: BigDecimal?,
    @SerializedName("minBidStep") val minBidStep: BigDecimal,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("imageUrls") val imageUrls: List<String>?
)
