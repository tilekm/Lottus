// ./app/src/main/java/kz/tilek/lottus/repositories/UserRepository.kt
package kz.tilek.lottus.repositories

import android.util.Log
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.UserProfileUpdateRequest // Импорт DTO
import kz.tilek.lottus.models.User
import kz.tilek.lottus.util.parseError

class UserRepository {
    private val apiService = ApiClient.instance

    /**
     * Получает профиль ТЕКУЩЕГО аутентифицированного пользователя через /me.
     */
    suspend fun getCurrentUserProfile(): Result<User> {
        // ... (код без изменений)
        return try {
            val response = apiService.getCurrentUserProfile()
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получены данные текущего пользователя от сервера."))
            } else {
                Result.failure(Exception("Ошибка получения данных пользователя: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Обновляет профиль ТЕКУЩЕГО аутентифицированного пользователя через /me.
     */
    suspend fun updateUserProfile(updateRequest: UserProfileUpdateRequest): Result<User> {
        return try {
            val response = apiService.updateCurrentUserProfile(updateRequest)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получены обновленные данные пользователя от сервера."))
            } else {
                // Обрабатываем ошибки валидации или конфликта (например, username занят)
                Result.failure(Exception("Ошибка обновления профиля: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получает ПУБЛИЧНЫЙ профиль пользователя по ID.
     */
    suspend fun getUserProfile(userId: String): Result<User> {
        // ... (код без изменений)
        return try {
            val response = apiService.getUserProfile(userId)
            Log.i("UserRepository", "Response for ID $userId: $response")
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получены данные пользователя (ID: $userId) от сервера."))
            } else {
                Result.failure(Exception("Ошибка получения профиля (ID: $userId): ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получает ПУБЛИЧНЫЙ профиль пользователя по имени пользователя.
     */
    suspend fun getUserByUsername(username: String): Result<User> {
        // ... (код без изменений)
        return try {
            val response = apiService.getUserByUsername(username)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получены данные пользователя (username: $username) от сервера."))
            } else {
                Result.failure(Exception("Ошибка получения пользователя (username: $username): ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
