package kz.tilek.lottus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kz.tilek.lottus.R // Для плейсхолдера
import kz.tilek.lottus.databinding.ItemImageSlideBinding

class ImagePagerAdapter(private var imageUrls: List<String>) :
    RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemImageSlideBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageSlideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        Glide.with(holder.itemView.context)
            .load(imageUrl) // Загружаем URL
            .placeholder(R.drawable.ic_image_placeholder) // Плейсхолдер во время загрузки
            .error(R.drawable.ic_broken_image) // Картинка при ошибке загрузки
            .transition(DrawableTransitionOptions.withCrossFade()) // Плавное появление
            .into(holder.binding.ivSlideImage)
    }

    override fun getItemCount(): Int = imageUrls.size

    // Метод для обновления данных
    fun updateData(newImageUrls: List<String>) {
        imageUrls = newImageUrls
        notifyDataSetChanged() // Простое обновление, можно использовать DiffUtil для оптимизации
    }
}
