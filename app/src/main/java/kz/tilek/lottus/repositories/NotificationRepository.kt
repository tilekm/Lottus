// ./app/src/main/java/kz/tilek/lottus/repositories/NotificationRepository.kt
package kz.tilek.lottus.repositories

import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.models.Notification
import kz.tilek.lottus.util.parseError

class NotificationRepository {
    private val apiService = ApiClient.instance

    /**
     * Получает все уведомления пользователя.
     */
    suspend fun getAllNotifications(userId: String): Result<List<Notification>> {
        return try {
            val response = apiService.getAllNotifications(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка получения уведомлений: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получает непрочитанные уведомления пользователя.
     */
    suspend fun getUnreadNotifications(userId: String): Result<List<Notification>> {
        return try {
            val response = apiService.getUnreadNotifications(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка получения непрочитанных уведомлений: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Помечает уведомление как прочитанное.
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            val response = apiService.markNotificationAsRead(notificationId)
            if (response.isSuccessful) {
                Result.success(Unit) // Успех без тела ответа
            } else {
                Result.failure(Exception("Ошибка пометки уведомления как прочитанного: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
