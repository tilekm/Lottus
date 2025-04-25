// ./app/src/main/java/kz/tilek/lottus/fragments/auth/LoginFragment.kt
package kz.tilek.lottus.fragments.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Используем Toast для простоты, можно Snackbar
import androidx.core.view.isVisible // Для удобного управления видимостью ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Импорт для делегата
import androidx.lifecycle.Observer // Импорт Observer
import androidx.navigation.fragment.findNavController
import kz.tilek.lottus.MainActivity
import kz.tilek.lottus.R
import kz.tilek.lottus.databinding.FragmentLoginBinding
import kz.tilek.lottus.viewmodels.AuthViewModel // Импорт ViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Получаем экземпляр AuthViewModel с помощью делегата
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Удаляем старую проверку SharedPreferences при запуске ---
        // val sharedPref = requireActivity().getSharedPreferences("user_prefs", 0)
        // if (sharedPref.getBoolean("isLoggedIn", false)) {
        //     startMainActivity()
        // }
        // --- Логика перехода теперь в SplashActivity ---

        // Наблюдаем за состоянием входа из ViewModel
        viewModel.loginState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false // Скрываем ProgressBar по завершении
            binding.btnLogin.isEnabled = true // Включаем кнопку обратно

            result.onSuccess {
                // Вход успешен
                Toast.makeText(requireContext(), "Вход выполнен успешно!", Toast.LENGTH_SHORT).show()
                startMainActivity()
            }.onFailure { exception ->
                // Ошибка входа
                binding.tilUsername.error = null // Сбрасываем предыдущие ошибки
                binding.tilPassword.error = null
                // Показываем сообщение об ошибке (из исключения, которое мы формируем в репозитории)
                Toast.makeText(requireContext(), exception.message ?: "Неизвестная ошибка", Toast.LENGTH_LONG).show()
                // Можно попытаться выделить конкретное поле, если ошибка специфична, но пока общее сообщение
                // binding.tilPassword.error = exception.message
            }
        })

        // Кнопка входа
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim() // Используем username
            val password = binding.etPassword.text.toString()

            // Сбрасываем ошибки перед валидацией
            binding.tilUsername.error = null // Обновлено для username
            binding.tilPassword.error = null

            if (validateInput(username, password)) {
                // Показываем ProgressBar и отключаем кнопку
                binding.progressBar.isVisible = true
                binding.btnLogin.isEnabled = false
                // Вызываем метод ViewModel для входа
                viewModel.login(username, password)
            }
        }

        // Переход на экран регистрации
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        // Переход на экран восстановления пароля
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPassword)
        }
    }

    // Обновленная валидация для username
    private fun validateInput(username: String, password: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.tilUsername.error = "Введите имя пользователя"
            isValid = false
        } else {
            binding.tilUsername.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Введите пароль"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun startMainActivity() {
        // Убедимся, что context не null
        context?.let {
            val intent = Intent(it, MainActivity::class.java).apply {
                // Очищаем стек активностей, чтобы пользователь не мог вернуться на экран входа
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
        // requireActivity().finish() // finish() не нужен, т.к. флаги очищают стек
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
