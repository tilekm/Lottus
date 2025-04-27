// ./app/src/main/java/kz/tilek/lottus/fragments/details/ItemDetailFragment.kt
package kz.tilek.lottus.fragments.details

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
// Убираем Glide отсюда, он будет в адаптере
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kz.tilek.lottus.R
import kz.tilek.lottus.adapters.BidAdapter
import kz.tilek.lottus.adapters.ImagePagerAdapter // <-- Импорт адаптера изображений
import kz.tilek.lottus.api.PlaceBidRequest
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.databinding.FragmentItemDetailBinding
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.util.FormatUtils
import kz.tilek.lottus.viewmodels.ItemDetailData
import kz.tilek.lottus.viewmodels.ItemDetailViewModel
import kz.tilek.lottus.websocket.WebSocketManager
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
// Убираем неиспользуемые импорты ZoneId, DateTimeFormatter, FormatStyle, Locale
import java.util.concurrent.TimeUnit

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemDetailViewModel by viewModels()
    private val args: ItemDetailFragmentArgs by navArgs()

    private lateinit var bidAdapter: BidAdapter
    private lateinit var imageAdapter: ImagePagerAdapter // <-- Добавляем адаптер изображений
    private var countDownTimer: CountDownTimer? = null
    private var currentItemData: ItemDetailData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerViews() // <-- Переименовываем и настраиваем оба RecyclerView/ViewPager

        val itemId = args.itemId
        val itemTopic = "/topic/items/$itemId/bids"

        WebSocketManager.subscribeToTopic(itemTopic)

        // --- Наблюдение за WebSocket (без изменений) ---
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                state = androidx.lifecycle.Lifecycle.State.STARTED
            ) {
                WebSocketManager.bidMessagesFlow.collectLatest { bidMessage ->
                    if (bidMessage != null && bidMessage.itemId == itemId) {
                        Log.d("ItemDetailFragment", "Новая ставка через WS: ${bidMessage.bidAmount}")
                        viewModel.loadItemDetails(itemId)
                        Toast.makeText(requireContext(), "Новая ставка: ${bidMessage.bidAmount} ₸ от ${bidMessage.bidderUsername}", Toast.LENGTH_SHORT).show()
                        // Сбрасываем значение, чтобы не обработать повторно при пересоздании View
                        WebSocketManager.bidMessagesFlow.value = null
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            WebSocketManager.connectionState.collectLatest { state ->
                Log.d("ItemDetailFragment", "WebSocket Connection State: $state")
            }
        }
        // ---------------------------------------------

        // --- Наблюдение за ViewModel (без изменений в логике, но bindItemData обновится) ---
        viewModel.itemDetailState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false
            enableBidInput(true)

            result.onSuccess { data ->
                currentItemData = data
                bindItemData(data.item) // <-- Этот метод теперь будет загружать и картинки
                bindBidsData(data.bids, data.item)
                startTimerIfNeeded(data.item)
            }.onFailure { exception ->
                currentItemData = null
                Toast.makeText(requireContext(), "Ошибка загрузки: ${exception.message}", Toast.LENGTH_LONG).show()
                binding.collapsingToolbar.title = "Ошибка"
                // Скрываем ViewPager и индикатор при ошибке
                binding.viewPagerImages.isVisible = false
                binding.dotsIndicator.isVisible = false
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.appBarLayout.isVisible = !isLoading
            binding.llPlaceBid.isVisible = !isLoading
            // Скрываем ViewPager и индикатор во время загрузки
            if (isLoading) {
                binding.viewPagerImages.isVisible = false
                binding.dotsIndicator.isVisible = false
            }
        })

        viewModel.placeBidState.observe(viewLifecycleOwner, Observer { result ->
            binding.btnPlaceBid.isEnabled = true
            binding.etBidAmount.isEnabled = true

            result.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка ставки: ${exception.message}", Toast.LENGTH_LONG).show()
                binding.tilBidAmount.error = exception.message
            }
            // При успехе данные перезагрузятся через itemDetailState
        })

        viewModel.isPlacingBid.observe(viewLifecycleOwner, Observer { isPlacing ->
            binding.btnPlaceBid.isEnabled = !isPlacing
            binding.etBidAmount.isEnabled = !isPlacing
            binding.tilBidAmount.error = null
            binding.btnPlaceBid.text = if (isPlacing) "Отправка..." else "Ставка"
        })
        // -----------------------------------------------------------------------

        binding.btnPlaceBid.setOnClickListener {
            placeBid(itemId, currentItemData)
        }

        viewModel.loadItemDetails(itemId)
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.collapsingToolbar.title = " "
    }

    // Настраиваем оба адаптера
    private fun setupRecyclerViews() {
        // Адаптер для ставок
        bidAdapter = BidAdapter(emptyList())
        binding.rvBids.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBids.adapter = bidAdapter
        binding.rvBids.isNestedScrollingEnabled = false

        // Адаптер для изображений
        imageAdapter = ImagePagerAdapter(emptyList())
        binding.viewPagerImages.adapter = imageAdapter

        // Связываем индикатор с ViewPager2
        binding.dotsIndicator.attachTo(binding.viewPagerImages)
    }

    private fun bindItemData(item: AuctionItem) {
        binding.collapsingToolbar.title = item.title
        binding.tvItemTitle.text = item.title
        binding.tvStartPrice.text = "Начальная: ${FormatUtils.formatPrice(item.startPrice)}"
        binding.tvMinStep.text = "Мин. шаг: ${FormatUtils.formatPrice(item.minBidStep)}"
        binding.tvItemDescription.text = item.description ?: "Нет описания"
        binding.tvSellerUsername.text = "${item.seller.username} (Рейтинг: ${item.seller.rating})"
        binding.tvItemStatus.text = "Статус: ${item.status.replaceFirstChar { it.uppercase() }}" // Используем uppercase()

        // Кнопка "Купить сейчас" (без изменений)
        if (item.buyNowPrice != null && item.status.equals("active", ignoreCase = true)) {
            binding.btnBuyNow.isVisible = true
            binding.btnBuyNow.text = "Купить сейчас за ${FormatUtils.formatPrice(item.buyNowPrice)}"
            binding.btnBuyNow.setOnClickListener { buyNow(item) }
        } else {
            binding.btnBuyNow.isVisible = false
        }

        // --- ЗАГРУЗКА ИЗОБРАЖЕНИЙ ---
        val imageUrls = item.imageUrls
        if (!imageUrls.isNullOrEmpty()) {
            imageAdapter.updateData(imageUrls)
            binding.viewPagerImages.isVisible = true
            // Показываем индикатор только если картинок больше одной
            binding.dotsIndicator.isVisible = imageUrls.size > 1
        } else {
            // Если картинок нет, скрываем ViewPager и индикатор
            imageAdapter.updateData(emptyList()) // Очищаем адаптер
            binding.viewPagerImages.isVisible = false
            binding.dotsIndicator.isVisible = false
            // Можно показать плейсхолдер в самом ViewPager или отдельном ImageView, если нужно
            // Например, можно было бы не скрывать ViewPager, а показать в нем один элемент с плейсхолдером
            // imageAdapter.updateData(listOf("placeholder")) // и обработать "placeholder" в адаптере
        }
        // ---------------------------

        enableBidInput(item.status.equals("active", ignoreCase = true))
    }

    // bindBidsData, startTimerIfNeeded, formatDuration, enableBidInput, placeBid, buyNow - без изменений

    private fun bindBidsData(bids: List<Bid>, item: AuctionItem) {
        bidAdapter.updateData(bids)
        binding.tvNoBids.isVisible = bids.isEmpty()
        binding.rvBids.isVisible = bids.isNotEmpty()

        val highestBid = bids.firstOrNull()
        val currentPrice = highestBid?.bidAmount ?: item.startPrice

        binding.tvCurrentPrice.text = "${FormatUtils.formatPrice(currentPrice)}"
        if (highestBid != null) {
            binding.tvHighestBidder.text = "(от ${highestBid.bidder.username})"
            binding.tvHighestBidder.isVisible = true
        } else {
            binding.tvHighestBidder.isVisible = false
        }

        val minNextBid = currentPrice.add(item.minBidStep)
        binding.tilBidAmount.hint = "Мин. след. ставка: ${FormatUtils.formatPrice(minNextBid)}"
    }

    private fun startTimerIfNeeded(item: AuctionItem) {
        countDownTimer?.cancel()

        if (item.status.equals("active", ignoreCase = true)) {
            try {
                val endTime = Instant.parse(item.endTime)
                val now = Instant.now()
                val durationMillis = Duration.between(now, endTime).toMillis()

                if (durationMillis > 0) {
                    countDownTimer = object : CountDownTimer(durationMillis, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            binding.tvTimeRemaining.text = formatDuration(millisUntilFinished)
                            binding.tvTimeRemaining.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                        }

                        override fun onFinish() {
                            binding.tvTimeRemaining.text = "Аукцион завершен"
                            binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant, null))
                            enableBidInput(false)
                            // viewModel.loadItemDetails(args.itemId) // Можно раскомментировать для автообновления
                        }
                    }.start()
                } else {
                    binding.tvTimeRemaining.text = "Аукцион завершен"
                    binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant, null))
                    enableBidInput(false)
                }
            } catch (e: Exception) {
                Log.e("ItemDetailFragment", "Ошибка парсинга или расчета времени", e)
                binding.tvTimeRemaining.text = "Ошибка времени"
                enableBidInput(false)
            }
        } else {
            binding.tvTimeRemaining.text = "Статус: ${item.status.replaceFirstChar { it.uppercase() }}"
            binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant, null))
            enableBidInput(false)
        }
    }

    private fun formatDuration(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return when {
            days > 0 -> String.format("Осталось: %dд %dч %dм", days, hours, minutes)
            hours > 0 -> String.format("Осталось: %dч %dм %dс", hours, minutes, seconds)
            minutes > 0 -> String.format("Осталось: %dм %dс", minutes, seconds)
            else -> String.format("Осталось: %dс", seconds)
        }
    }

    private fun enableBidInput(enabled: Boolean) {
        binding.llPlaceBid.isVisible = enabled
        binding.etBidAmount.isEnabled = enabled
        binding.btnPlaceBid.isEnabled = enabled
    }

    private fun placeBid(itemId: String, currentData: ItemDetailData?) {
        val amountStr = binding.etBidAmount.text.toString()
        val bidderId = TokenManager.userId

        if (bidderId == null) {
            Toast.makeText(requireContext(), "Не удалось определить пользователя", Toast.LENGTH_SHORT).show()
            return
        }

        binding.tilBidAmount.error = null

        val amount = try {
            BigDecimal(amountStr)
        } catch (e: Exception) {
            binding.tilBidAmount.error = "Некорректная сумма"
            return
        }

        if (amount <= BigDecimal.ZERO) {
            binding.tilBidAmount.error = "Сумма должна быть положительной"
            return
        }

        if (currentData != null) {
            val item = currentData.item
            val currentPrice = currentData.highestBid?.bidAmount ?: item.startPrice
            val minAllowedBid = currentPrice.add(item.minBidStep)

            if (amount < minAllowedBid) {
                binding.tilBidAmount.error = "Ставка должна быть не менее ${FormatUtils.formatPrice(minAllowedBid)}"
                return
            }

            if (item.seller.id == bidderId) {
                binding.tilBidAmount.error = "Продавец не может делать ставки"
                return
            }

        } else {
            Toast.makeText(requireContext(), "Не удалось проверить минимальную ставку. Попробуйте обновить.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.placeBid(PlaceBidRequest(bidderId, itemId, amount))
    }

    private fun buyNow(item: AuctionItem) {
        val bidderId = TokenManager.userId
        val buyNowPrice = item.buyNowPrice

        if (bidderId == null || buyNowPrice == null) {
            Toast.makeText(requireContext(), "Невозможно выполнить покупку", Toast.LENGTH_SHORT).show()
            return
        }

        if (item.seller.id == bidderId) {
            Toast.makeText(requireContext(), "Продавец не может купить свой лот", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Купить сейчас")
            .setMessage("Вы уверены, что хотите купить лот '${item.title}' за ${FormatUtils.formatPrice(buyNowPrice)}?")
            .setPositiveButton("Купить") { _, _ ->
                viewModel.placeBid(PlaceBidRequest(bidderId, item.id, buyNowPrice))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        val itemTopic = "/topic/items/${args.itemId}/bids"
        WebSocketManager.unsubscribeFromTopic(itemTopic)
        // Обнуляем адаптер ViewPager, чтобы избежать утечек
        binding.viewPagerImages.adapter = null
        _binding = null
    }
}
