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
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val _loginState = MutableLiveData<Result<Unit>>() 
    val loginState: LiveData<Result<Unit>> = _loginState
    private val _registerState = MutableLiveData<Result<User>>()
    val registerState: LiveData<Result<User>> = _registerState
    fun login(username: String, password: String) { 
        viewModelScope.launch {
            val result = authRepository.login(username, password)
            _loginState.value = result
             if (result.isSuccess) {
                 WebSocketManager.connect()
             }
        }
    }
    fun register(username: String, email: String, password: String) { 
        viewModelScope.launch {
            _registerState.value = authRepository.register(username, email, password)
        }
    }
    fun logout() {
        WebSocketManager.disconnect()
        authRepository.logout()
    }
}
