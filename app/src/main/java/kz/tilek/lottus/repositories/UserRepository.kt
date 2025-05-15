package kz.tilek.lottus.repositories
import android.util.Log
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.UserProfileUpdateRequest 
import kz.tilek.lottus.models.User
import kz.tilek.lottus.util.parseError
class UserRepository {
    private val apiService = ApiClient.instance
    suspend fun getCurrentUserProfile(): Result<User> {
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
    suspend fun updateUserProfile(updateRequest: UserProfileUpdateRequest): Result<User> {
        return try {
            val response = apiService.updateCurrentUserProfile(updateRequest)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получены обновленные данные пользователя от сервера."))
            } else {
                Result.failure(Exception("Ошибка обновления профиля: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getUserProfile(userId: String): Result<User> {
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
    suspend fun getUserByUsername(username: String): Result<User> {
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
