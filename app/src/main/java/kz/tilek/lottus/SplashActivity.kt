// ./app/src/main/java/kz/tilek/lottus/SplashActivity.kt
package kz.tilek.lottus

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.tilek.lottus.data.TokenManager

@SuppressLint("CustomSplashScreen") // Стандартный SplashScreen API требует API 31+
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ANDROIDMODEL", android.os.Build.MODEL) // Логируем вызов метода
        // Можно установить простой layout с логотипом, если хочешь
        // setContentView(R.layout.activity_splash)

        // Используем корутины для небольшой задержки (опционально, для показа лого)
        // и проверки статуса входа
        CoroutineScope(Dispatchers.Main).launch {
            // Небольшая задержка для показа сплэш-экрана (можно убрать)
            // delay(1000) // 1 секунда

            // Проверяем, вошел ли пользователь
            if (TokenManager.isUserLoggedIn) { // Используем удобное свойство
                // Пользователь вошел -> Запускаем MainActivity
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // Пользователь не вошел -> Запускаем AuthActivity
                startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
            }
            // Закрываем SplashActivity, чтобы пользователь не мог вернуться на нее кнопкой "Назад"
            finish()
        }
    }
}
