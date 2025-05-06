package kz.tilek.lottus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kz.tilek.lottus.databinding.ItemLoadingFooterBinding

class LoadingStateAdapter : RecyclerView.Adapter<LoadingStateAdapter.LoadingViewHolder>() {

    private var isLoading = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadingViewHolder {
        val binding = ItemLoadingFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoadingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoadingViewHolder, position: Int) {
        holder.bind(isLoading)
    }

    override fun getItemCount(): Int = if (isLoading) 1 else 0

    fun setLoading(isLoading: Boolean) {
        val previousState = this.isLoading
        this.isLoading = isLoading
        if (previousState != isLoading) {
            if (isLoading) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }

    class LoadingViewHolder(private val binding: ItemLoadingFooterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(isLoading: Boolean) {
            binding.progressBarFooter.isVisible = isLoading
        }
    }
}
