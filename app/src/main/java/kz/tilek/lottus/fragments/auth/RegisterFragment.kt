package kz.tilek.lottus.fragments.auth
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kz.tilek.lottus.databinding.FragmentRegisterBinding
import kz.tilek.lottus.viewmodels.AuthViewModel
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.registerState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false 
            binding.btnRegister.isEnabled = true 
            result.onSuccess { registeredUser ->
                Toast.makeText(requireContext(), "Регистрация успешна! (${registeredUser.username}). Теперь вы можете войти.", Toast.LENGTH_LONG).show()
                findNavController().popBackStack() 
            }.onFailure { exception ->
                binding.tilUsername.error = null 
                binding.tilEmail.error = null
                binding.tilPassword.error = null
                binding.tilConfirmPassword.error = null
                Toast.makeText(requireContext(), exception.message ?: "Неизвестная ошибка регистрации", Toast.LENGTH_LONG).show()
            }
        })
        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim() 
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            binding.tilUsername.error = null
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null
            if (validateInput(username, email, password, confirmPassword)) {
                binding.progressBar.isVisible = true
                binding.btnRegister.isEnabled = false
                viewModel.register(username, email, password)
            }
        }
    }
    private fun validateInput(username: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true
        if (username.isEmpty()) {
            binding.tilUsername.error = "Введите имя пользователя"
            isValid = false
        } else if (username.length < 3) {
            binding.tilUsername.error = "Имя пользователя должно быть не менее 3 символов"
            isValid = false
        }
        else {
            binding.tilUsername.error = null
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = "Введите email"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Некорректный формат email"
            isValid = false
        }
        else {
            binding.tilEmail.error = null
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Введите пароль"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Пароль должен быть не менее 6 символов"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Подтвердите пароль"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Пароли не совпадают"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }
        return isValid
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
