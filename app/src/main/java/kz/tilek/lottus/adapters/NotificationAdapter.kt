// ./app/src/main/java/kz/tilek/lottus/adapters/NotificationAdapter.kt
package kz.tilek.lottus.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kz.tilek.lottus.R
import kz.tilek.lottus.databinding.ItemNotificationBinding // Создадим этот layout
import kz.tilek.lottus.models.Notification
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class NotificationAdapter(
    private var notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit // Обработчик клика для пометки как прочитанное
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val notification = notifications[position]
                    // Вызываем обработчик только если уведомление не прочитано
                    if (!notification.isRead) {
                        onItemClick(notification)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.binding.apply {
            tvNotificationMessage.text = notification.message

            // Форматируем время
            try {
                val timeInstant = Instant.parse(notification.createdAt)
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale("ru", "RU"))
                    .withZone(ZoneId.systemDefault())
                tvNotificationTime.text = formatter.format(timeInstant)
            } catch (e: Exception) {
                tvNotificationTime.text = "Неверное время"
            }

            // Визуально выделяем непрочитанные уведомления
            if (notification.isRead) {
                tvNotificationMessage.setTypeface(null, Typeface.NORMAL)
                root.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.transparent)) // Обычный фон
            } else {
                tvNotificationMessage.setTypeface(null, Typeface.BOLD)
                root.setBackgroundColor(ContextCompat.getColor(root.context, R.color.light_gray_background)) // Слегка выделяем фон
            }
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateData(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged() // Простое обновление
    }
}
