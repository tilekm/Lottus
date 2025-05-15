package kz.tilek.lottus.viewmodels
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kz.tilek.lottus.App 
import kz.tilek.lottus.data.TokenManager 
import kz.tilek.lottus.models.User
import kz.tilek.lottus.repositories.UserRepository
class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val _userProfileState = MutableLiveData<Result<User>>()
    val userProfileState: LiveData<Result<User>> = _userProfileState
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    fun loadCurrentUserProfile() {
        Log.d("ProfileViewModel", "Загрузка профиля текущего пользователя через /me...")
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.getCurrentUserProfile()
            _userProfileState.value = result
            if (result.isSuccess) {
                result.getOrNull()?.let { user ->
                    TokenManager.saveUserDetails(App.appContext, user.id, user.email, user.username)
                }
            } else {
                Log.e("ProfileViewModel", "Ошибка загрузки профиля /me: ${result.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
        }
    }
    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.getUserProfile(userId) 
            _isLoading.value = false
        }
    }
}
