// ./app/src/main/java/kz/tilek/lottus/fragments/home/CreateAuctionFragment.kt
package kz.tilek.lottus.fragments.home

import android.Manifest // <-- Импорт
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager // <-- Импорт
import android.net.Uri // <-- Импорт
import android.os.Build // <-- Импорт
import android.os.Bundle
import android.util.Log // <-- Импорт
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest // <-- Импорт
import androidx.activity.result.contract.ActivityResultContracts // <-- Импорт
import androidx.core.content.ContextCompat // <-- Импорт
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope // <-- Импорт
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager // <-- Импорт
import kotlinx.coroutines.launch // <-- Импорт
import kz.tilek.lottus.adapters.ImagePreviewAdapter // <-- Импорт
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.databinding.FragmentCreateAuctionBinding
import kz.tilek.lottus.viewmodels.CreateAuctionViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class CreateAuctionFragment : Fragment() {

    private var _binding: FragmentCreateAuctionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateAuctionViewModel by viewModels()

    private var startDateTime: Calendar? = null
    private var endDateTime: Calendar? = null

    private val displayDateTimeFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    // --- НОВЫЕ ПОЛЯ ---
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter

    // ActivityResultLauncher для выбора нескольких изображений
    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            // Callback срабатывает, когда пользователь выбрал изображения (или закрыл выбор)
            if (uris.isNotEmpty()) {
                Log.d("PhotoPicker", "Выбрано URI: $uris")
                imagePreviewAdapter.addImages(uris) // Добавляем в адаптер
            } else {
                Log.d("PhotoPicker", "Изображения не выбраны")
            }
        }

    // ActivityResultLauncher для запроса разрешений
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "Разрешение на чтение медиа получено")
                launchImagePicker() // Запускаем выбор после получения разрешения
            } else {
                Log.d("Permission", "Разрешение на чтение медиа отклонено")
                Toast.makeText(requireContext(), "Разрешение необходимо для выбора фото", Toast.LENGTH_SHORT).show()
            }
        }
    // -----------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateAuctionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupImagePreviewRecyclerView() // Настраиваем RecyclerView для превью

        // --- Наблюдение за ViewModel (без изменений) ---
        viewModel.createAuctionState.observe(viewLifecycleOwner, Observer { result ->
            setLoading(false)
            result.onSuccess { createdItem ->
                Toast.makeText(requireContext(), "Аукцион '${createdItem.title}' успешно создан!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка создания: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        })
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            setLoading(isLoading)
        })
        // ---------------------------------------------

        // --- Обработчики выбора времени (без изменений) ---
        binding.btnPickStartTime.setOnClickListener {
            showDateTimePicker(true) { calendar ->
                startDateTime = calendar
                binding.tvStartTimeValue.text = displayDateTimeFormatter.format(calendar.time)
                binding.tvStartTimeValue.error = null
            }
        }
        binding.btnPickEndTime.setOnClickListener {
            showDateTimePicker(false) { calendar ->
                endDateTime = calendar
                binding.tvEndTimeValue.text = displayDateTimeFormatter.format(calendar.time)
                binding.tvEndTimeValue.error = null
            }
        }
        // ---------------------------------------------

        // --- НОВЫЙ ОБРАБОТЧИК ---
        // Обработчик кнопки "Добавить фото"
        binding.btnAddPhoto.setOnClickListener {
            checkPermissionAndLaunchPicker()
        }
        // -----------------------

        // Обработчик кнопки создания
        binding.btnSubmitAuction.setOnClickListener {
            submitAuction()
        }
    }

    private fun setupImagePreviewRecyclerView() {
        imagePreviewAdapter = ImagePreviewAdapter { uriToRemove ->
            // Обработка удаления из превью
            imagePreviewAdapter.removeImage(uriToRemove)
        }
        binding.rvImagePreviews.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imagePreviewAdapter
        }
    }

    // --- НОВЫЕ МЕТОДЫ ---
    private fun checkPermissionAndLaunchPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                // Разрешение уже есть, запускаем выбор
                launchImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Показать объяснение пользователю, почему нужно разрешение (опционально)
                // Можно показать диалог перед запросом
                requestPermissionLauncher.launch(permission) // Запрашиваем разрешение
            }
            else -> {
                // Запрашиваем разрешение напрямую
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun launchImagePicker() {
        // Запускаем системный выборщик фото, запрашивая только изображения
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // -----------------

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnSubmitAuction.isEnabled = !isLoading
        binding.btnAddPhoto.isEnabled = !isLoading // Отключаем кнопку добавления фото
        // ... (отключение остальных полей) ...
        binding.etAuctionTitle.isEnabled = !isLoading
        binding.etAuctionDescription.isEnabled = !isLoading
        binding.etStartingPrice.isEnabled = !isLoading
        binding.etMinBidStep.isEnabled = !isLoading
        binding.etBuyNowPrice.isEnabled = !isLoading
        binding.btnPickStartTime.isEnabled = !isLoading
        binding.btnPickEndTime.isEnabled = !isLoading
    }

    // --- ИЗМЕНЕННЫЙ МЕТОД submitAuction ---
    private fun submitAuction() {
        val sellerId = TokenManager.userId
        if (sellerId == null) {
            Toast.makeText(requireContext(), "Ошибка: Не удалось определить пользователя.", Toast.LENGTH_SHORT).show()
            return
        }

        // Сбор и валидация текстовых данных (как раньше)
        val title = binding.etAuctionTitle.text.toString().trim()
        val description = binding.etAuctionDescription.text.toString().trim().ifEmpty { null }
        val startPriceStr = binding.etStartingPrice.text.toString()
        val minBidStepStr = binding.etMinBidStep.text.toString()
        val buyNowPriceStr = binding.etBuyNowPrice.text.toString()

        var isValid = true
        // ... (вся валидация полей title, startPrice, minBidStep, buyNowPrice, startTime, endTime как раньше) ...
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

        if (startPrice != null && buyNowPrice != null && buyNowPrice <= startPrice) {
            binding.tilBuyNowPrice.error = "Должна быть больше начальной цены"
            isValid = false
        }

        val startTimeIso: String?
        val endTimeIso: String?

        if (startDateTime == null) {
            binding.tvStartTimeValue.error = "Выберите время начала"
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

        if (startDateTime != null && endDateTime != null && !endDateTime!!.after(startDateTime)) {
            binding.tvEndTimeValue.error = "Должно быть позже времени начала"
            isValid = false
        }

        // Получаем список URI из адаптера
        val imageUrisToUpload = imagePreviewAdapter.getCurrentImageUris()

        // Если все валидно, начинаем процесс: сначала загрузка фото, потом создание лота
        if (isValid && startPrice != null && minBidStep != null && startTimeIso != null && endTimeIso != null) {
            setLoading(true) // Показываем индикатор загрузки

            // Запускаем корутину для загрузки изображений и создания лота
            viewLifecycleOwner.lifecycleScope.launch {
                // Шаг 1: Загрузить изображения
                val uploadResult = viewModel.uploadImages(imageUrisToUpload)

                uploadResult.onSuccess { uploadedImageUrls ->
                    // Шаг 2: Если изображения загружены успешно (или их не было), создаем лот
                    Log.d("CreateAuction", "Изображения загружены: $uploadedImageUrls")
                    viewModel.createAuction(
                        sellerId = sellerId,
                        title = title,
                        description = description,
                        startPrice = startPrice,
                        buyNowPrice = buyNowPrice,
                        minBidStep = minBidStep,
                        startTime = startTimeIso,
                        endTime = endTimeIso,
                        imageUrls = uploadedImageUrls // Передаем полученные URL
                    )
                    // setLoading(false) будет вызван в Observer для createAuctionState
                }.onFailure { uploadException ->
                    // Ошибка загрузки изображений
                    Log.e("CreateAuction", "Ошибка загрузки изображений", uploadException)
                    Toast.makeText(requireContext(), "Ошибка загрузки фото: ${uploadException.message}", Toast.LENGTH_LONG).show()
                    setLoading(false) // Скрываем индикатор
                }
            }
        }
    }
    // ---------------------------------------

    // showDateTimePicker и calendarToIsoString без изменений
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
                            set(Calendar.MILLISECOND, 0)
                        }
                        onDateTimeSet(selectedCalendar)
                    },
                    calendarToShow.get(Calendar.HOUR_OF_DAY),
                    calendarToShow.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendarToShow.get(Calendar.YEAR),
            calendarToShow.get(Calendar.MONTH),
            calendarToShow.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun calendarToIsoString(calendar: Calendar): String {
        val utcInstant: Instant = calendar.toInstant()
        return DateTimeFormatter.ISO_INSTANT.format(utcInstant)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
