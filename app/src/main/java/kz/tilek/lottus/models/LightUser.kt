package kz.tilek.lottus.models
import com.google.gson.annotations.SerializedName
data class LightUser(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String
)
