// ./app/src/main/java/kz/tilek/lottus/viewmodels/ProfileViewModel.kt
package kz.tilek.lottus.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.App
import kz.tilek.lottus.data.TokenManager // Для получения ID текущего пользователя
import kz.tilek.lottus.models.User
import kz.tilek.lottus.repositories.UserRepository

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()

    // LiveData для хранения данных профиля пользователя
    private val _userProfileState = MutableLiveData<Result<User>>()
    val userProfileState: LiveData<Result<User>> = _userProfileState

    // LiveData для индикатора загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Загружает профиль текущего пользователя.
     */
    fun loadCurrentUserProfile() {
        val currentUserId = TokenManager.userId // Получаем ID из TokenManager
        Log.i("ProfileViewModel", "Current User ID: ${TokenManager.token}")
        if (currentUserId != null) {
            loadUserProfile(currentUserId)
        } else {
            // Обработка случая, когда ID пользователя не найден (не должен происходить, если пользователь вошел)
            _userProfileState.value = Result.failure(Exception("Не удалось получить ID текущего пользователя."))
        }
    }

    /**
     * Загружает профиль пользователя по указанному ID.
     */
    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.getUserProfile(userId)
            _userProfileState.value = result

            // Опционально: если успешно, обновить данные в TokenManager
            if (result.isSuccess) {
                result.getOrNull()?.let { user ->
                    TokenManager.saveUserDetails(App.appContext, user.id, user.email, user.username)
                }
            }

            _isLoading.value = false
        }
    }

    // Логика выхода (logout) находится в AuthViewModel
}
