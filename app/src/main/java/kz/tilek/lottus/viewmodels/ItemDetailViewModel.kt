// ./app/src/main/java/kz/tilek/lottus/viewmodels/ItemDetailViewModel.kt
package kz.tilek.lottus.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.repositories.BidRepository
import kz.tilek.lottus.repositories.ItemRepository

data class ItemDetailData(
    val item: AuctionItem,
    val bids: List<Bid>,
    val highestBid: Bid? // Добавим отдельно для удобства
)

class ItemDetailViewModel : ViewModel() {

    private val itemRepository = ItemRepository()
    private val bidRepository = BidRepository()

    // LiveData для хранения объединенных данных (лот + ставки)
    private val _itemDetailState = MutableLiveData<Result<ItemDetailData>>()
    val itemDetailState: LiveData<Result<ItemDetailData>> = _itemDetailState

    // LiveData для состояния размещения ставки
    private val _placeBidState = MutableLiveData<Result<Bid>>()
    val placeBidState: LiveData<Result<Bid>> = _placeBidState

    // LiveData для индикатора общей загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData для индикатора загрузки при размещении ставки
    private val _isPlacingBid = MutableLiveData<Boolean>()
    val isPlacingBid: LiveData<Boolean> = _isPlacingBid

    /**
     * Загружает детали лота и историю ставок одновременно.
     */
    fun loadItemDetails(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Запускаем запросы параллельно
                val itemDeferred = async { itemRepository.getItemDetails(itemId) }
                val bidsDeferred = async { bidRepository.getBidsForItem(itemId) }
                val highestBidDeferred = async { bidRepository.getHighestBidForItem(itemId) }

                // Ожидаем результаты
                val itemResult = itemDeferred.await()
                val bidsResult = bidsDeferred.await()
                val highestBidResult = highestBidDeferred.await()

                // Проверяем, что все запросы успешны
                if (itemResult.isSuccess && bidsResult.isSuccess && highestBidResult.isSuccess) {
                    val itemData = ItemDetailData(
                        item = itemResult.getOrThrow(),
                        bids = bidsResult.getOrThrow(),
                        highestBid = highestBidResult.getOrThrow() // Может быть null
                    )
                    _itemDetailState.value = Result.success(itemData)
                } else {
                    // Если хотя бы один запрос не удался, возвращаем первую ошибку
                    val error = itemResult.exceptionOrNull()
                        ?: bidsResult.exceptionOrNull()
                        ?: highestBidResult.exceptionOrNull()
                        ?: Exception("Неизвестная ошибка загрузки деталей лота")
                    _itemDetailState.value = Result.failure(error)
                }

            } catch (e: Exception) {
                _itemDetailState.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Размещает ставку.
     */
    fun placeBid(request: kz.tilek.lottus.api.PlaceBidRequest) { // Используем полное имя, чтобы избежать конфликта
        viewModelScope.launch {
            _isPlacingBid.value = true
            val result = bidRepository.placeBid(request)
            _placeBidState.value = result // Уведомляем UI о результате ставки

            // Если ставка успешна, перезагружаем данные, чтобы обновить список ставок
            if (result.isSuccess) {
                loadItemDetails(request.itemId)
            }
            _isPlacingBid.value = false
        }
    }
}
