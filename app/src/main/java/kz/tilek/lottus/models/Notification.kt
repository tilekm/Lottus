package kz.tilek.lottus.models
import com.google.gson.annotations.SerializedName
data class Notification(
    @SerializedName("id") val id: String, 
    @SerializedName("user") val user: User, 
    @SerializedName("message") val message: String,
    @SerializedName("isRead") val isRead: Boolean, 
    @SerializedName("createdAt") val createdAt: String 
)
