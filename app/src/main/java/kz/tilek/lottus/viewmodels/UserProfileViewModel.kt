package kz.tilek.lottus.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kz.tilek.lottus.api.ReviewCreateRequest // Импорт DTO
import kz.tilek.lottus.models.Review
import kz.tilek.lottus.models.User
import kz.tilek.lottus.repositories.ReviewRepository
import kz.tilek.lottus.repositories.UserRepository
import java.math.BigDecimal // Импорт BigDecimal

data class UserProfileDetails(
    val user: User,
    val reviews: List<Review>
)

class UserProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val reviewRepository = ReviewRepository()

    private val _profileDetailsState = MutableLiveData<Result<UserProfileDetails>>()
    val profileDetailsState: LiveData<Result<UserProfileDetails>> = _profileDetailsState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _createReviewState = MutableLiveData<Result<Review>?>()
    val createReviewState: MutableLiveData<Result<Review>?> = _createReviewState

    private val _isCreatingReview = MutableLiveData<Boolean>()
    val isCreatingReview: LiveData<Boolean> = _isCreatingReview
    // -----------------------------------------

    /**
     * Загружает публичный профиль пользователя и отзывы о нем.
     * (без изменений)
     */
    fun loadUserProfileAndReviews(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userProfileDeferred = async { userRepository.getUserProfile(userId) }
                val reviewsDeferred = async { reviewRepository.getReviewsForUser(userId) }
                val userProfileResult = userProfileDeferred.await()
                val reviewsResult = reviewsDeferred.await()

                if (userProfileResult.isSuccess && reviewsResult.isSuccess) {
                    val details = UserProfileDetails(
                        user = userProfileResult.getOrThrow(),
                        reviews = reviewsResult.getOrThrow()
                    )
                    _profileDetailsState.value = Result.success(details)
                } else {
                    val error = userProfileResult.exceptionOrNull()
                        ?: reviewsResult.exceptionOrNull()
                        ?: Exception("Неизвестная ошибка загрузки профиля")
                    _profileDetailsState.value = Result.failure(error)
                }
            } catch (e: Exception) {
                _profileDetailsState.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Создает новый отзыв.
     */
    fun createReview(reviewedUserId: String, rating: BigDecimal, comment: String?) {
        viewModelScope.launch {
            _isCreatingReview.value = true
            val request = ReviewCreateRequest(
                reviewedUserId = reviewedUserId,
                rating = rating,
                comment = comment
            )
            val result = reviewRepository.createReview(request)
            _createReviewState.value = result // Обновляем LiveData с результатом
            // Если успешно, нужно будет перезагрузить отзывы на экране UserProfileFragment
            if (result.isSuccess) {
               Log.d("UserProfileViewModel", "Отзыв успешно создан")
                loadUserProfileAndReviews(reviewedUserId)
            }
            _isCreatingReview.value = false
        }
    }

    /**
     * Сбрасывает состояние createReviewState.
     * Вызывается из диалога, чтобы избежать повторной обработки при пересоздании.
     */
    fun clearCreateReviewState() {
        _createReviewState.value = null
    }
}
