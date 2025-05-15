package kz.tilek.lottus.repositories
import android.content.Context
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.LoginRequest
import kz.tilek.lottus.api.RegisterRequest
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.models.User
import kz.tilek.lottus.util.parseError 
class AuthRepository(private val context: Context) {
    private val apiService = ApiClient.instance
    suspend fun login(username: String, password: String): Result<Unit> { 
        return try {
            val response = apiService.login(LoginRequest(username, password)) 
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse?.token != null) {
                    TokenManager.saveAuthToken(context, loginResponse.token)
                    TokenManager.saveUserDetails(
                        context,
                        loginResponse.userId,
                        loginResponse.email,
                        loginResponse.username
                    )
                    TokenManager.setLoggedInStatus(context, true)
                    Result.success(Unit) 
                } else {
                    Result.failure(Exception("Ошибка авторизации: Не получен токен от сервера."))
                }
            } else {
                val errorMsg = parseError(response) 
                Result.failure(Exception("Ошибка авторизации: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun register(username: String, email: String, password: String): Result<User> { 
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                val registeredUser = response.body()
                if (registeredUser != null) {
                    Result.success(registeredUser)
                } else {
                    Result.failure(Exception("Ошибка регистрации: Не получены данные пользователя от сервера."))
                }
            } else {
                val errorMsg = parseError(response) 
                Result.failure(Exception("Ошибка регистрации: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    fun logout() {
        TokenManager.clearUserData(context)
    }
}
