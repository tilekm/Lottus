// ./app/src/main/java/kz/tilek/lottus/adapters/AuctionAdapter.kt
package kz.tilek.lottus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // <-- Импорт Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions // <-- Для плавного появления
import kz.tilek.lottus.R // <-- Для доступа к drawable
import kz.tilek.lottus.databinding.ItemAuctionBinding
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.util.FormatUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class AuctionAdapter(
    private var items: List<AuctionItem>,
    private val onItemClick: (AuctionItem) -> Unit
) : RecyclerView.Adapter<AuctionAdapter.AuctionViewHolder>() {

    inner class AuctionViewHolder(val binding: ItemAuctionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuctionViewHolder {
        val binding = ItemAuctionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AuctionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuctionViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvTitle.text = item.title
            tvPrice.text = "Старт: ${FormatUtils.formatPrice(item.startPrice)}"

            // --- ЗАГРУЗКА ИЗОБРАЖЕНИЯ ---
            val firstImageUrl = item.imageUrls?.firstOrNull() // Берем первый URL, если есть

            Glide.with(holder.itemView.context)
                .load(firstImageUrl) // Загружаем URL (может быть null)
                .placeholder(R.drawable.ic_image_placeholder) // Плейсхолдер во время загрузки
                .error(R.drawable.ic_broken_image) // Картинка при ошибке или если URL null
                .transition(DrawableTransitionOptions.withCrossFade()) // Плавное появление
                .into(ivAuctionImage) // В наш ImageView
            // ---------------------------

            // Отображаем статус
            tvStatus.text = when (item.status.lowercase()) {
                "active" -> "Активен"
                "completed" -> "Завершен"
                "cancelled" -> "Отменен"
                "scheduled" -> "Запланирован" // Добавим запланированный
                else -> item.status
            }
            // TODO: Установить соответствующий фон для статуса (status_background_active, _completed, _scheduled и т.д.)
            // val statusBackgroundRes = when (item.status.lowercase()) { ... }
            // tvStatus.setBackgroundResource(statusBackgroundRes)

            // Форматируем и отображаем время окончания
            try {
                val endTimeInstant = Instant.parse(item.endTime)
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale("ru", "RU"))
                    .withZone(ZoneId.systemDefault())
                tvEndTime.text = "До: ${formatter.format(endTimeInstant)}"
            } catch (e: Exception) {
                tvEndTime.text = "Неверное время"
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AuctionItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
