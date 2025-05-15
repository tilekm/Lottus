package kz.tilek.lottus.data
import android.content.Context
import android.content.SharedPreferences
import kz.tilek.lottus.App 
object TokenManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_TOKEN = "auth_token" 
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_USERNAME = "user_username" 
    private const val KEY_IS_LOGGED_IN = "is_logged_in" 
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    fun saveAuthToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply()
    }
    fun saveUserDetails(context: Context, userId: String, email: String, username: String) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_USERNAME, username)
            apply()
        }
    }
    fun setLoggedInStatus(context: Context, isLoggedIn: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    fun getAuthToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }
    fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }
    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }
    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_USERNAME, null)
    }
    fun isLoggedIn(context: Context): Boolean {
        return getAuthToken(context) != null && getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }
    fun clearUserData(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
    val token: String?
        get() = getAuthToken(App.appContext)
    val userId: String?
        get() = getUserId(App.appContext)
    val email: String?
        get() = getUserEmail(App.appContext)
    val username: String?
        get() = getUsername(App.appContext)
    val isUserLoggedIn: Boolean
        get() = isLoggedIn(App.appContext)
    fun clear() {
        clearUserData(App.appContext)
    }
}
