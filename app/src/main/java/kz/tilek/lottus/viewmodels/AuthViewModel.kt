// ./app/src/main/java/kz/tilek/lottus/viewmodels/AuthViewModel.kt
package kz.tilek.lottus.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.models.User
import kz.tilek.lottus.repositories.AuthRepository
import kz.tilek.lottus.websocket.WebSocketManager

// Используем AndroidViewModel для доступа к Application Context
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    // Передаем applicationContext в репозиторий
    private val authRepository = AuthRepository(application)

    // LiveData для состояния входа (успех/неудача)
    private val _loginState = MutableLiveData<Result<Unit>>() // Тип изменен на Result<Unit>
    val loginState: LiveData<Result<Unit>> = _loginState

    // LiveData для состояния регистрации (успех с данными User / неудача)
    private val _registerState = MutableLiveData<Result<User>>()
    val registerState: LiveData<Result<User>> = _registerState

    // LiveData для хранения данных пользователя после успешного входа (опционально)
    // private val _userData = MutableLiveData<User?>()
    // val userData: LiveData<User?> = _userData

    fun login(username: String, password: String) { // Изменили email на username
        viewModelScope.launch {
            // Показываем состояние загрузки (можно добавить отдельный LiveData для этого)
            // _isLoading.value = true
            val result = authRepository.login(username, password)
            _loginState.value = result
            // _isLoading.value = false

            // Если вход успешен, можно сразу запросить данные пользователя
             if (result.isSuccess) {
                 WebSocketManager.connect()
             }
        }
    }

    fun register(username: String, email: String, password: String) { // Изменили name на username
        viewModelScope.launch {
            // _isLoading.value = true
            _registerState.value = authRepository.register(username, email, password)
            // _isLoading.value = false
        }
    }

    fun logout() {
        WebSocketManager.disconnect()
        authRepository.logout()
        // Очищаем LiveData, если нужно
        // _loginState.value = null
        // _registerState.value = null
        // _userData.value = null
    }

    // Опционально: функция для загрузки профиля пользователя после входа
    // fun fetchUserProfile() {
    //     viewModelScope.launch {
    //         val userId = TokenManager.userId // Получаем ID из TokenManager
    //         if (userId != null) {
    //             // Нужен UserRepository для выполнения запроса
    //             // val result = userRepository.getUserProfile(userId)
    //             // if (result.isSuccess) {
    //             //     _userData.value = result.getOrNull()
    //             //     // Можно сохранить детали в TokenManager, если нужно
    //             //     result.getOrNull()?.let { user ->
    //             //         TokenManager.saveUserDetails(getApplication(), user.id, user.email, user.username)
    //             //     }
    //             // } else {
    //             //     // Обработка ошибки загрузки профиля
    //             // }
    //         }
    //     }
    // }
}
