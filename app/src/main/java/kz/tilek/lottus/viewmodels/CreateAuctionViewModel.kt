// ./app/src/main/java/kz/tilek/lottus/viewmodels/CreateAuctionViewModel.kt
package kz.tilek.lottus.viewmodels

import android.app.Application // <-- Импорт
import android.net.Uri // <-- Импорт
import androidx.lifecycle.AndroidViewModel // <-- Изменяем на AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers // <-- Импорт
import kotlinx.coroutines.async // <-- Импорт
import kotlinx.coroutines.awaitAll // <-- Импорт
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // <-- Импорт
import kz.tilek.lottus.api.CreateAuctionRequest
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.repositories.ItemRepository
import kz.tilek.lottus.repositories.MediaRepository // <-- Импорт MediaRepository
import java.math.BigDecimal

// Используем AndroidViewModel для доступа к ContentResolver при загрузке файлов
class CreateAuctionViewModel(application: Application) : AndroidViewModel(application) {

    private val itemRepository = ItemRepository()
    // Создаем MediaRepository, передавая контекст
    private val mediaRepository = MediaRepository(application.applicationContext) // <-- Создаем MediaRepository

    private val _createAuctionState = MutableLiveData<Result<AuctionItem>>()
    val createAuctionState: LiveData<Result<AuctionItem>> = _createAuctionState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Загружает список изображений по их URI и возвращает список URL.
     * Выполняет загрузку параллельно.
     */
    suspend fun uploadImages(uris: List<Uri>): Result<List<String>> {
        // Выполняем в IO потоке
        return withContext(Dispatchers.IO) {
            if (uris.isEmpty()) {
                return@withContext Result.success(emptyList()) // Нет URI - нет загрузки
            }
            try {
                // Запускаем загрузку каждого URI в отдельной корутине (async)
                val uploadJobs = uris.map { uri ->
                    async { mediaRepository.uploadImage(uri) } // Вызываем метод репозитория
                }
                // Ожидаем завершения всех загрузок
                val results = uploadJobs.awaitAll()

                // Собираем URL или первую ошибку
                val uploadedUrls = mutableListOf<String>()
                var firstError: Throwable? = null

                results.forEach { result ->
                    result.onSuccess { url ->
                        uploadedUrls.add(url)
                    }.onFailure { error ->
                        if (firstError == null) firstError = error
                    }
                }

                // Если была ошибка, возвращаем ее
                if (firstError != null) {
                    Result.failure(firstError!!)
                } else {
                    // Все успешно, возвращаем список URL
                    Result.success(uploadedUrls)
                }
            } catch (e: Exception) {
                Result.failure(e) // Общая ошибка при параллельной загрузке
            }
        }
    }

    /**
     * Создает новый аукцион с учетом загруженных изображений.
     */
    fun createAuction(
        sellerId: String,
        title: String,
        description: String?,
        startPrice: BigDecimal,
        buyNowPrice: BigDecimal?,
        minBidStep: BigDecimal,
        startTime: String,
        endTime: String,
        imageUrls: List<String> // <-- Добавлен параметр imageUrls
    ) {
        // Запускаем в viewModelScope (главный поток по умолчанию)
        viewModelScope.launch {
            _isLoading.value = true // Уже должно быть true из submitAuction, но на всякий случай
            val request = CreateAuctionRequest(
                sellerId = sellerId,
                title = title,
                description = description,
                startPrice = startPrice,
                buyNowPrice = buyNowPrice,
                minBidStep = minBidStep,
                startTime = startTime,
                endTime = endTime,
                imageUrls = imageUrls // <-- Передаем список URL в запрос
            )
            // Вызываем метод репозитория для создания лота
            val result = itemRepository.createItem(request)
            _createAuctionState.value = result // Обновляем LiveData с результатом
            _isLoading.value = false // Скрываем загрузку после получения результата
        }
    }
}
