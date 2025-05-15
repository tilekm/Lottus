package kz.tilek.lottus.viewmodels
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.App
import kz.tilek.lottus.api.UserProfileUpdateRequest
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.models.User
import kz.tilek.lottus.repositories.UserRepository
class UserSettingsViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val _userProfileState = MutableLiveData<Result<User>>()
    val userProfileState: LiveData<Result<User>> = _userProfileState
    private val _updateState = MutableLiveData<Result<User>>()
    val updateState: LiveData<Result<User>> = _updateState
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _isSaving = MutableLiveData<Boolean>()
    val isSaving: LiveData<Boolean> = _isSaving
    fun loadCurrentUserProfile() {
        Log.d("UserSettingsViewModel", "Загрузка профиля текущего пользователя...")
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.getCurrentUserProfile()
            _userProfileState.value = result
            _isLoading.value = false
            if (result.isFailure) {
                Log.e("UserSettingsViewModel", "Ошибка загрузки профиля: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    fun updateProfile(newUsername: String?, newEmail: String?) {
        val currentState = _userProfileState.value
        if (currentState == null || currentState.isFailure) {
            _updateState.value = Result.failure(Exception("Текущие данные профиля не загружены."))
            return
        }
        val currentUser = currentState.getOrNull() ?: return 
        val updatedUsername = newUsername?.trim().takeIf { it != currentUser.username && !it.isNullOrBlank() }
        val updatedEmail = newEmail?.trim().takeIf { it != currentUser.email && !it.isNullOrBlank() }
        if (updatedUsername == null && updatedEmail == null) {
            Log.d("UserSettingsViewModel", "Нет изменений для сохранения.")
            _updateState.value = Result.success(currentUser) 
            return
        }
        val request = UserProfileUpdateRequest(
            username = updatedUsername,
            email = updatedEmail
        )
        Log.d("UserSettingsViewModel", "Обновление профиля с данными: $request")
        viewModelScope.launch {
            _isSaving.value = true
            val result = userRepository.updateUserProfile(request)
            _updateState.value = result
            _isSaving.value = false
            if (result.isSuccess) {
                Log.d("UserSettingsViewModel", "Профиль успешно обновлен.")
                _userProfileState.value = result
                result.getOrNull()?.let { updatedUser ->
                    TokenManager.saveUserDetails(App.appContext, updatedUser.id, updatedUser.email, updatedUser.username)
                }
            } else {
                Log.e("UserSettingsViewModel", "Ошибка обновления профиля: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
