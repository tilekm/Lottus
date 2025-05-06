// ./app/src/main/java/kz/tilek/lottus/repositories/ReviewRepository.kt
package kz.tilek.lottus.repositories

import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.ReviewCreateRequest // Импорт DTO
import kz.tilek.lottus.models.Review
import kz.tilek.lottus.util.parseError

class ReviewRepository {
    private val apiService = ApiClient.instance

    /**
     * Получает список отзывов о пользователе по его ID.
     */
    suspend fun getReviewsForUser(userId: String): Result<List<Review>> {
        return try {
            val response = apiService.getReviewsForUser(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка получения отзывов: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Создает новый отзыв.
     * (Понадобится на следующем шаге)
     */
    suspend fun createReview(request: ReviewCreateRequest): Result<Review> {
        return try {
            val response = apiService.createReview(request)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получен созданный отзыв от сервера."))
            } else {
                Result.failure(Exception("Ошибка создания отзыва: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
