// ./app/src/main/java/kz/tilek/lottus/repositories/MediaRepository.kt
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

class MediaRepository(private val context: Context) { // Требуется Context для работы с URI

    private val apiService = ApiClient.instance

    /**
     * Загружает файл по URI на сервер.
     * Возвращает Result с URL загруженного файла.
     */
    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            // Получаем InputStream из ContentResolver
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Создаем MultipartBody.Part из InputStream
                val filePart = createMultipartBodyPart(uri, inputStream)
                    ?: return Result.failure(Exception("Не удалось создать multipart-часть для URI: $uri"))

                // Выполняем запрос к API
                val response = apiService.uploadImage(filePart)

                if (response.isSuccessful) {
                    response.body()?.fileUrl?.let { Result.success(it) }
                        ?: Result.failure(Exception("Не получен URL файла от сервера."))
                } else {
                    Result.failure(Exception("Ошибка загрузки файла: ${parseError(response)}"))
                }
            } ?: Result.failure(Exception("Не удалось открыть InputStream для URI: $uri")) // Если openInputStream вернул null
        } catch (e: Exception) {
            Log.e("MediaRepository", "Ошибка при загрузке файла URI: $uri", e)
            Result.failure(e)
        }
    }

    /**
     * Вспомогательная функция для создания MultipartBody.Part из InputStream и Uri.
     */
    private fun createMultipartBodyPart(uri: Uri, inputStream: InputStream): MultipartBody.Part? {
        // Пытаемся получить имя файла и MIME-тип из URI
        val fileName = getFileName(uri) ?: "upload_${System.currentTimeMillis()}" // Генерируем имя, если не удалось получить
        val mimeType = context.contentResolver.getType(uri) ?: "image/*" // Тип по умолчанию, если не определен

        // Читаем байты из InputStream
        val fileBytes = inputStream.readBytes()

        // Создаем RequestBody из байтов
        val requestFile = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())

        // Создаем MultipartBody.Part
        return MultipartBody.Part.createFormData("file", fileName, requestFile)
    }

    /**
     * Вспомогательная функция для получения имени файла из URI.
     */
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
