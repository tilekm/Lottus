// ./app/src/main/java/kz/tilek/lottus/SplashActivity.kt
package kz.tilek.lottus

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Убедитесь, что эта зависимость добавлена: implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0" или новее
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.data.TokenManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_splash) // Опционально, если у вас есть макет для сплэш-экрана

        // Используем lifecycleScope для корутины в Activity
        lifecycleScope.launch {
            // kotlinx.coroutines.delay(1000) // Опциональная задержка для показа сплэш-экрана

            if (TokenManager.isUserLoggedIn) {
                // Токен существует, проверяем его валидность
                val isValidToken = checkTokenValidity()
                if (isValidToken) {
                    // Токен валиден, переходим в MainActivity
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                } else {
                    // Токен невалиден или истек, очищаем его и переходим в AuthActivity
                    Log.i("SplashActivity", "Токен невалиден или истек. Очистка и переход на экран авторизации.")
                    TokenManager.clearUserData(this@SplashActivity) // Очищаем невалидные данные
                    startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
                }
            } else {
                // Токен отсутствует или пользователь не залогинен, переходим в AuthActivity
                Log.i("SplashActivity", "Пользователь не авторизован. Переход на экран авторизации.")
                startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
            }
            // Закрываем SplashActivity, чтобы пользователь не мог вернуться на нее кнопкой "Назад"
            finish()
        }
    }

    /**
     * Проверяет валидность текущего токена путем выполнения запроса к /api/users/me.
     * @return true, если токен валиден, false в противном случае.
     */
    private suspend fun checkTokenValidity(): Boolean {
        // Выполняем сетевой запрос в IO-диспетчере
        return withContext(Dispatchers.IO) {
            try {
                // Пытаемся выполнить легковесный аутентифицированный API-запрос
                // Например, получение данных текущего пользователя
                val response = ApiClient.instance.getCurrentUserProfile()
                // Если запрос успешен (код 2xx), считаем токен валидным
                if (response.isSuccessful) {
                    Log.i("SplashActivity", "Проверка токена: успех, пользователь ${response.body()?.username}.")
                    true
                } else {
                    Log.w("SplashActivity", "Проверка токена: неудача, код ${response.code()}, сообщение: ${response.message()}.")
                    false
                }
            } catch (e: Exception) {
                // Сетевая ошибка или другая проблема
                Log.e("SplashActivity", "Ошибка при проверке валидности токена", e)
                false // В случае ошибки считаем токен невалидным для безопасности
            }
        }
    }
}
