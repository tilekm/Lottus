// ./app/src/main/java/kz/tilek/lottus/repositories/UserRepository.kt
package kz.tilek.lottus.repositories

import android.util.Log
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.models.User
import kz.tilek.lottus.util.parseError // Используем наш парсер ошибок

class UserRepository {
    // Используем ленивый instance из ApiClient
    private val apiService = ApiClient.instance

    /**
     * Получает профиль пользователя по ID.
     */
    suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val response = apiService.getUserProfile(userId)
            Log.i("UserRepository", "Response: $response")
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получены данные пользователя от сервера."))
            } else {
                Result.failure(Exception("Ошибка получения профиля: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получает пользователя по имени пользователя.
     */
    suspend fun getUserByUsername(username: String): Result<User> {
        return try {
            val response = apiService.getUserByUsername(username)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получены данные пользователя от сервера."))
            } else {
                Result.failure(Exception("Ошибка получения пользователя: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
