package kz.tilek.lottus.util
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response
data class ApiErrorResponse(
    @SerializedName("status") val status: Int?,
    @SerializedName("message") val message: String?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("path") val path: String?,
    @SerializedName("details") val details: String?
)
fun parseError(response: Response<*>): String {
    return try {
        val errorBody = response.errorBody()?.string()
        if (errorBody != null) {
            val errorResponse = Gson().fromJson(errorBody, ApiErrorResponse::class.java)
            errorResponse?.message ?: errorResponse?.details ?: "Код ошибки: ${response.code()}"
        } else {
            "Неизвестная ошибка (код: ${response.code()})"
        }
    } catch (e: Exception) {
        "Ошибка парсинга ответа сервера (код: ${response.code()})"
    }
}
