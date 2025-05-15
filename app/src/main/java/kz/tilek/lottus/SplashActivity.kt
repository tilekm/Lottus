package kz.tilek.lottus
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope 
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.data.TokenManager
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (TokenManager.isUserLoggedIn) {
                TokenManager.token?.let { Log.d("SplashActivity", it) }
                val isValidToken = checkTokenValidity()
                if (isValidToken) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                } else {
                    Log.i("SplashActivity", "Токен невалиден или истек. Очистка и переход на экран авторизации.")
                    TokenManager.clearUserData(this@SplashActivity) 
                    startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
                }
            } else {
                Log.i("SplashActivity", "Пользователь не авторизован. Переход на экран авторизации.")
                startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
            }
            finish()
        }
    }
    private suspend fun checkTokenValidity(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getCurrentUserProfile()
                if (response.isSuccessful) {
                    Log.i("SplashActivity", "Проверка токена: успех, пользователь ${response.body()?.username}.")
                    true
                } else {
                    Log.w("SplashActivity", "Проверка токена: неудача, код ${response.code()}, сообщение: ${response.message()}.")
                    false
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "Ошибка при проверке валидности токена", e)
                false 
            }
        }
    }
}
