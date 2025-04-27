package kz.tilek.lottus.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kz.tilek.lottus.databinding.ItemImagePreviewBinding

class ImagePreviewAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.PreviewViewHolder>() {

    private var imageUris: MutableList<Uri> = mutableListOf()

    inner class PreviewViewHolder(val binding: ItemImagePreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnRemoveImage.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClick(imageUris[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val binding = ItemImagePreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val uri = imageUris[position]
        Glide.with(holder.itemView.context)
            .load(uri)
            .centerCrop()
            .into(holder.binding.ivPreviewImage)
    }

    override fun getItemCount(): Int = imageUris.size

    fun addImages(uris: List<Uri>) {
        val startPosition = imageUris.size
        imageUris.addAll(uris)
        notifyItemRangeInserted(startPosition, uris.size)
    }

    fun removeImage(uri: Uri) {
        val position = imageUris.indexOf(uri)
        if (position != -1) {
            imageUris.removeAt(position)
            notifyItemRemoved(position)
            // Оповещаем об изменении позиций следующих элементов
            notifyItemRangeChanged(position, imageUris.size - position)
        }
    }

    fun getCurrentImageUris(): List<Uri> {
        return imageUris.toList() // Возвращаем копию списка
    }

    fun clearImages() {
        val size = imageUris.size
        imageUris.clear()
        notifyItemRangeRemoved(0, size)
    }
}
