package kz.tilek.lottus.api

import com.google.gson.annotations.SerializedName
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.models.Notification
import kz.tilek.lottus.models.Review
import kz.tilek.lottus.models.User
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

data class UserProfileUpdateRequest(
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String?
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class CreateAuctionRequest(
    @SerializedName("sellerId") val sellerId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("startPrice") val startPrice: BigDecimal,
    @SerializedName("buyNowPrice") val buyNowPrice: BigDecimal?,
    @SerializedName("minBidStep") val minBidStep: BigDecimal,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("imageUrls") val imageUrls: List<String>?
)

data class PlaceBidRequest(
    @SerializedName("bidderId") val bidderId: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("bidAmount") val bidAmount: BigDecimal
)

data class ReviewCreateRequest(
    @SerializedName("reviewedUserId") val reviewedUserId: String,
    @SerializedName("rating") val rating: BigDecimal,
    @SerializedName("comment") val comment: String?
)

// --- Responses (без изменений LoginResponse, BidMessage, FileUploadResponse, PageResponse) ---
data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String
)

data class BidMessage(
    @SerializedName("bidId") val bidId: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemTitle") val itemTitle: String,
    @SerializedName("bidderId") val bidderId: String,
    @SerializedName("bidderUsername") val bidderUsername: String,
    @SerializedName("bidAmount") val bidAmount: BigDecimal,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("nextMinimumBid") val nextMinimumBid: BigDecimal
)

data class FileUploadResponse(
    @SerializedName("fileUrl") val fileUrl: String
)


interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("api/users/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<User>

    @GET("api/users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Response<User>

    @GET("api/users/me")
    suspend fun getCurrentUserProfile(): Response<User>

    @GET("api/users/by-username/{username}")
    suspend fun getUserByUsername(@Path("username") username: String): Response<User>

    @PUT("api/users/me")
    suspend fun updateCurrentUserProfile(
        @Body updateRequest: UserProfileUpdateRequest
    ): Response<User>

    @GET("api/items")
    suspend fun getItems(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("viewType") viewType: String,
        @Query("searchTerm") searchTerm: String?, // <-- НОВЫЙ ПАРАМЕТР для поиска (nullable)
        @Query("sort") sort: String? = "createdAt,desc"
    ): Response<PageResponse<AuctionItem>>
    // ------------------------------------------------------------------------------------

    @POST("api/items/{itemId}/buy-now")
    suspend fun buyNowForItem(@Path("itemId") itemId: String): Response<Bid>

    @GET("api/items/{id}")
    suspend fun getItemDetails(@Path("id") itemId: String): Response<AuctionItem>

    @POST("api/items")
    suspend fun createItem(@Body createAuctionRequest: CreateAuctionRequest): Response<AuctionItem>

    @GET("api/items/seller/{sellerId}")
    suspend fun getItemsBySeller(@Path("sellerId") sellerId: String): Response<List<AuctionItem>>

    @POST("api/bids")
    suspend fun placeBid(@Body placeBidRequest: PlaceBidRequest): Response<Bid>

    @GET("api/bids/item/{itemId}")
    suspend fun getBidsForItem(@Path("itemId") itemId: String): Response<List<Bid>>

    @GET("api/bids/item/{itemId}/highest")
    suspend fun getHighestBidForItem(@Path("itemId") itemId: String): Response<Bid>

    @GET("api/bids/user/{userId}")
    suspend fun getUserBidHistory(@Path("userId") userId: String): Response<List<Bid>>

    @GET("api/notifications/user/{userId}")
    suspend fun getAllNotifications(@Path("userId") userId: String): Response<List<Notification>>

    @GET("api/notifications/user/{userId}/unread")
    suspend fun getUnreadNotifications(@Path("userId") userId: String): Response<List<Notification>>

    @POST("api/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") notificationId: String): Response<Unit>

    @Multipart
    @POST("api/media/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): Response<FileUploadResponse>

    @GET("api/users/{userId}/reviews")
    suspend fun getReviewsForUser(@Path("userId") userId: String): Response<List<Review>>

    @POST("api/reviews")
    suspend fun createReview(@Body reviewRequest: ReviewCreateRequest): Response<Review>
}
