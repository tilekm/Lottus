// ./app/src/main/java/kz/tilek/lottus/models/LightUser.kt
package kz.tilek.lottus.models

import com.google.gson.annotations.SerializedName

data class LightUser(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String
)
