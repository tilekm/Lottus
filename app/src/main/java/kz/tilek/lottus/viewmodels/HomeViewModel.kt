package kz.tilek.lottus.viewmodels
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.repositories.ItemRepository
object AuctionFilterType {
    const val ALL = "ALL"
    const val ACTIVE = "ACTIVE"
    const val SCHEDULED = "SCHEDULED"
}
class HomeViewModel : ViewModel() {
    private val itemRepository = ItemRepository()
    private val _itemsList = MutableLiveData<List<AuctionItem>>(emptyList()) 
    val itemsList: LiveData<List<AuctionItem>> = _itemsList 
    private val _loadState = MutableLiveData<LoadState>(LoadState.Idle)
    val loadState: LiveData<LoadState> = _loadState
    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    private var currentPage = 0
    private var isLastPage = false
    private var currentFilterType = AuctionFilterType.ACTIVE
    private var currentSearchTerm: String? = null
    private val pageSize = 20
    private var searchJob: Job? = null
    private val searchDelayMillis = 500L
    init {
        loadItems(isRefresh = true)
    }
    fun loadItems(isRefresh: Boolean = false) {
        if (isRefresh) {
            currentPage = 0
            isLastPage = false
            _itemsList.value = emptyList()      
            _loadState.value = LoadState.Loading 
        } else { 
            if (_isLoadingMore.value == true || isLastPage) {
                if (!isLastPage) _isLoadingMore.value = false 
                return
            }
            _isLoadingMore.value = true
        }
        viewModelScope.launch {
            val result = itemRepository.getItems(
                page = currentPage,
                size = pageSize,
                viewType = currentFilterType,
                searchTerm = currentSearchTerm
            )
            if (!isRefresh) {
                _isLoadingMore.value = false
            }
            result.onSuccess { pageResponse ->
                val newItems = pageResponse.content
                if (isRefresh) {
                    _itemsList.value = newItems 
                } else {
                    _itemsList.value = (_itemsList.value ?: emptyList()) + newItems
                }
                isLastPage = pageResponse.last
                if (!isLastPage && newItems.isNotEmpty()) {
                    currentPage++
                }
                if (isRefresh) {
                    _loadState.value = if ((_itemsList.value ?: emptyList()).isEmpty()) LoadState.Empty else LoadState.Success
                } else {
                    if (newItems.isNotEmpty() && _loadState.value !is LoadState.Error) {
                        _loadState.value = LoadState.Success
                    }
                }
            }.onFailure { exception ->
                if (isRefresh) {
                    _loadState.value = LoadState.Error(exception.message ?: "Неизвестная ошибка")
                } else {
                    Log.e("HomeViewModel", "Ошибка пагинации: ${exception.message}")
                }
            }
        }
    }
    fun setFilter(filterType: String) {
        val oldFilter = currentFilterType
        val oldSearchTerm = currentSearchTerm 
        currentFilterType = filterType
        currentSearchTerm = null 
        if (oldFilter != currentFilterType || oldSearchTerm != null) {
            searchJob?.cancel() 
            loadItems(isRefresh = true)
        }
    }
    fun setSearchTerm(searchTerm: String?) {
        val newSearchTerm = searchTerm?.trim()?.ifEmpty { null }
        if (currentSearchTerm != newSearchTerm) {
            currentSearchTerm = newSearchTerm
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(searchDelayMillis)
                loadItems(isRefresh = true)
            }
        } else if (newSearchTerm == null && currentSearchTerm == null && searchJob?.isActive != true && _loadState.value != LoadState.Loading) {
            loadItems(isRefresh = true)
        }
    }
    fun performSearchNow(searchTerm: String?) {
        searchJob?.cancel()
        currentSearchTerm = searchTerm?.trim()?.ifEmpty { null }
        loadItems(isRefresh = true)
    }
    fun getCurrentFilter(): String = currentFilterType
    fun getCurrentSearchTerm(): String? = currentSearchTerm
    sealed class LoadState {
        object Idle : LoadState()
        object Loading : LoadState()
        object Success : LoadState()
        object Empty : LoadState() 
        data class Error(val message: String) : LoadState() 
    }
}
