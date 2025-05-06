package kz.tilek.lottus.api

import android.content.Context
import kz.tilek.lottus.App
import kz.tilek.lottus.data.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // ЗАМЕНИ НА АДРЕС ТВОЕГО БЭКЕНДА!
    // Если бэкенд на том же компьютере, что и эмулятор:
    private const val BASE_URL = "http://10.0.2.2:8080/" // Убедись, что порт 8080 верный
    const val WEBSOCKET_URL = "ws://10.0.2.2:8080/ws/websocket" // Используем ws:// и путь к SockJS эндпоинту + /websocket
    // Если используешь реальное устройство в той же Wi-Fi сети:
    // private const val BASE_URL = "http://ТВОЙ_IP_АДРЕС_В_ЛОКАЛЬНОЙ_СЕТИ:8080/"

    // Ленивая инициализация OkHttpClient
    private val okHttpClient: OkHttpClient by lazy {
        // Логирование
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Логируем тело запроса/ответа
        }

        // Интерцептор для добавления токена
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            // Получаем Context из Application Context (требует небольшой доработки)
            // или передаем его при инициализации ApiClient
            val token = TokenManager.getAuthToken(App.appContext) // Предполагаем наличие App.appContext

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
            .addInterceptor(authInterceptor) // Добавляем Auth интерцептор
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS) // Добавим и write timeout
            .build()
    }

    // Ленивая инициализация Retrofit
    private val retrofit: Retrofit by lazy {
//        if (android.os.Build.MODEL == "sdk_gphone64_x86_64") {
//            Retrofit.Builder()
//                .baseUrl("http://10.0.2.2:8080")
//                .client(okHttpClient) // Используем один и тот же клиент
//                .addConverterFactory(GsonConverterFactory.create()) // Используем Gson
//                .build()
//        } else {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient) // Используем один и тот же клиент
                .addConverterFactory(GsonConverterFactory.create()) // Используем Gson
                .build()
//        }
    }

    // Ленивая инициализация ApiService
    val instance: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}