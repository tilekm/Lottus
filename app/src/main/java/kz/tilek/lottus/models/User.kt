package kz.tilek.lottus.models
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
data class User(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String, 
    @SerializedName("email") val email: String,       
    @SerializedName("createdAt") val createdAt: String, 
    @SerializedName("updatedAt") val updatedAt: String, 
    @SerializedName("rating") val rating: BigDecimal,   
    @SerializedName("isVerified") val isVerified: Boolean,
    @SerializedName("profilePictureUrl") val profilePictureUrl: String? 
)
