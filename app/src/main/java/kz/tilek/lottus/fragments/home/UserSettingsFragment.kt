// ./app/src/main/java/kz/tilek/lottus/fragments/home/UserSettingsFragment.kt
package kz.tilek.lottus.fragments.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kz.tilek.lottus.R
import kz.tilek.lottus.databinding.FragmentUserSettingsBinding
import kz.tilek.lottus.viewmodels.UserSettingsViewModel

class UserSettingsFragment : Fragment() {

    private var _binding: FragmentUserSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        observeViewModel()

        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        // Загружаем текущие данные при входе на экран
        viewModel.loadCurrentUserProfile()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack() // Возврат на предыдущий экран
        }
    }

    private fun observeViewModel() {
        // Наблюдение за загрузкой профиля
        viewModel.userProfileState.observe(viewLifecycleOwner, Observer { result ->
            result.onSuccess { user ->
                binding.etUsername.setText(user.username)
                binding.etEmail.setText(user.email)
                // Загрузка аватара
                Glide.with(this)
                    .load(user.profilePictureUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivAvatar)
                setInputsEnabled(true) // Включаем поля после загрузки
            }.onFailure {
                Toast.makeText(requireContext(), "Ошибка загрузки данных профиля", Toast.LENGTH_SHORT).show()
                setInputsEnabled(false) // Оставляем поля выключенными при ошибке
            }
        })

        // Наблюдение за результатом обновления
        viewModel.updateState.observe(viewLifecycleOwner, Observer { result ->
            result.onSuccess { updatedUser ->
                Toast.makeText(requireContext(), "Профиль успешно обновлен!", Toast.LENGTH_SHORT).show()
                // Можно вернуться назад автоматически
                // findNavController().popBackStack()
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка сохранения: ${exception.message}", Toast.LENGTH_LONG).show()
                // Можно подсветить поле с ошибкой, если бэкенд возвращает информацию
                // if (exception.message?.contains("username", ignoreCase = true) == true) {
                //    binding.tilUsername.error = exception.message
                // } else if (exception.message?.contains("email", ignoreCase = true) == true) {
                //    binding.tilEmail.error = exception.message
                // }
            }
        })

        // Наблюдение за общей загрузкой
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.loadProgressBar.isVisible = isLoading
            // Блокируем ввод во время первичной загрузки
            if (isLoading) setInputsEnabled(false)
        })

        // Наблюдение за процессом сохранения
        viewModel.isSaving.observe(viewLifecycleOwner, Observer { isSaving ->
            binding.saveProgressBar.isVisible = isSaving
            binding.btnSave.text = if (isSaving) "" else "Сохранить изменения"
            setInputsEnabled(!isSaving) // Блокируем ввод во время сохранения
        })
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etUsername.isEnabled = enabled
        binding.etEmail.isEnabled = enabled
        binding.btnSave.isEnabled = enabled
        // binding.btnChangeAvatar.isEnabled = enabled // Если будет кнопка смены аватара
    }

    private fun saveChanges() {
        val newUsername = binding.etUsername.text.toString()
        val newEmail = binding.etEmail.text.toString()

        // Простая валидация (можно улучшить)
        var isValid = true
        if (newUsername.isBlank()) {
            binding.tilUsername.error = "Имя пользователя не может быть пустым"
            isValid = false
        } else {
            binding.tilUsername.error = null
        }
        if (newEmail.isBlank()) {
            binding.tilEmail.error = "Email не может быть пустым"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            binding.tilEmail.error = "Некорректный формат email"
            isValid = false
        }
        else {
            binding.tilEmail.error = null
        }

        if (isValid) {
            viewModel.updateProfile(newUsername, newEmail)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
