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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kz.tilek.lottus.R
import kz.tilek.lottus.adapters.BidAdapter
import kz.tilek.lottus.adapters.ImagePagerAdapter
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
import java.util.concurrent.TimeUnit

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemDetailViewModel by viewModels()
    private val args: ItemDetailFragmentArgs by navArgs()

    private lateinit var bidAdapter: BidAdapter
    private lateinit var imageAdapter: ImagePagerAdapter
    private var countDownTimer: CountDownTimer? = null
    private var currentItemData: ItemDetailData? = null // Храним текущие данные

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
        setupRecyclerViews()

        val itemId = args.itemId
        val itemTopic = "/topic/items/$itemId/bids"

        WebSocketManager.subscribeToTopic(itemTopic)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                WebSocketManager.bidMessagesFlow.collectLatest { bidMessage ->
                    if (bidMessage != null && bidMessage.itemId == itemId) {
                        Log.d("ItemDetailFragment", "Новая ставка через WS: ${bidMessage.bidAmount}")
                        viewModel.loadItemDetails(itemId) // Перезагружаем данные
                        Toast.makeText(
                            requireContext(),
                            "Новая ставка: ${bidMessage.bidAmount} ₸ от ${bidMessage.bidderUsername}",
                            Toast.LENGTH_SHORT
                        ).show()
                        WebSocketManager.bidMessagesFlow.value = null
                    }
                }
            }
        }
        // ... (остальные WebSocket observers)

        viewModel.itemDetailState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false
            // enableBidInput(true) // Будет управляться в bindItemData

            result.onSuccess { data ->
                currentItemData = data // Сохраняем актуальные данные
                bindItemData(data.item, data.highestBid) // Передаем и лот, и высшую ставку
                bindBidsData(data.bids, data.item)
                startTimerIfNeeded(data.item)
            }.onFailure { exception ->
                currentItemData = null
                Toast.makeText(requireContext(), "Ошибка загрузки: ${exception.message}", Toast.LENGTH_LONG).show()
                binding.collapsingToolbar.title = "Ошибка"
                binding.viewPagerImages.isVisible = false
                binding.dotsIndicator.isVisible = false
                enableBidInput(false) // Отключаем ввод при ошибке
                binding.btnBuyNow.isVisible = false // Скрываем кнопку "Купить сейчас"
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading
            if (!isLoading) { // Показываем контент только после загрузки
                binding.appBarLayout.isVisible = true
                // Видимость llPlaceBid и btnBuyNow будет управляться в bindItemData/updateBuyNowButtonVisibility
            } else {
                binding.appBarLayout.isVisible = false // Скрываем AppBar во время загрузки
                binding.llPlaceBid.isVisible = false
                binding.btnBuyNow.isVisible = false
                binding.viewPagerImages.isVisible = false
                binding.dotsIndicator.isVisible = false
            }
        })

        viewModel.placeBidState.observe(viewLifecycleOwner, Observer { result ->
            // enableBidInput(true) // Уже управляется isPlacingBid
            result.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка ставки: ${exception.message}", Toast.LENGTH_LONG).show()
                binding.tilBidAmount.error = exception.message
            }
            // При успехе данные перезагрузятся через itemDetailState (вызов loadItemDetails в ViewModel)
        })

        viewModel.isPlacingBid.observe(viewLifecycleOwner, Observer { isPlacing ->
            binding.btnPlaceBid.isEnabled = !isPlacing
            binding.etBidAmount.isEnabled = !isPlacing
            if (!isPlacing) binding.tilBidAmount.error = null // Сбрасываем ошибку только когда не загружаемся
            binding.btnPlaceBid.text = if (isPlacing) "Отправка..." else "Ставка"
        })

        // --- Наблюдение за состоянием "Купить сейчас" ---
        viewModel.buyNowState.observe(viewLifecycleOwner, Observer { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Лот успешно куплен!", Toast.LENGTH_SHORT).show()
                // UI обновится через itemDetailState после loadItemDetails
            }
            result.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка покупки: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        })

        viewModel.isBuyingNow.observe(viewLifecycleOwner, Observer { isBuying ->
            binding.btnBuyNow.isEnabled = !isBuying // Блокируем кнопку во время процесса
            // Можно добавить ProgressBar на кнопку или рядом
        })
        // ---------------------------------------------

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
        binding.collapsingToolbar.title = " " // Изначально пустое
    }

    private fun setupRecyclerViews() {
        bidAdapter = BidAdapter(emptyList())
        binding.rvBids.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBids.adapter = bidAdapter
        binding.rvBids.isNestedScrollingEnabled = false

        imageAdapter = ImagePagerAdapter(emptyList())
        binding.viewPagerImages.adapter = imageAdapter
        binding.dotsIndicator.attachTo(binding.viewPagerImages)
    }

    private fun bindItemData(item: AuctionItem, highestBid: Bid?) {
        binding.collapsingToolbar.title = item.title
        binding.tvItemTitle.text = item.title
        binding.tvStartPrice.text = "Начальная: ${FormatUtils.formatPrice(item.startPrice)}"
        binding.tvMinStep.text = "Мин. шаг: ${FormatUtils.formatPrice(item.minBidStep)}"
        binding.tvItemDescription.text = item.description ?: "Нет описания"
        binding.tvItemStatus.text = "Статус: ${item.status.replaceFirstChar { it.uppercase() }}"

        updateBuyNowButtonVisibility(item, highestBid) // Обновляем видимость кнопки "Купить сейчас"

        val imageUrls = item.imageUrls
        if (!imageUrls.isNullOrEmpty()) {
            imageAdapter.updateData(imageUrls)
            binding.viewPagerImages.isVisible = true
            binding.dotsIndicator.isVisible = imageUrls.size > 1
        } else {
            imageAdapter.updateData(emptyList())
            binding.viewPagerImages.isVisible = false
            binding.dotsIndicator.isVisible = false
        }

        val sellerText = "${item.seller.username} (Рейтинг: ${item.seller.rating})"
        Log.d("ItemDetailFragment", "All user fields mapped: ${item.seller.username}, ${item.seller.rating}")
        binding.tvSellerUsername.text = sellerText
        binding.tvSellerUsername.setOnClickListener {
            val action = ItemDetailFragmentDirections.actionItemDetailFragmentToUserProfileFragment(item.seller.id)
            findNavController().navigate(action)
        }

        // Управляем видимостью и активностью поля для ставки
        val auctionIsActive = item.status.equals("active", ignoreCase = true)
        enableBidInput(auctionIsActive)
    }

    private fun updateBuyNowButtonVisibility(item: AuctionItem, currentHighestBid: Bid?) {
        val buyNowPrice = item.buyNowPrice
        val currentPrice = currentHighestBid?.bidAmount ?: item.startPrice

        val canBuyNow = buyNowPrice != null &&
                item.status.equals("active", ignoreCase = true) &&
                buyNowPrice > currentPrice && // Цена "Купить сейчас" должна быть СТРОГО БОЛЬШЕ текущей цены
                item.seller.id != TokenManager.userId // Продавец не может купить свой лот

        binding.btnBuyNow.isVisible = canBuyNow
        if (canBuyNow) {
            binding.btnBuyNow.text = "Купить сейчас за ${FormatUtils.formatPrice(buyNowPrice!!)}"
            binding.btnBuyNow.setOnClickListener {
                showBuyNowConfirmationDialog(item)
            }
        } else {
            binding.btnBuyNow.setOnClickListener(null) // Убираем слушатель, если кнопка неактивна
        }
    }


    private fun bindBidsData(bids: List<Bid>, item: AuctionItem) {
        bidAdapter.updateData(bids)
        binding.tvNoBids.isVisible = bids.isEmpty()
        binding.rvBids.isVisible = bids.isNotEmpty()

        val highestBid = bids.firstOrNull() // Уже отсортированы по убыванию суммы, затем по времени
        val currentPrice = highestBid?.bidAmount ?: item.startPrice

        binding.tvCurrentPrice.text = FormatUtils.formatPrice(currentPrice)
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
        val auctionIsActive = item.status.equals("active", ignoreCase = true)

        if (auctionIsActive) {
            try {
                val endTime = Instant.parse(item.endTime)
                val now = Instant.now()
                val durationMillis = Duration.between(now, endTime).toMillis()

                if (durationMillis > 0) {
                    binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.md_theme_secondary, null)) // Цвет для активного таймера
                    countDownTimer = object : CountDownTimer(durationMillis, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            binding.tvTimeRemaining.text = formatDuration(millisUntilFinished)
                        }

                        override fun onFinish() {
                            binding.tvTimeRemaining.text = "Аукцион завершен"
                            binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant, null))
                            enableBidInput(false)
                            updateBuyNowButtonVisibility(item, currentItemData?.highestBid) // Обновляем кнопку
                            // viewModel.loadItemDetails(args.itemId) // Можно для автообновления
                        }
                    }.start()
                } else {
                    binding.tvTimeRemaining.text = "Аукцион завершен"
                    binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant, null))
                    binding.tvItemStatus.text = "Статус: COMPLETED"
                    // enableBidInput(false) // Уже будет false из-за статуса
                }
            } catch (e: Exception) {
                Log.e("ItemDetailFragment", "Ошибка парсинга или расчета времени", e)
                binding.tvTimeRemaining.text = "Ошибка времени"
            }
        } else {
            binding.tvTimeRemaining.text = "Статус: ${item.status.replaceFirstChar { it.uppercase() }}"
            binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant, null))
        }
        // Видимость и активность полей ввода и кнопки "Купить сейчас" управляются в bindItemData и updateBuyNowButtonVisibility
        enableBidInput(auctionIsActive)
        updateBuyNowButtonVisibility(item, currentItemData?.highestBid)
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
        if (enabled) {
            binding.etBidAmount.isEnabled = true
            binding.btnPlaceBid.isEnabled = true
        } else {
            binding.etBidAmount.isEnabled = false
            binding.btnPlaceBid.isEnabled = false
            binding.etBidAmount.text?.clear()
            binding.tilBidAmount.error = null
        }
    }

    private fun placeBid(itemId: String, currentData: ItemDetailData?) {
        val amountStr = binding.etBidAmount.text.toString()
        val bidderId = TokenManager.userId

        if (bidderId == null) {
            Toast.makeText(requireContext(), "Не удалось определить пользователя", Toast.LENGTH_SHORT).show()
            return
        }
        binding.tilBidAmount.error = null
        val amount = try { BigDecimal(amountStr) } catch (e: Exception) {
            binding.tilBidAmount.error = "Некорректная сумма"; return
        }
        if (amount <= BigDecimal.ZERO) {
            binding.tilBidAmount.error = "Сумма должна быть положительной"; return
        }

        val item = currentData?.item
        if (item == null) {
            Toast.makeText(requireContext(), "Данные лота не загружены.", Toast.LENGTH_SHORT).show(); return
        }
        if (item.seller.id == bidderId) {
            binding.tilBidAmount.error = "Продавец не может делать ставки"; return
        }

        val currentPrice = currentData.highestBid?.bidAmount ?: item.startPrice
        val minAllowedBid = currentPrice.add(item.minBidStep)

        if (amount < minAllowedBid) {
            binding.tilBidAmount.error = "Ставка должна быть не менее ${FormatUtils.formatPrice(minAllowedBid)}"; return
        }

        // Проверка на buyNowPrice при обычной ставке (если цена ставки достигает buyNowPrice)
        if (item.buyNowPrice != null && amount >= item.buyNowPrice) {
            // Предлагаем пользователю подтвердить покупку по цене "Купить сейчас"
            // или просто делаем ставку по buyNowPrice
            AlertDialog.Builder(requireContext())
                .setTitle("Достигнута цена 'Купить сейчас'")
                .setMessage("Ваша ставка достигла или превысила цену 'Купить сейчас' (${FormatUtils.formatPrice(item.buyNowPrice)}). " +
                        "Лот будет куплен по этой цене. Продолжить?")
                .setPositiveButton("Да, купить") { _, _ ->
                    viewModel.placeBid(PlaceBidRequest(bidderId, itemId, item.buyNowPrice)) // Ставка по цене buyNow
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            viewModel.placeBid(PlaceBidRequest(bidderId, itemId, amount))
        }
    }

    private fun showBuyNowConfirmationDialog(item: AuctionItem) {
        val buyNowPrice = item.buyNowPrice ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Купить сейчас")
            .setMessage("Вы уверены, что хотите купить лот '${item.title}' за ${FormatUtils.formatPrice(buyNowPrice)}?")
            .setPositiveButton("Купить") { _, _ ->
                viewModel.executeBuyNow(item.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        val itemTopic = "/topic/items/${args.itemId}/bids"
        WebSocketManager.unsubscribeFromTopic(itemTopic)
        binding.viewPagerImages.adapter = null
        _binding = null
    }
}
