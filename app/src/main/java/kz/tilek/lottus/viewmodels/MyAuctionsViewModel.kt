package kz.tilek.lottus.viewmodels
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.tilek.lottus.data.TokenManager 
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.repositories.ItemRepository
class MyAuctionsViewModel(application: Application) : AndroidViewModel(application) {
    private val itemRepository = ItemRepository()
    private val _myAuctionsList = MutableLiveData<List<AuctionItem>>(emptyList())
    val myAuctionsList: LiveData<List<AuctionItem>> = _myAuctionsList
    private val _loadState = MutableLiveData<LoadState>(LoadState.Idle)
    val loadState: LiveData<LoadState> = _loadState
    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    private var currentPage = 0
    private var isLastPage = false
    private val pageSize = 20
    private val _currentFilterType = MutableLiveData(MyAuctionFilterType.PARTICIPATING)
    val currentFilterType: LiveData<MyAuctionFilterType> = _currentFilterType
    private val _currentSearchTerm = MutableLiveData<String?>(null)
    val currentSearchTerm: LiveData<String?> = _currentSearchTerm
    private var fetchJob: Job? = null
    private var searchDebounceJob: Job? = null
    private val searchDelayMillis = 500L
    init {
        _currentFilterType.value?.let { filter ->
            loadMyAuctions(isRefresh = true, filter = filter, searchTerm = _currentSearchTerm.value)
        }
    }
    fun setFilter(filterType: MyAuctionFilterType) {
        val oldFilter = _currentFilterType.value
        val oldSearchTerm = _currentSearchTerm.value
        _currentFilterType.value = filterType
        _currentSearchTerm.value = null 
        if (oldFilter != filterType || oldSearchTerm != null) {
            searchDebounceJob?.cancel()
            loadMyAuctions(isRefresh = true, filter = filterType, searchTerm = null)
        } else if (_myAuctionsList.value.isNullOrEmpty() && _loadState.value != LoadState.Loading) {
            loadMyAuctions(isRefresh = true, filter = filterType, searchTerm = null)
        }
    }
    fun setSearchTerm(searchTerm: String?) {
        val newSearchTerm = searchTerm?.trim()?.ifEmpty { null }
        if (_currentSearchTerm.value == newSearchTerm) {
            return 
        }
        _currentSearchTerm.value = newSearchTerm
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(searchDelayMillis)
            _currentFilterType.value?.let { filter ->
                loadMyAuctions(isRefresh = true, filter = filter, searchTerm = newSearchTerm)
            }
        }
    }
    fun performSearchNow(searchTerm: String?) {
        searchDebounceJob?.cancel()
        val newSearchTerm = searchTerm?.trim()?.ifEmpty { null }
        _currentSearchTerm.value = newSearchTerm 
        _currentFilterType.value?.let { filter ->
            loadMyAuctions(isRefresh = true, filter = filter, searchTerm = newSearchTerm)
        }
    }
    fun loadMyAuctions(
        isRefresh: Boolean = false,
        filter: MyAuctionFilterType = _currentFilterType.value ?: MyAuctionFilterType.PARTICIPATING,
        searchTerm: String? = _currentSearchTerm.value
    ) {
        fetchJob?.cancel()
        if (isRefresh) {
            currentPage = 0
            isLastPage = false
            _myAuctionsList.value = emptyList()
            _loadState.value = LoadState.Loading
        } else {
            if (_isLoadingMore.value == true || isLastPage) return
            _isLoadingMore.value = true
        }
        fetchJob = viewModelScope.launch {
            val result = itemRepository.getMyItems(
                page = currentPage,
                size = pageSize,
                filterType = filter.value, 
                searchTerm = searchTerm
            )
            if (!isRefresh) {
                _isLoadingMore.value = false
            }
            result.onSuccess { pageResponse ->
                val newItems = pageResponse.content
                if (isRefresh) {
                    _myAuctionsList.value = newItems
                } else {
                    _myAuctionsList.value = (_myAuctionsList.value ?: emptyList()) + newItems
                }
                isLastPage = pageResponse.last
                if (!isLastPage && newItems.isNotEmpty()) {
                    currentPage++
                }
                if (isRefresh) {
                    _loadState.value = if (_myAuctionsList.value.isNullOrEmpty()) LoadState.Empty else LoadState.Success
                } else {
                    if (newItems.isNotEmpty() && _loadState.value !is LoadState.Error) {
                        _loadState.value = LoadState.Success
                    }
                }
            }.onFailure { exception ->
                Log.e("MyAuctionsVM", "Ошибка загрузки 'Мои аукционы': ${exception.message}", exception)
                if (isRefresh) {
                    _loadState.value = LoadState.Error(exception.message ?: "Неизвестная ошибка")
                }
            }
        }
    }
    sealed class LoadState {
        object Idle : LoadState()
        object Loading : LoadState()
        object Success : LoadState()
        object Empty : LoadState()
        data class Error(val message: String) : LoadState()
    }
    fun getCurrentFilterValue(): MyAuctionFilterType = _currentFilterType.value ?: MyAuctionFilterType.PARTICIPATING
    fun getCurrentSearchTermValue(): String? = _currentSearchTerm.value
}
