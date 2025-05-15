package kz.tilek.lottus.models
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
data class Bid(
    @SerializedName("id") val id: String, 
    @SerializedName("item") val item: AuctionItem, 
    @SerializedName("bidder") val bidder: User, 
    @SerializedName("bidAmount") val bidAmount: BigDecimal,
    @SerializedName("createdAt") val createdAt: String 
)
