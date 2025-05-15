package kz.tilek.lottus.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.data.TokenManager 
import kz.tilek.lottus.models.Notification 
import kz.tilek.lottus.repositories.NotificationRepository
class NotificationViewModel : ViewModel() {
    private val notificationRepository = NotificationRepository()
    private val _notificationsState = MutableLiveData<Result<List<Notification>>>()
    val notificationsState: LiveData<Result<List<Notification>>> = _notificationsState
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _markAsReadState = MutableLiveData<Result<Unit>>()
    val markAsReadState: LiveData<Result<Unit>> = _markAsReadState
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
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            val result = notificationRepository.markNotificationAsRead(notificationId)
            _markAsReadState.value = result 
            if (result.isSuccess) {
                loadNotifications() 
            }
        }
    }
}
