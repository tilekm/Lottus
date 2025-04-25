// ./app/src/main/java/kz/tilek/lottus/viewmodels/CreateAuctionViewModel.kt
package kz.tilek.lottus.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.api.CreateAuctionRequest
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.repositories.ItemRepository
import java.math.BigDecimal

class CreateAuctionViewModel : ViewModel() {

    private val itemRepository = ItemRepository()

    // LiveData для результата создания аукциона
    private val _createAuctionState = MutableLiveData<Result<AuctionItem>>()
    val createAuctionState: LiveData<Result<AuctionItem>> = _createAuctionState

    // LiveData для индикатора загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Создает новый аукцион.
     */
    fun createAuction(
        sellerId: String,
        title: String,
        description: String?,
        startPrice: BigDecimal,
        buyNowPrice: BigDecimal?,
        minBidStep: BigDecimal,
        startTime: String, // Передаем как String (ISO 8601)
        endTime: String    // Передаем как String (ISO 8601)
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = CreateAuctionRequest(
                sellerId = sellerId,
                title = title,
                description = description,
                startPrice = startPrice,
                buyNowPrice = buyNowPrice,
                minBidStep = minBidStep,
                startTime = startTime,
                endTime = endTime
            )
            val result = itemRepository.createItem(request)
            _createAuctionState.value = result
            _isLoading.value = false
        }
    }
}
