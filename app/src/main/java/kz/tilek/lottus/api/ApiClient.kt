package kz.tilek.lottus.api
import kz.tilek.lottus.App
import kz.tilek.lottus.data.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object ApiClient {

    private const val BASE_URL = "YOUR_BASE_URL"
    const val WEBSOCKET_URL = "wss://YOUR_BASE_URL/ws/websocket"

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY 
        }
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = TokenManager.getAuthToken(App.appContext) 
            val newRequest = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
            chain.proceed(newRequest)
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor) 
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS) 
            .build()
    }
    private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient) 
                .addConverterFactory(GsonConverterFactory.create()) 
                .build()
    }
    val instance: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
