package kz.tilek.lottus.repositories
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.PlaceBidRequest
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.util.parseError
class BidRepository {
    private val apiService = ApiClient.instance
    suspend fun placeBid(request: PlaceBidRequest): Result<Bid> {
        return try {
            val response = apiService.placeBid(request)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Не получен ответ о ставке от сервера."))
            } else {
                Result.failure(Exception("Ошибка размещения ставки: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getBidsForItem(itemId: String): Result<List<Bid>> {
        return try {
            val response = apiService.getBidsForItem(itemId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка получения ставок для лота: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getHighestBidForItem(itemId: String): Result<Bid?> {
        return try {
            val response = apiService.getHighestBidForItem(itemId)
            if (response.isSuccessful) {
                Result.success(response.body())
            } else if (response.code() == 204) {
                Result.success(null)
            }
            else {
                Result.failure(Exception("Ошибка получения максимальной ставки: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getUserBidHistory(userId: String): Result<List<Bid>> {
        return try {
            val response = apiService.getUserBidHistory(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Ошибка получения истории ставок: ${parseError(response)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
