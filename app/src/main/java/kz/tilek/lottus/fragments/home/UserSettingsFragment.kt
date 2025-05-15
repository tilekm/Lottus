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
        viewModel.loadCurrentUserProfile()
    }
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack() 
        }
    }
    private fun observeViewModel() {
        viewModel.userProfileState.observe(viewLifecycleOwner, Observer { result ->
            result.onSuccess { user ->
                binding.etUsername.setText(user.username)
                binding.etEmail.setText(user.email)
                Glide.with(this)
                    .load(user.profilePictureUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivAvatar)
                setInputsEnabled(true) 
            }.onFailure {
                Toast.makeText(requireContext(), "Ошибка загрузки данных профиля", Toast.LENGTH_SHORT).show()
                setInputsEnabled(false) 
            }
        })
        viewModel.updateState.observe(viewLifecycleOwner, Observer { result ->
            result.onSuccess { updatedUser ->
                Toast.makeText(requireContext(), "Профиль успешно обновлен!", Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка сохранения: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        })
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.loadProgressBar.isVisible = isLoading
            if (isLoading) setInputsEnabled(false)
        })
        viewModel.isSaving.observe(viewLifecycleOwner, Observer { isSaving ->
            binding.saveProgressBar.isVisible = isSaving
            binding.btnSave.text = if (isSaving) "" else "Сохранить изменения"
            setInputsEnabled(!isSaving) 
        })
    }
    private fun setInputsEnabled(enabled: Boolean) {
        binding.etUsername.isEnabled = enabled
        binding.etEmail.isEnabled = enabled
        binding.btnSave.isEnabled = enabled
    }
    private fun saveChanges() {
        val newUsername = binding.etUsername.text.toString()
        val newEmail = binding.etEmail.text.toString()
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
