package kz.tilek.lottus.viewmodels
import android.app.Application 
import android.net.Uri 
import androidx.lifecycle.AndroidViewModel 
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers 
import kotlinx.coroutines.async 
import kotlinx.coroutines.awaitAll 
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext 
import kz.tilek.lottus.api.CreateAuctionRequest
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.repositories.ItemRepository
import kz.tilek.lottus.repositories.MediaRepository 
import java.math.BigDecimal
class CreateAuctionViewModel(application: Application) : AndroidViewModel(application) {
    private val itemRepository = ItemRepository()
    private val mediaRepository = MediaRepository(application.applicationContext) 
    private val _createAuctionState = MutableLiveData<Result<AuctionItem>>()
    val createAuctionState: LiveData<Result<AuctionItem>> = _createAuctionState
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    suspend fun uploadImages(uris: List<Uri>): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            if (uris.isEmpty()) {
                return@withContext Result.success(emptyList()) 
            }
            try {
                val uploadJobs = uris.map { uri ->
                    async { mediaRepository.uploadImage(uri) } 
                }
                val results = uploadJobs.awaitAll()
                val uploadedUrls = mutableListOf<String>()
                var firstError: Throwable? = null
                results.forEach { result ->
                    result.onSuccess { url ->
                        uploadedUrls.add(url)
                    }.onFailure { error ->
                        if (firstError == null) firstError = error
                    }
                }
                if (firstError != null) {
                    Result.failure(firstError!!)
                } else {
                    Result.success(uploadedUrls)
                }
            } catch (e: Exception) {
                Result.failure(e) 
            }
        }
    }
    fun createAuction(
        sellerId: String,
        title: String,
        description: String?,
        startPrice: BigDecimal,
        buyNowPrice: BigDecimal?,
        minBidStep: BigDecimal,
        startTime: String,
        endTime: String,
        imageUrls: List<String> 
    ) {
        viewModelScope.launch {
            _isLoading.value = true 
            val request = CreateAuctionRequest(
                sellerId = sellerId,
                title = title,
                description = description,
                startPrice = startPrice,
                buyNowPrice = buyNowPrice,
                minBidStep = minBidStep,
                startTime = startTime,
                endTime = endTime,
                imageUrls = imageUrls 
            )
            val result = itemRepository.createItem(request)
            _createAuctionState.value = result 
            _isLoading.value = false 
        }
    }
}
