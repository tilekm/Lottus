// ./app/src/main/java/kz/tilek/lottus/repositories/ItemRepository.kt
package kz.tilek.lottus.repositories

import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.CreateAuctionRequest // <-- Убедись, что импорт правильный
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.util.parseError

class ItemRepository {
    private val apiService = ApiClient.instance

    // getActiveItems, getAllItems, getItemDetails, getItemsBySeller - без изменений
    suspend fun getActiveItems(): Result<List<AuctionItem>> {
        return try {
            val response = apiService.getActiveItems()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка получения активных лотов: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllItems(): Result<List<AuctionItem>> {
        return try {
            val response = apiService.getAllItems()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка получения всех лотов: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemDetails(itemId: String): Result<AuctionItem> {
        return try {
            val response = apiService.getItemDetails(itemId)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получены детали лота от сервера."))
            } else {
                Result.failure(Exception("Ошибка получения деталей лота: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemsBySeller(sellerId: String): Result<List<AuctionItem>> {
        return try {
            val response = apiService.getItemsBySeller(sellerId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка получения лотов продавца: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Создает новый аукцион.
     * Принимает готовый CreateAuctionRequest.
     */
    suspend fun createItem(request: CreateAuctionRequest): Result<AuctionItem> { // <-- Принимаем request
        return try {
            // Просто передаем request в ApiService
            val response = apiService.createItem(request)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получен созданный лот от сервера."))
            } else {
                Result.failure(Exception("Ошибка создания лота: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buyNowForItem(itemId: String): Result<Bid> {
        return try {
            val response = apiService.buyNowForItem(itemId)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получен ответ о покупке от сервера."))
            } else {
                Result.failure(Exception("Ошибка покупки лота: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
