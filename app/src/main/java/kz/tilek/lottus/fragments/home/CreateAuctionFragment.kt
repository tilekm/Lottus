// ./app/src/main/java/kz/tilek/lottus/fragments/home/CreateAuctionFragment.kt
package kz.tilek.lottus.fragments.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kz.tilek.lottus.data.TokenManager // Для получения ID продавца
import kz.tilek.lottus.databinding.FragmentCreateAuctionBinding
import kz.tilek.lottus.viewmodels.CreateAuctionViewModel // Импорт ViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Instant // Для работы с датами
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CreateAuctionFragment : Fragment() {

    private var _binding: FragmentCreateAuctionBinding? = null
    private val binding get() = _binding!!

    // Получаем ViewModel
    private val viewModel: CreateAuctionViewModel by viewModels()

    // Переменные для хранения выбранных дат и времени
    private var startDateTime: Calendar? = null
    private var endDateTime: Calendar? = null

    // Форматтер для отображения даты и времени пользователю
    private val displayDateTimeFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    // Форматтер для отправки на бэкенд (ISO 8601 UTC)
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateAuctionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Наблюдаем за состоянием создания аукциона
        viewModel.createAuctionState.observe(viewLifecycleOwner, Observer { result ->
            setLoading(false) // Скрываем загрузку
            result.onSuccess { createdItem ->
                Toast.makeText(requireContext(), "Аукцион '${createdItem.title}' успешно создан!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // Возвращаемся назад
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка создания: ${exception.message}", Toast.LENGTH_LONG).show()
                // Можно сбросить ошибки полей, если нужно
            }
        })

        // Наблюдаем за индикатором загрузки
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            setLoading(isLoading)
        })

        // Обработчики нажатия на кнопки выбора даты/времени
        binding.btnPickStartTime.setOnClickListener {
            showDateTimePicker(true) { calendar ->
                startDateTime = calendar
                binding.tvStartTimeValue.text = displayDateTimeFormatter.format(calendar.time)
                binding.tvStartTimeValue.error = null // Сброс ошибки
            }
        }
        binding.btnPickEndTime.setOnClickListener {
            showDateTimePicker(false) { calendar ->
                endDateTime = calendar
                binding.tvEndTimeValue.text = displayDateTimeFormatter.format(calendar.time)
                binding.tvEndTimeValue.error = null // Сброс ошибки
            }
        }

        // Обработчик нажатия на кнопку создания
        binding.btnSubmitAuction.setOnClickListener {
            submitAuction()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnSubmitAuction.isEnabled = !isLoading
        // Можно также отключить поля ввода
        binding.etAuctionTitle.isEnabled = !isLoading
        binding.etAuctionDescription.isEnabled = !isLoading
        binding.etStartingPrice.isEnabled = !isLoading
        binding.etMinBidStep.isEnabled = !isLoading
        binding.etBuyNowPrice.isEnabled = !isLoading
        binding.btnPickStartTime.isEnabled = !isLoading
        binding.btnPickEndTime.isEnabled = !isLoading
    }

    private fun submitAuction() {
        // Получаем ID продавца
        val sellerId = TokenManager.userId
        if (sellerId == null) {
            Toast.makeText(requireContext(), "Ошибка: Не удалось определить пользователя.", Toast.LENGTH_SHORT).show()
            return
        }

        // Сбор данных из полей
        val title = binding.etAuctionTitle.text.toString().trim()
        val description = binding.etAuctionDescription.text.toString().trim().ifEmpty { null } // null если пусто
        val startPriceStr = binding.etStartingPrice.text.toString()
        val minBidStepStr = binding.etMinBidStep.text.toString()
        val buyNowPriceStr = binding.etBuyNowPrice.text.toString()

        // Валидация и конвертация
        var isValid = true

        if (title.isEmpty()) {
            binding.tilAuctionTitle.error = "Введите название"
            isValid = false
        } else {
            binding.tilAuctionTitle.error = null
        }

        val startPrice = try {
            BigDecimal(startPriceStr).also {
                if (it <= BigDecimal.ZERO) throw NumberFormatException()
                binding.tilStartingPrice.error = null
            }
        } catch (e: Exception) {
            binding.tilStartingPrice.error = "Некорректная цена"
            isValid = false
            null
        }

        val minBidStep = try {
            BigDecimal(minBidStepStr).also {
                if (it <= BigDecimal.ZERO) throw NumberFormatException()
                binding.tilMinBidStep.error = null
            }
        } catch (e: Exception) {
            binding.tilMinBidStep.error = "Некорректный шаг"
            isValid = false
            null
        }

        val buyNowPrice = try {
            if (buyNowPriceStr.isNotBlank()) {
                BigDecimal(buyNowPriceStr).also {
                    if (it <= BigDecimal.ZERO) throw NumberFormatException()
                    binding.tilBuyNowPrice.error = null
                }
            } else {
                binding.tilBuyNowPrice.error = null
                null // Цена "Купить сейчас" опциональна
            }
        } catch (e: Exception) {
            binding.tilBuyNowPrice.error = "Некорректная цена"
            isValid = false
            null
        }

        // Проверка buyNowPrice > startPrice
        if (startPrice != null && buyNowPrice != null && buyNowPrice <= startPrice) {
            binding.tilBuyNowPrice.error = "Должна быть больше начальной цены"
            isValid = false
        }

        // Валидация времени
        val startTimeIso: String?
        val endTimeIso: String?

        if (startDateTime == null) {
            binding.tvStartTimeValue.error = "Выберите время начала" // Используем setError на TextView
            isValid = false
            startTimeIso = null
        } else {
            startTimeIso = calendarToIsoString(startDateTime!!)
            binding.tvStartTimeValue.error = null
        }

        if (endDateTime == null) {
            binding.tvEndTimeValue.error = "Выберите время окончания"
            isValid = false
            endTimeIso = null
        } else {
            endTimeIso = calendarToIsoString(endDateTime!!)
            binding.tvEndTimeValue.error = null
        }

        // Проверка endTime > startTime
        if (startDateTime != null && endDateTime != null && !endDateTime!!.after(startDateTime)) {
            binding.tvEndTimeValue.error = "Должно быть позже времени начала"
            isValid = false
        }

        // Если все валидно, вызываем ViewModel
        if (isValid && startPrice != null && minBidStep != null && startTimeIso != null && endTimeIso != null) {
            viewModel.createAuction(
                sellerId = sellerId,
                title = title,
                description = description,
                startPrice = startPrice,
                buyNowPrice = buyNowPrice, // Может быть null
                minBidStep = minBidStep,
                startTime = startTimeIso,
                endTime = endTimeIso
            )
        }
    }

    // Функция для показа DatePickerDialog, а затем TimePickerDialog
    private fun showDateTimePicker(isStart: Boolean, onDateTimeSet: (Calendar) -> Unit) {
        val currentCalendar = Calendar.getInstance()
        val targetCalendar = if (isStart) startDateTime else endDateTime
        val calendarToShow = targetCalendar ?: currentCalendar

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute, 0)
                            set(Calendar.MILLISECOND, 0) // Миллисекунды в 0
                        }
                        onDateTimeSet(selectedCalendar)
                    },
                    calendarToShow.get(Calendar.HOUR_OF_DAY),
                    calendarToShow.get(Calendar.MINUTE),
                    true // 24-часовой формат
                ).show()
            },
            calendarToShow.get(Calendar.YEAR),
            calendarToShow.get(Calendar.MONTH),
            calendarToShow.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Ограничиваем минимальную дату (например, не раньше текущего момента)
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    // Функция для конвертации Calendar в ISO 8601 строку в UTC
    private fun calendarToIsoString(calendar: Calendar): String {
        // Получаем Instant напрямую из Calendar. Он уже представляет момент времени в UTC.
        val utcInstant: Instant = calendar.toInstant()
        // Форматируем этот Instant в стандартную ISO строку (которая всегда в UTC с 'Z')
        return DateTimeFormatter.ISO_INSTANT.format(utcInstant)
        // Или можно использовать твой isoFormatter, если он настроен как DateTimeFormatter.ISO_INSTANT
        // return isoFormatter.format(utcInstant)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
