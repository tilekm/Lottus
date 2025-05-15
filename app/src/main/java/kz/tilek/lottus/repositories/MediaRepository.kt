package kz.tilek.lottus.repositories
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kz.tilek.lottus.api.ApiClient
import kz.tilek.lottus.util.parseError
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
class MediaRepository(private val context: Context) { 
    private val apiService = ApiClient.instance
    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val filePart = createMultipartBodyPart(uri, inputStream)
                    ?: return Result.failure(Exception("Не удалось создать multipart-часть для URI: $uri"))
                val response = apiService.uploadImage(filePart)
                if (response.isSuccessful) {
                    response.body()?.fileUrl?.let { Result.success(it) }
                        ?: Result.failure(Exception("Не получен URL файла от сервера."))
                } else {
                    Result.failure(Exception("Ошибка загрузки файла: ${parseError(response)}"))
                }
            } ?: Result.failure(Exception("Не удалось открыть InputStream для URI: $uri")) 
        } catch (e: Exception) {
            Log.e("MediaRepository", "Ошибка при загрузке файла URI: $uri", e)
            Result.failure(e)
        }
    }
    private fun createMultipartBodyPart(uri: Uri, inputStream: InputStream): MultipartBody.Part? {
        val fileName = getFileName(uri) ?: "upload_${System.currentTimeMillis()}" 
        val mimeType = context.contentResolver.getType(uri) ?: "image/*" 
        val fileBytes = inputStream.readBytes()
        val requestFile = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", fileName, requestFile)
    }
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
