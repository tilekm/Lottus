// ./app/src/main/java/kz/tilek/lottus/viewmodels/ProfileViewModel.kt
package kz.tilek.lottus.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.App // Импорт App для контекста
import kz.tilek.lottus.data.TokenManager // TokenManager больше не нужен для ID здесь
import kz.tilek.lottus.models.User
import kz.tilek.lottus.repositories.UserRepository

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _userProfileState = MutableLiveData<Result<User>>()
    val userProfileState: LiveData<Result<User>> = _userProfileState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Загружает профиль ТЕКУЩЕГО пользователя через эндпоинт /me.
     */
    fun loadCurrentUserProfile() {
        // ID пользователя из TokenManager больше не нужен для этого запроса
        Log.d("ProfileViewModel", "Загрузка профиля текущего пользователя через /me...")
        viewModelScope.launch {
            _isLoading.value = true
            // Вызываем новый метод репозитория
            val result = userRepository.getCurrentUserProfile()
            _userProfileState.value = result

            // Опционально: обновить данные в TokenManager при успехе, если они изменились
            if (result.isSuccess) {
                result.getOrNull()?.let { user ->
                    TokenManager.saveUserDetails(App.appContext, user.id, user.email, user.username)
                    // Можно добавить сохранение profilePictureUrl, если нужно где-то еще
                }
            } else {
                Log.e("ProfileViewModel", "Ошибка загрузки профиля /me: ${result.exceptionOrNull()?.message}")
            }

            _isLoading.value = false
        }
    }

    // Метод loadUserProfile(userId) остается для возможной загрузки других профилей в будущем
    // Если он точно не нужен, его можно удалить.
    /**
     * Загружает ПУБЛИЧНЫЙ профиль пользователя по указанному ID.
     * (Оставлен для будущего использования)
     */
    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.getUserProfile(userId) // Использует старый метод API GET /users/{id}
            // Важно: Этот метод теперь вернет ПУБЛИЧНЫЕ данные.
            // _userProfileState.value = result // Не обновляем userProfileState здесь, чтобы не смешивать
            _isLoading.value = false
        }
    }

    // Логика выхода (logout) находится в AuthViewModel
}
