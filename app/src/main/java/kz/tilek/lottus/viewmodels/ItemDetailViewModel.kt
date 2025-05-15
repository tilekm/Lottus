package kz.tilek.lottus.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kz.tilek.lottus.api.PlaceBidRequest 
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.repositories.BidRepository
import kz.tilek.lottus.repositories.ItemRepository
data class ItemDetailData(
    val item: AuctionItem,
    val bids: List<Bid>,
    val highestBid: Bid?
)
class ItemDetailViewModel : ViewModel() {
    private val itemRepository = ItemRepository()
    private val bidRepository = BidRepository()
    private val _itemDetailState = MutableLiveData<Result<ItemDetailData>>()
    val itemDetailState: LiveData<Result<ItemDetailData>> = _itemDetailState
    private val _placeBidState = MutableLiveData<Result<Bid>>()
    val placeBidState: LiveData<Result<Bid>> = _placeBidState
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _isPlacingBid = MutableLiveData<Boolean>()
    val isPlacingBid: LiveData<Boolean> = _isPlacingBid
    private val _buyNowState = MutableLiveData<Result<Bid>>()
    val buyNowState: LiveData<Result<Bid>> = _buyNowState
    private val _isBuyingNow = MutableLiveData<Boolean>()
    val isBuyingNow: LiveData<Boolean> = _isBuyingNow
    fun loadItemDetails(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val itemDeferred = async { itemRepository.getItemDetails(itemId) }
                val bidsDeferred = async { bidRepository.getBidsForItem(itemId) }
                val itemResult = itemDeferred.await()
                val bidsResult = bidsDeferred.await()
                if (itemResult.isSuccess && bidsResult.isSuccess) {
                    val bidsList = bidsResult.getOrThrow()
                    val highestBid = bidsList.firstOrNull() 
                    val itemData = ItemDetailData(
                        item = itemResult.getOrThrow(),
                        bids = bidsList,
                        highestBid = highestBid
                    )
                    _itemDetailState.value = Result.success(itemData)
                } else {
                    val error = itemResult.exceptionOrNull()
                        ?: bidsResult.exceptionOrNull()
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
    fun placeBid(request: PlaceBidRequest) {
        viewModelScope.launch {
            _isPlacingBid.value = true
            val result = bidRepository.placeBid(request)
            _placeBidState.value = result
            if (result.isSuccess) {
                loadItemDetails(request.itemId)
            }
            _isPlacingBid.value = false
        }
    }
    fun executeBuyNow(itemId: String) {
        viewModelScope.launch {
            _isBuyingNow.value = true
            val result = itemRepository.buyNowForItem(itemId)
            _buyNowState.value = result
            if (result.isSuccess) {
                loadItemDetails(itemId) 
            }
            _isBuyingNow.value = false
        }
    }
}
