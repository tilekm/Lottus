package kz.tilek.lottus.repositories

import android.content.Context
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.api.LoginRequest
import kz.tilek.lottus.api.RegisterRequest
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.models.User
import kz.tilek.lottus.util.parseError // Предполагаем наличие функции парсинга ошибок

class AuthRepository(private val context: Context) {
    // Используем ленивый instance из ApiClient
    private val apiService = ApiClient.instance

    /**
     * Выполняет вход пользователя.
     * В случае успеха сохраняет токен и статус входа.
     * Возвращает Result<Unit>, сигнализируя об успехе/неудаче операции входа.
     * Данные пользователя (ID, email) нужно будет загружать отдельно после успешного входа.
     */
    suspend fun login(username: String, password: String): Result<Unit> { // Изменили email на username, возвращаем Unit
        return try {
            val response = apiService.login(LoginRequest(username, password)) // Используем username
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse?.token != null) {
                    // Сохраняем токен
                    TokenManager.saveAuthToken(context, loginResponse.token)
                    // Устанавливаем статус "вошел"
                    TokenManager.saveUserDetails(
                        context,
                        loginResponse.userId,
                        loginResponse.email,
                        loginResponse.username
                    )
                    TokenManager.setLoggedInStatus(context, true)
                    // Не сохраняем User ID/email здесь, их нужно будет получить отдельно
                    Result.success(Unit) // Успешный вход
                } else {
                    // Неожиданный ответ без токена
                    Result.failure(Exception("Ошибка авторизации: Не получен токен от сервера."))
                }
            } else {
                // Ошибка от сервера (4xx, 5xx)
                val errorMsg = parseError(response) // Используем парсер ошибок
                Result.failure(Exception("Ошибка авторизации: $errorMsg"))
            }
        } catch (e: Exception) {
            // Сетевая или другая ошибка
            Result.failure(e)
        }
    }

    /**
     * Регистрирует нового пользователя.
     * В случае успеха возвращает данные созданного пользователя.
     * Токен НЕ сохраняется, пользователь должен войти после регистрации.
     */
    suspend fun register(username: String, email: String, password: String): Result<User> { // Изменили name на username
        return try {
            // Вызываем register из ApiService, который возвращает Response<User>
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                val registeredUser = response.body()
                if (registeredUser != null) {
                    // Регистрация успешна, возвращаем данные пользователя
                    Result.success(registeredUser)
                    // Токен здесь не получаем и не сохраняем
                } else {
                    Result.failure(Exception("Ошибка регистрации: Не получены данные пользователя от сервера."))
                }
            } else {
                // Ошибка от сервера (например, пользователь уже существует)
                val errorMsg = parseError(response) // Используем парсер ошибок
                Result.failure(Exception("Ошибка регистрации: $errorMsg"))
            }
        } catch (e: Exception) {
            // Сетевая или другая ошибка
            Result.failure(e)
        }
    }

    /**
     * Выполняет выход пользователя, очищая все сохраненные данные.
     */
    fun logout() {
        TokenManager.clearUserData(context)
    }
}