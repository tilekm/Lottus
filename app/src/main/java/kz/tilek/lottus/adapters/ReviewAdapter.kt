// ./app/src/main/java/kz/tilek/lottus/adapters/ReviewAdapter.kt
package kz.tilek.lottus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kz.tilek.lottus.databinding.ItemReviewBinding // Создадим этот layout
import kz.tilek.lottus.models.Review
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class ReviewAdapter(private var reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.binding.apply {
            // Отображаем имя автора отзыва
            tvReviewerUsername.text = review.reviewer.username

            // Отображаем рейтинг
            ratingBarReview.rating = review.rating.toFloat()

            // Отображаем комментарий, если он есть
            if (!review.comment.isNullOrBlank()) {
                tvReviewComment.text = review.comment
                tvReviewComment.visibility = ViewGroup.VISIBLE
            } else {
                tvReviewComment.visibility = ViewGroup.GONE
            }

            // Форматируем дату создания отзыва
            try {
                val createdAtInstant = Instant.parse(review.createdAt)
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) // Формат даты (напр., 29 апр. 2025 г.)
                    .withLocale(Locale("ru", "RU"))
                    .withZone(ZoneId.systemDefault())
                tvReviewDate.text = formatter.format(createdAtInstant)
            } catch (e: Exception) {
                tvReviewDate.text = "Неверная дата"
            }
        }
    }

    override fun getItemCount(): Int = reviews.size

    fun updateData(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged() // Простое обновление
    }
}
