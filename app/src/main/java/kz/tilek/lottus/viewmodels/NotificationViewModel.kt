// ./app/src/main/java/kz/tilek/lottus/viewmodels/NotificationViewModel.kt
package kz.tilek.lottus.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.data.TokenManager // Для получения ID пользователя
import kz.tilek.lottus.models.Notification // Наша модель
import kz.tilek.lottus.repositories.NotificationRepository

class NotificationViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository()

    // LiveData для списка уведомлений
    private val _notificationsState = MutableLiveData<Result<List<Notification>>>()
    val notificationsState: LiveData<Result<List<Notification>>> = _notificationsState

    // LiveData для состояния загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData для результата пометки как прочитанного (опционально, для обратной связи)
    private val _markAsReadState = MutableLiveData<Result<Unit>>()
    val markAsReadState: LiveData<Result<Unit>> = _markAsReadState

    /**
     * Загружает все уведомления текущего пользователя.
     */
    fun loadNotifications() {
        val userId = TokenManager.userId
        if (userId == null) {
            _notificationsState.value = Result.failure(Exception("Не удалось получить ID пользователя."))
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = notificationRepository.getAllNotifications(userId)
            _notificationsState.value = result
            _isLoading.value = false
        }
    }

    /**
     * Помечает уведомление как прочитанное.
     */
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            // Можно показать индикатор для конкретного элемента или обновить его вид
            val result = notificationRepository.markNotificationAsRead(notificationId)
            _markAsReadState.value = result // Сообщаем результат

            // Если успешно, перезагружаем список, чтобы обновить статус isRead
            if (result.isSuccess) {
                loadNotifications() // Перезагружаем список
            }
        }
    }

    // Можно добавить метод для загрузки только непрочитанных: loadUnreadNotifications()
}
