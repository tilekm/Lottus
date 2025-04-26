// ./app/src/main/java/kz/tilek/lottus/adapters/AuctionAdapter.kt
package kz.tilek.lottus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
// import android.widget.PopupMenu // Пока убираем PopupMenu
import androidx.recyclerview.widget.RecyclerView
// import kz.tilek.lottus.R // Пока не нужен R.menu
import kz.tilek.lottus.databinding.ItemAuctionBinding
import kz.tilek.lottus.models.AuctionItem // Используем новую модель
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Изменяем тип списка на List<AuctionItem>
class AuctionAdapter(
    private var items: List<AuctionItem>, // Используем items вместо auctions, делаем var для обновления
    private val onItemClick: (AuctionItem) -> Unit // Добавляем обработчик клика
    // Убираем onItemAction для редактирования/удаления
    // private val onItemAction: (position: Int, action: String) -> Unit
) : RecyclerView.Adapter<AuctionAdapter.AuctionViewHolder>() {

    inner class AuctionViewHolder(val binding: ItemAuctionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            // Добавляем обработчик клика на весь элемент
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
        val item = items[position] // Используем item
        holder.binding.apply {
            tvTitle.text = item.title
//            tvDescription.text = item.description ?: "Нет описания" // Обработка null
            // Форматируем цену
            tvPrice.text = "Старт: ${item.startPrice} ₸" // Отображаем начальную цену

            // Отображаем статус
            tvStatus.text = when (item.status.lowercase()) {
                "active" -> "Активен"
                "completed" -> "Завершен"
                "cancelled" -> "Отменен"
                else -> item.status // Показываем как есть, если статус неизвестен
            }

            // Форматируем и отображаем время окончания
            try {
                val endTimeInstant = Instant.parse(item.endTime) // Парсим строку ISO 8601
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale("ru", "RU")) // Используем русскую локаль
                    .withZone(ZoneId.systemDefault()) // Используем системную временную зону
                tvEndTime.text = "До: ${formatter.format(endTimeInstant)}"
            } catch (e: Exception) {
                tvEndTime.text = "Неверное время" // Обработка ошибки парсинга
                // Логирование ошибки e.printStackTrace()
            }

            // Убираем обработчик долгого нажатия
            // holder.itemView.setOnLongClickListener { view -> ... }
        }
    }

    override fun getItemCount(): Int = items.size

    // Метод для обновления данных в адаптере
    fun updateData(newItems: List<AuctionItem>) {
        items = newItems
        notifyDataSetChanged() // Простое обновление, для оптимизации можно использовать DiffUtil
    }
}
