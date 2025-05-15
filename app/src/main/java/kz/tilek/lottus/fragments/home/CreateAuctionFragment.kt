package kz.tilek.lottus.fragments.home
import android.Manifest 
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager 
import android.net.Uri 
import android.os.Build 
import android.os.Bundle
import android.util.Log 
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest 
import androidx.activity.result.contract.ActivityResultContracts 
import androidx.core.content.ContextCompat 
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope 
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager 
import kotlinx.coroutines.launch 
import kz.tilek.lottus.adapters.ImagePreviewAdapter 
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
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            if (uris.isNotEmpty()) {
                Log.d("PhotoPicker", "Выбрано URI: $uris")
                imagePreviewAdapter.addImages(uris) 
            } else {
                Log.d("PhotoPicker", "Изображения не выбраны")
            }
        }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "Разрешение на чтение медиа получено")
                launchImagePicker() 
            } else {
                Log.d("Permission", "Разрешение на чтение медиа отклонено")
                Toast.makeText(requireContext(), "Разрешение необходимо для выбора фото", Toast.LENGTH_SHORT).show()
            }
        }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateAuctionBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImagePreviewRecyclerView() 
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
        binding.btnAddPhoto.setOnClickListener {
            checkPermissionAndLaunchPicker()
        }
        binding.btnSubmitAuction.setOnClickListener {
            submitAuction()
        }
    }
    private fun setupImagePreviewRecyclerView() {
        imagePreviewAdapter = ImagePreviewAdapter { uriToRemove ->
            imagePreviewAdapter.removeImage(uriToRemove)
        }
        binding.rvImagePreviews.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imagePreviewAdapter
        }
    }
    private fun checkPermissionAndLaunchPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                launchImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                requestPermissionLauncher.launch(permission) 
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    private fun launchImagePicker() {
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnSubmitAuction.isEnabled = !isLoading
        binding.btnAddPhoto.isEnabled = !isLoading 
        binding.etAuctionTitle.isEnabled = !isLoading
        binding.etAuctionDescription.isEnabled = !isLoading
        binding.etStartingPrice.isEnabled = !isLoading
        binding.etMinBidStep.isEnabled = !isLoading
        binding.etBuyNowPrice.isEnabled = !isLoading
        binding.btnPickStartTime.isEnabled = !isLoading
        binding.btnPickEndTime.isEnabled = !isLoading
    }
    private fun submitAuction() {
        val sellerId = TokenManager.userId
        if (sellerId == null) {
            Toast.makeText(requireContext(), "Ошибка: Не удалось определить пользователя.", Toast.LENGTH_SHORT).show()
            return
        }
        val title = binding.etAuctionTitle.text.toString().trim()
        val description = binding.etAuctionDescription.text.toString().trim().ifEmpty { null }
        val startPriceStr = binding.etStartingPrice.text.toString()
        val minBidStepStr = binding.etMinBidStep.text.toString()
        val buyNowPriceStr = binding.etBuyNowPrice.text.toString()
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
                null 
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
        val imageUrisToUpload = imagePreviewAdapter.getCurrentImageUris()
        if (isValid && startPrice != null && minBidStep != null && startTimeIso != null && endTimeIso != null) {
            setLoading(true) 
            viewLifecycleOwner.lifecycleScope.launch {
                val uploadResult = viewModel.uploadImages(imageUrisToUpload)
                uploadResult.onSuccess { uploadedImageUrls ->
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
                        imageUrls = uploadedImageUrls 
                    )
                }.onFailure { uploadException ->
                    Log.e("CreateAuction", "Ошибка загрузки изображений", uploadException)
                    Toast.makeText(requireContext(), "Ошибка загрузки фото: ${uploadException.message}", Toast.LENGTH_LONG).show()
                    setLoading(false) 
                }
            }
        }
    }
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
