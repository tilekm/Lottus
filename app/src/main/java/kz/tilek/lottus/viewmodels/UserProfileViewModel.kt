// ./app/src/main/java/kz/tilek/lottus/viewmodels/UserProfileViewModel.kt
package kz.tilek.lottus.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kz.tilek.lottus.models.Review
import kz.tilek.lottus.models.User
import kz.tilek.lottus.repositories.ReviewRepository
import kz.tilek.lottus.repositories.UserRepository

// Data class для объединения данных профиля и отзывов
data class UserProfileDetails(
    val user: User,
    val reviews: List<Review>
)

class UserProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val reviewRepository = ReviewRepository()

    // LiveData для хранения профиля и отзывов
    private val _profileDetailsState = MutableLiveData<Result<UserProfileDetails>>()
    val profileDetailsState: LiveData<Result<UserProfileDetails>> = _profileDetailsState

    // LiveData для индикатора загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData для результата создания отзыва (понадобится позже)
    private val _createReviewState = MutableLiveData<Result<Review>>()
    val createReviewState: LiveData<Result<Review>> = _createReviewState

    private val _isCreatingReview = MutableLiveData<Boolean>()
    val isCreatingReview: LiveData<Boolean> = _isCreatingReview

    /**
     * Загружает публичный профиль пользователя и отзывы о нем.
     */
    fun loadUserProfileAndReviews(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Запускаем запросы параллельно
                val userProfileDeferred = async { userRepository.getUserProfile(userId) }
                val reviewsDeferred = async { reviewRepository.getReviewsForUser(userId) }

                // Ожидаем результаты
                val userProfileResult = userProfileDeferred.await()
                val reviewsResult = reviewsDeferred.await()

                // Проверяем оба результата
                if (userProfileResult.isSuccess && reviewsResult.isSuccess) {
                    val details = UserProfileDetails(
                        user = userProfileResult.getOrThrow(),
                        reviews = reviewsResult.getOrThrow()
                    )
                    _profileDetailsState.value = Result.success(details)
                } else {
                    // Если хотя бы один запрос неудачен, возвращаем первую ошибку
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

    // Метод для создания отзыва (понадобится позже)
    // fun createReview(reviewedUserId: String, rating: BigDecimal, comment: String?) { ... }
}
