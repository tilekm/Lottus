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
import com.bumptech.glide.Glide // Добавь зависимость Glide или Picasso
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kz.tilek.lottus.R
import kz.tilek.lottus.adapters.BidAdapter
import kz.tilek.lottus.api.PlaceBidRequest
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.databinding.FragmentItemDetailBinding
import kz.tilek.lottus.models.AuctionItem
import kz.tilek.lottus.models.Bid
import kz.tilek.lottus.viewmodels.ItemDetailData
import kz.tilek.lottus.viewmodels.ItemDetailViewModel
import kz.tilek.lottus.websocket.WebSocketManager
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemDetailViewModel by viewModels()
    private val args: ItemDetailFragmentArgs by navArgs() // Получаем itemId через Safe Args

    private lateinit var bidAdapter: BidAdapter
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
        setupRecyclerView()

        val itemId = args.itemId // Получаем ID лота из аргументов
        val itemTopic = "/topic/items/$itemId/bids"

        WebSocketManager.subscribeToTopic(itemTopic)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                state = androidx.lifecycle.Lifecycle.State.STARTED
            ) {
                WebSocketManager.bidMessagesFlow.collectLatest { bidMessage ->
                    if (bidMessage != null && bidMessage.itemId == itemId) {
                        // Получили новую ставку для НАШЕГО лота
                        Log.d("ItemDetailFragment", "Новая ставка через WS: ${bidMessage.bidAmount}")
                        // Обновляем UI (можно перезапросить все данные или обновить частично)
                        // Простой вариант: перезапросить все детали
                        viewModel.loadItemDetails(itemId)
                        // TODO: Более сложный вариант: обновить только список ставок и текущую цену,
                        // не дергая весь запрос /api/items/{id}
                        // Например, добавить ставку в текущий список и обновить адаптер/цену.
                        Toast.makeText(requireContext(), "Новая ставка: ${bidMessage.bidAmount} ₸ от ${bidMessage.bidderUsername}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            WebSocketManager.connectionState.collectLatest { state ->
                Log.d("ItemDetailFragment", "WebSocket Connection State: $state")
                // Можно показывать индикатор статуса соединения в UI
            }
        }

        // Наблюдаем за состоянием загрузки деталей
        viewModel.itemDetailState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false // Скрываем прогрессбар
            enableBidInput(true) // Включаем ввод ставки

            result.onSuccess { data ->
                currentItemData = data // Сохраняем текущие данные
                bindItemData(data.item)
                bindBidsData(data.bids, data.item) // Передаем item для определения мин. ставки
                startTimerIfNeeded(data.item)
            }.onFailure { exception ->
                currentItemData = null
                Toast.makeText(requireContext(), "Ошибка загрузки: ${exception.message}", Toast.LENGTH_LONG).show()
                // Можно показать сообщение об ошибке в UI
                binding.collapsingToolbar.title = "Ошибка"
            }
        })

        // Наблюдаем за индикатором общей загрузки
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading
            // Скрываем/показываем основные блоки во время загрузки
            binding.appBarLayout.isVisible = !isLoading
            binding.llPlaceBid.isVisible = !isLoading
        })

        // Наблюдаем за состоянием размещения ставки
        viewModel.placeBidState.observe(viewLifecycleOwner, Observer { result ->
            binding.btnPlaceBid.isEnabled = true // Включаем кнопку обратно
            binding.etBidAmount.isEnabled = true

            result.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка ставки: ${exception.message}", Toast.LENGTH_LONG).show()
                binding.tilBidAmount.error = exception.message // Показываем ошибку у поля ввода
            }
            // При успехе данные перезагрузятся через itemDetailState
        })

        // Наблюдаем за индикатором загрузки ставки
        viewModel.isPlacingBid.observe(viewLifecycleOwner, Observer { isPlacing ->
            binding.btnPlaceBid.isEnabled = !isPlacing
            binding.etBidAmount.isEnabled = !isPlacing
            binding.tilBidAmount.error = null // Сбрасываем ошибку при начале отправки
            if (isPlacing) {
                binding.btnPlaceBid.text = "Отправка..." // Меняем текст кнопки
            } else {
                binding.btnPlaceBid.text = "Ставка"
            }
        })

        // Обработчик кнопки "Сделать ставку"
        binding.btnPlaceBid.setOnClickListener {
            placeBid(itemId, currentItemData)
        }

        // Загружаем данные
        viewModel.loadItemDetails(itemId)
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack() // Возврат назад по кнопке в Toolbar
        }
        // Сначала устанавливаем пустой заголовок, он заполнится данными
        binding.collapsingToolbar.title = " "
    }

    private fun setupRecyclerView() {
        bidAdapter = BidAdapter(emptyList())
        binding.rvBids.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBids.adapter = bidAdapter
        binding.rvBids.isNestedScrollingEnabled = false // Отключаем свою прокрутку
    }

    private fun bindItemData(item: AuctionItem) {
        binding.collapsingToolbar.title = item.title // Заголовок в Toolbar
        binding.tvItemTitle.text = item.title

        // Отображаем цены (текущую/начальную)
        // Логика отображения текущей цены будет в bindBidsData

        binding.tvStartPrice.text = "Начальная: ${item.startPrice} ₸"
        binding.tvMinStep.text = "Мин. шаг: ${item.minBidStep} ₸"

        binding.tvItemDescription.text = item.description ?: "Нет описания"
        binding.tvSellerUsername.text = "${item.seller.username} (Рейтинг: ${item.seller.rating})" // Добавим рейтинг
        binding.tvItemStatus.text = "Статус: ${item.status.replaceFirstChar { it.titlecase(Locale.getDefault()) }}"

        // Кнопка "Купить сейчас"
        if (item.buyNowPrice != null && item.status.equals("active", ignoreCase = true)) {
            binding.btnBuyNow.isVisible = true
            binding.btnBuyNow.text = "Купить сейчас за ${item.buyNowPrice} ₸"
            binding.btnBuyNow.setOnClickListener {
                // Вызываем метод для покупки сейчас
                buyNow(item)
            }
        } else {
            binding.btnBuyNow.isVisible = false
        }

        // Загрузка изображения (замени на реальный URL, если он будет в API)
        // val imageUrl = item.imageUrl ?: R.drawable.ic_image_placeholder // Используй плейсхолдер
        // Glide.with(this).load(imageUrl).into(binding.ivItemImage)
        binding.ivItemImage.setImageResource(R.drawable.app_logo) // Временная заглушка

        // Включаем/отключаем ввод ставки в зависимости от статуса
        enableBidInput(item.status.equals("active", ignoreCase = true))
    }

    private fun bindBidsData(bids: List<Bid>, item: AuctionItem) {
        bidAdapter.updateData(bids)
        binding.tvNoBids.isVisible = bids.isEmpty()
        binding.rvBids.isVisible = bids.isNotEmpty()

        // Обновляем текущую цену и лидера
        val highestBid = bids.firstOrNull()
        val currentPrice = highestBid?.bidAmount ?: item.startPrice // Текущая цена - макс. ставка или стартовая

        binding.tvCurrentPrice.text = "${currentPrice} ₸"
        if (highestBid != null) {
            binding.tvHighestBidder.text = "(от ${highestBid.bidder.username})"
            binding.tvHighestBidder.isVisible = true
        } else {
            binding.tvHighestBidder.isVisible = false
        }

        // Рассчитываем и показываем минимально допустимую следующую ставку в hint
        val minNextBid = currentPrice.add(item.minBidStep)
        binding.tilBidAmount.hint = "Мин. след. ставка: $minNextBid ₸"
    }

    private fun startTimerIfNeeded(item: AuctionItem) {
        countDownTimer?.cancel() // Отменяем предыдущий таймер, если был

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
                            binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.gray, null))
                            enableBidInput(false) // Отключаем ввод ставки
                            // Можно добавить авто-обновление данных
                            // viewModel.loadItemDetails(args.itemId)
                        }
                    }.start()
                } else {
                    binding.tvTimeRemaining.text = "Аукцион завершен"
                    binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.gray, null))
                    enableBidInput(false)
                }
            } catch (e: Exception) {
                binding.tvTimeRemaining.text = "Ошибка времени"
                enableBidInput(false)
            }
        } else {
            binding.tvTimeRemaining.text = "Статус: ${item.status.replaceFirstChar { it.titlecase(Locale.getDefault()) }}"
            binding.tvTimeRemaining.setTextColor(resources.getColor(R.color.gray, null))
            enableBidInput(false)
        }
    }

    // Форматирует миллисекунды в строку "Xd Yh Zm Ss"
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

        // --- Валидация ---
        binding.tilBidAmount.error = null // Сброс предыдущей ошибки

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

        // Проверка минимальной допустимой ставки
        if (currentData != null) {
            val item = currentData.item
            val currentPrice = currentData.highestBid?.bidAmount ?: item.startPrice
            val minAllowedBid = currentPrice.add(item.minBidStep)

            if (amount < minAllowedBid) {
                binding.tilBidAmount.error = "Ставка должна быть не менее $minAllowedBid ₸"
                return
            }

            // Проверка, не является ли ставящий продавцом
            if (item.seller.id == bidderId) {
                binding.tilBidAmount.error = "Продавец не может делать ставки"
                return
            }

        } else {
            // Не удалось получить текущие данные для валидации
            Toast.makeText(requireContext(), "Не удалось проверить минимальную ставку. Попробуйте обновить.", Toast.LENGTH_SHORT).show()
            return
        }
        // --- Конец валидации ---

        // Вызов ViewModel
        viewModel.placeBid(PlaceBidRequest(bidderId, itemId, amount))
    }

    // Новый метод для обработки "Купить сейчас"
    private fun buyNow(item: AuctionItem) {
        val bidderId = TokenManager.userId
        val buyNowPrice = item.buyNowPrice

        if (bidderId == null || buyNowPrice == null) {
            Toast.makeText(requireContext(), "Невозможно выполнить покупку", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка, не является ли покупатель продавцом
        if (item.seller.id == bidderId) {
            Toast.makeText(requireContext(), "Продавец не может купить свой лот", Toast.LENGTH_SHORT).show()
            return
        }

        // Диалог подтверждения
        AlertDialog.Builder(requireContext())
            .setTitle("Купить сейчас")
            .setMessage("Вы уверены, что хотите купить лот '${item.title}' за ${buyNowPrice} ₸?")
            .setPositiveButton("Купить") { _, _ ->
                // Отправляем ставку, равную цене "Купить сейчас"
                viewModel.placeBid(PlaceBidRequest(bidderId, item.id, buyNowPrice))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel() // Останавливаем таймер при уничтожении View
        val itemTopic = "/topic/items/${args.itemId}/bids"
        WebSocketManager.unsubscribeFromTopic(itemTopic)
        _binding = null
    }
}
