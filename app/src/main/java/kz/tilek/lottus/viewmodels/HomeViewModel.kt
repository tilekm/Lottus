// ./app/src/main/java/kz/tilek/lottus/viewmodels/HomeViewModel.kt
package kz.tilek.lottus.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.repositories.ItemRepository

class HomeViewModel : ViewModel() {

    private val itemRepository = ItemRepository() // Создаем экземпляр репозитория

    // LiveData для хранения списка активных аукционов и состояния загрузки/ошибки
    private val _activeItemsState = MutableLiveData<Result<List<AuctionItem>>>()
    val activeItemsState: LiveData<Result<List<AuctionItem>>> = _activeItemsState

    // LiveData для индикатора загрузки (опционально)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Загружает список активных аукционов.
     */
    fun loadActiveItems() {
        viewModelScope.launch {
            _isLoading.value = true // Показываем загрузку
            val result = itemRepository.getActiveItems()
            _activeItemsState.value = result // Обновляем состояние (успех или ошибка)
            _isLoading.value = false // Скрываем загрузку
        }
    }

    // Можно добавить методы для загрузки всех лотов, поиска и т.д.
}
