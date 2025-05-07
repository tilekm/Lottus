package kz.tilek.lottus

import android.os.Bundle
import android.view.View
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

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.itemDetailFragment,
                R.id.userProfileFragment,
                R.id.createAuctionFragment,
                R.id.userSettingsFragment -> {
                    bottomNavigationView.visibility = View.GONE
                }
                else -> {
                    bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }

        if (TokenManager.isUserLoggedIn) {
            WebSocketManager.connect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (WebSocketManager.connectionState.value != WebSocketManager.ConnectionState.DISCONNECTED) {
            // WebSocketManager.disconnect() // Раскомментируй, если нужно принудительно отключаться
        }
    }
}
