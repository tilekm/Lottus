package kz.tilek.lottus.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kz.tilek.lottus.R
import kz.tilek.lottus.databinding.ItemAuctionBinding
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.util.FormatUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
class AuctionAdapter(
    private val onItemClick: (AuctionItem) -> Unit
) : ListAdapter<AuctionItem, AuctionAdapter.AuctionViewHolder>(AuctionDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuctionViewHolder {
        val binding = ItemAuctionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AuctionViewHolder(binding, onItemClick)
    }
    override fun onBindViewHolder(holder: AuctionViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }
    class AuctionViewHolder(
        private val binding: ItemAuctionBinding,
        private val onItemClick: (AuctionItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AuctionItem) {
            binding.root.setOnClickListener {
                onItemClick(item)
            }
            binding.apply {
                tvTitle.text = item.title
                tvPrice.text = "Старт: ${FormatUtils.formatPrice(item.startPrice)}"
                val firstImageUrl = item.imageUrls?.firstOrNull()
                Glide.with(itemView.context)
                    .load(firstImageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivAuctionImage)
                tvStatus.text = when (item.status.lowercase()) {
                    "active" -> "Активен"
                    "completed" -> "Завершен"
                    "cancelled" -> "Отменен"
                    "scheduled" -> "Запланирован"
                    else -> item.status.replaceFirstChar { it.uppercase() }
                }
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
    }
    class AuctionDiffCallback : DiffUtil.ItemCallback<AuctionItem>() {
        override fun areItemsTheSame(oldItem: AuctionItem, newItem: AuctionItem): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: AuctionItem, newItem: AuctionItem): Boolean {
            return oldItem == newItem
        }
    }
}
