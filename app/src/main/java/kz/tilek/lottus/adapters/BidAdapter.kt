package kz.tilek.lottus.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kz.tilek.lottus.databinding.ItemBidBinding 
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.util.FormatUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
class BidAdapter(private var bids: List<Bid>) : RecyclerView.Adapter<BidAdapter.BidViewHolder>() {
    inner class BidViewHolder(val binding: ItemBidBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
        val binding = ItemBidBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BidViewHolder(binding)
    }
    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
        val bid = bids[position]
        holder.binding.apply {
            tvBidderUsername.text = bid.bidder.username
            tvBidAmount.text = "${FormatUtils.formatPrice(bid.bidAmount)}"
            try {
                val bidTimeInstant = Instant.parse(bid.createdAt)
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale("ru", "RU"))
                    .withZone(ZoneId.systemDefault())
                tvBidTime.text = formatter.format(bidTimeInstant)
            } catch (e: Exception) {
                tvBidTime.text = "Неверное время"
            }
        }
    }
    override fun getItemCount(): Int = bids.size
    fun updateData(newBids: List<Bid>) {
        bids = newBids
        notifyDataSetChanged() 
    }
}
