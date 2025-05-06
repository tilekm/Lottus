// ./app/src/main/java/kz/tilek/lottus/viewmodels/HomeViewModel.kt
// ИЗМЕНЕННЫЙ ФАЙЛ (фрагмент onSuccess в методе loadItems)
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

    private val _itemsList = MutableLiveData<List<AuctionItem>>(emptyList()) // Инициализируем пустым списком
    val itemsList: LiveData<List<AuctionItem>> = _itemsList // Убрал явное приведение, LiveData<List<AuctionItem>> достаточно

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
            _itemsList.value = emptyList()      // НЕМЕДЛЕННО очищаем список для UI реакции
            _loadState.value = LoadState.Loading // Устанавливаем состояние загрузки
        } else { // Логика для пагинации
            if (_isLoadingMore.value == true || isLastPage) {
                // Если уже грузим еще или это последняя страница, выходим
                if (!isLastPage) _isLoadingMore.value = false // Сбросить, если пытались загрузить последнюю страницу
                return
            }
            _isLoadingMore.value = true
        }

        // Отменяем предыдущий searchJob, если он активен (на всякий случай, хотя setFilter/setSearchTerm должны это делать)
        // searchJob?.cancel() // Это может быть излишним здесь, если setFilter/setSearchTerm всегда вызываются первыми

        viewModelScope.launch {
            val result = itemRepository.getItems(
                page = currentPage,
                size = pageSize,
                viewType = currentFilterType,
                searchTerm = currentSearchTerm
            )

            // Сбрасываем индикатор "загрузка еще" только после получения ответа для пагинации
            if (!isRefresh) {
                _isLoadingMore.value = false
            }

            result.onSuccess { pageResponse ->
                val newItems = pageResponse.content
                if (isRefresh) {
                    _itemsList.value = newItems // Присваиваем новый список
                } else {
                    // Добавляем к существующему списку, создавая новый экземпляр для LiveData
                    _itemsList.value = (_itemsList.value ?: emptyList()) + newItems
                }

                isLastPage = pageResponse.last
                if (!isLastPage && newItems.isNotEmpty()) {
                    currentPage++
                }

                if (isRefresh) {
                    // Для refresh, состояние зависит от того, пуст ли новый список
                    _loadState.value = if ((_itemsList.value ?: emptyList()).isEmpty()) LoadState.Empty else LoadState.Success
                } else {
                    // Для пагинации: если были загружены новые элементы и предыдущее состояние не было ошибкой,
                    // то общее состояние остается Success. Если список был пуст и что-то подгрузилось, станет Success.
                    if (newItems.isNotEmpty() && _loadState.value !is LoadState.Error) {
                        _loadState.value = LoadState.Success
                    }
                    // Если после пагинации список все еще пуст (хотя isLastPage=false),
                    // то _loadState не меняем (остается Empty или Success от предыдущего refresh).
                }

            }.onFailure { exception ->
                if (isRefresh) {
                    // _itemsList уже пуст из начала isRefresh
                    _loadState.value = LoadState.Error(exception.message ?: "Неизвестная ошибка")
                } else {
                    // Ошибка при пагинации. Можно показать Toast во фрагменте.
                    // _loadState не меняем, чтобы не скрывать уже загруженные данные.
                    // Можно добавить специальный LiveData для ошибок пагинации, если нужно.
                    Log.e("HomeViewModel", "Ошибка пагинации: ${exception.message}")
                }
            }
        }
    }

    fun setFilter(filterType: String) {
        val oldFilter = currentFilterType
        val oldSearchTerm = currentSearchTerm // Запоминаем старый поисковый запрос

        currentFilterType = filterType
        currentSearchTerm = null // Всегда сбрасываем поисковый запрос при смене фильтра

        // Перезагружаем данные, если фильтр изменился ИЛИ если был активен поисковый запрос
        if (oldFilter != currentFilterType || oldSearchTerm != null) {
            searchJob?.cancel() // Отменяем любой отложенный поиск
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
            // Если поиск был очищен, текущий запрос null, нет активного searchJob и не грузимся
            // Это может быть полезно, если пользователь быстро стер текст
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
        object Empty : LoadState() // Специфичное состояние для пустого ответа на 0 странице
        data class Error(val message: String) : LoadState() // data class требует message
    }
}

