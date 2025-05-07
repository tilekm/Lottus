package kz.tilek.lottus.repositories

import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.CreateAuctionRequest
import kz.tilek.lottus.api.PageResponse
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.util.parseError

class ItemRepository {
    private val apiService = ApiClient.instance

    /**
     * Получает страницу с аукционами для главной страницы.
     * @param page Номер страницы (0-indexed).
     * @param size Количество элементов на странице.
     * @param viewType Тип фильтра ("ALL", "ACTIVE", "SCHEDULED").
     * @param searchTerm Поисковый запрос (может быть null или пустым).
     * @param sort Параметр сортировки (например, "createdAt,desc").
     */
    suspend fun getItems(
        page: Int,
        size: Int,
        viewType: String,
        searchTerm: String?,
        sort: String? = "createdAt,desc"
    ): Result<PageResponse<AuctionItem>> {
        return try {
            val response = apiService.getItems(page, size, viewType, searchTerm, sort)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получена страница с лотами от сервера."))
            } else {
                Result.failure(Exception("Ошибка получения страницы лотов: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получает страницу с аукционами текущего пользователя для страницы "Мои аукционы".
     * @param page Номер страницы (0-indexed).
     * @param size Количество элементов на странице.
     * @param filterType Тип фильтра ("PARTICIPATING", "WON", "CREATED").
     * @param searchTerm Поисковый запрос (может быть null или пустым).
     * @param sort Параметр сортировки (например, "createdAt,desc").
     */
    suspend fun getMyItems(
        page: Int,
        size: Int,
        filterType: String, // Строковое значение Enum MyAuctionFilterType
        searchTerm: String?,
        sort: String? = "createdAt,desc" // Можно настроить другую сортировку по умолчанию
    ): Result<PageResponse<AuctionItem>> {
        return try {
            // Вызываем новый метод из ApiService
            val response = apiService.getMyItems(page, size, filterType, searchTerm, sort)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получена страница 'Мои аукционы' от сервера."))
            } else {
                Result.failure(Exception("Ошибка получения страницы 'Мои аукционы': ${parseError(response)}"))
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

    suspend fun createItem(request: CreateAuctionRequest): Result<AuctionItem> {
        return try {
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
