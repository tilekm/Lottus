// ./app/src/main/java/kz/tilek/lottus/models/Notification.kt
// Новый файл
package kz.tilek.lottus.models

import com.google.gson.annotations.SerializedName

data class Notification(
    @SerializedName("id") val id: String, // UUID -> String
    @SerializedName("user") val user: User, // Связь с пользователем
    @SerializedName("message") val message: String,
    @SerializedName("read") val isRead: Boolean, // isRead -> read (соответствует JSON)
    @SerializedName("createdAt") val createdAt: String // Instant -> String
)
