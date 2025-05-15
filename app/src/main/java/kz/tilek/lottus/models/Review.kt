package kz.tilek.lottus.models
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
data class Review(
    @SerializedName("id") val id: String,
    @SerializedName("reviewer") val reviewer: LightUser,
    @SerializedName("reviewedUser") val reviewedUser: LightUser,
    @SerializedName("rating") val rating: BigDecimal,
    @SerializedName("comment") val comment: String?, 
    @SerializedName("createdAt") val createdAt: String 
)
