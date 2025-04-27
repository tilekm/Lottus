// ./app/src/main/java/kz/tilek/lottus/api/ApiService.kt

package kz.tilek.lottus.api

import com.google.gson.annotations.SerializedName
import kz.tilek.lottus.models.AuctionItem // Импорт исправленной модели
import kz.tilek.lottus.models.Bid // Импорт новой модели
import kz.tilek.lottus.models.Notification // Импорт новой модели
import kz.tilek.lottus.models.User // Импорт исправленной модели
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.math.BigDecimal // Импорт BigDecimal

// --- Requests ---

// Соответствует AuthenticationRequest.java
data class LoginRequest(
    @SerializedName("username") val username: String, // email -> username
    @SerializedName("password") val password: String
)

// Соответствует UserRegistrationRequest.java
data class RegisterRequest(
    @SerializedName("username") val username: String, // name -> username
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

// Соответствует ItemCreateRequest.java
data class CreateAuctionRequest(
    @SerializedName("sellerId") val sellerId: String, // Добавлено (UUID -> String)
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?, // Nullable
    @SerializedName("startPrice") val startPrice: BigDecimal, // startingPrice: Double -> startPrice: BigDecimal
    @SerializedName("buyNowPrice") val buyNowPrice: BigDecimal?, // Добавлено, Nullable
    @SerializedName("minBidStep") val minBidStep: BigDecimal, // Добавлено
    @SerializedName("startTime") val startTime: String, // Добавлено (Instant -> String)
    @SerializedName("endTime") val endTime: String, // endTime: Long -> endTime: String
    @SerializedName("imageUrls") val imageUrls: List<String>? // Список URL загруженных
    // imageUrl удален
)

// Соответствует BidRequest.java
data class PlaceBidRequest( // Переименовано для ясности
    @SerializedName("bidderId") val bidderId: String, // Добавлено (UUID -> String)
    @SerializedName("itemId") val itemId: String, // Добавлено (UUID -> String)
    @SerializedName("bidAmount") val bidAmount: BigDecimal // amount: Double -> bidAmount: BigDecimal
)

// --- Responses ---

// Соответствует AuthenticationResponse.java
data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("userId") val userId: String, // Добавлено
    @SerializedName("username") val username: String, // Добавлено
    @SerializedName("email") val email: String     // Добавлено
    // user удален, т.к. его нет в AuthenticationResponse.java
)

// Для WebSocket сообщений, соответствует BidMessage.java
data class BidMessage(
    @SerializedName("bidId") val bidId: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemTitle") val itemTitle: String,
    @SerializedName("bidderId") val bidderId: String,
    @SerializedName("bidderUsername") val bidderUsername: String,
    @SerializedName("bidAmount") val bidAmount: BigDecimal,
    @SerializedName("createdAt") val createdAt: String, // Instant -> String
    @SerializedName("nextMinimumBid") val nextMinimumBid: BigDecimal
)

data class FileUploadResponse(
    @SerializedName("fileUrl") val fileUrl: String
)


// --- ApiService Interface ---
// (Интерфейс будет исправлен на следующем шаге)
interface ApiService {
    // Методы будут обновлены позже...
    @POST("api/auth/login") // Добавлен префикс /api/
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("api/users/register") // Добавлен префикс /api/ и исправлен путь
    suspend fun register(@Body registerRequest: RegisterRequest): Response<User> // Бэкенд возвращает User при регистрации

    // Users
    @GET("api/users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Response<User>

    @GET("api/users/by-username/{username}")
    suspend fun getUserByUsername(@Path("username") username: String): Response<User>

    // Items (Auctions)
    @GET("api/items")
    suspend fun getAllItems(): Response<List<AuctionItem>>

    @GET("api/items/active")
    suspend fun getActiveItems(): Response<List<AuctionItem>>

    @GET("api/items/{id}")
    suspend fun getItemDetails(@Path("id") itemId: String): Response<AuctionItem>

    @POST("api/items")
    suspend fun createItem(@Body createAuctionRequest: CreateAuctionRequest): Response<AuctionItem> // Исправлен тип запроса и ответа

    @GET("api/items/seller/{sellerId}")
    suspend fun getItemsBySeller(@Path("sellerId") sellerId: String): Response<List<AuctionItem>>

    // Bids
    @POST("api/bids")
    suspend fun placeBid(@Body placeBidRequest: PlaceBidRequest): Response<Bid> // Исправлен тип запроса и ответа

    @GET("api/bids/item/{itemId}")
    suspend fun getBidsForItem(@Path("itemId") itemId: String): Response<List<Bid>>

    @GET("api/bids/item/{itemId}/highest")
    suspend fun getHighestBidForItem(@Path("itemId") itemId: String): Response<Bid> // Может вернуть 204 No Content, обработать в репозитории

    @GET("api/bids/user/{userId}")
    suspend fun getUserBidHistory(@Path("userId") userId: String): Response<List<Bid>>

    // Notifications
    @GET("api/notifications/user/{userId}")
    suspend fun getAllNotifications(@Path("userId") userId: String): Response<List<Notification>>

    @GET("api/notifications/user/{userId}/unread")
    suspend fun getUnreadNotifications(@Path("userId") userId: String): Response<List<Notification>>

    @POST("api/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") notificationId: String): Response<Unit> // Ответ без тела (Void/Unit)

    // Media
    @Multipart // Указываем, что это multipart запрос
    @POST("api/media/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part // Сам файл
        // Можно добавить @Part("description") description: RequestBody, если нужно передать доп. данные
    ): Response<FileUploadResponse> // Ожидаем ответ с URL файла
    // ---------------------------------------
}
