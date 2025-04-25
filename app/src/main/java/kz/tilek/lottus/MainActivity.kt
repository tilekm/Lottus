package kz.tilek.lottus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.websocket.WebSocketManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .setupWithNavController(navController)
        if (TokenManager.isUserLoggedIn) {
            WebSocketManager.connect()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Проверяем, не был ли выход выполнен через logout (isManuallyDisconnected)
        // Если просто закрыли приложение, то отключаемся.
        // Это необязательно, если логика переподключения устраивает,
        // но может быть полезно для явного управления.
         if (WebSocketManager.connectionState.value != WebSocketManager.ConnectionState.DISCONNECTED) {
             WebSocketManager.disconnect() // Можно раскомментировать, если нужно принудительно отключаться при закрытии MainActivity
         }
    }

}