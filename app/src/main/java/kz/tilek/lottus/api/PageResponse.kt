package kz.tilek.lottus.api
import com.google.gson.annotations.SerializedName
data class PageResponse<T>(
    @SerializedName("content") val content: List<T>,
    @SerializedName("pageable") val pageable: PageableDetails,
    @SerializedName("last") val last: Boolean,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("size") val size: Int,
    @SerializedName("number") val number: Int, 
    @SerializedName("sort") val sort: SortDetails,
    @SerializedName("first") val first: Boolean,
    @SerializedName("numberOfElements") val numberOfElements: Int,
    @SerializedName("empty") val empty: Boolean
)
data class PageableDetails(
    @SerializedName("pageNumber") val pageNumber: Int,
    @SerializedName("pageSize") val pageSize: Int,
    @SerializedName("sort") val sort: SortDetails,
    @SerializedName("offset") val offset: Long,
    @SerializedName("paged") val paged: Boolean,
    @SerializedName("unpaged") val unpaged: Boolean
)
data class SortDetails(
    @SerializedName("empty") val empty: Boolean,
    @SerializedName("sorted") val sorted: Boolean,
    @SerializedName("unsorted") val unsorted: Boolean
)
