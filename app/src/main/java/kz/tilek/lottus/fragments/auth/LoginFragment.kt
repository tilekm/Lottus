package kz.tilek.lottus.fragments.auth
import android.content.Intent
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
import kz.tilek.lottus.MainActivity
import kz.tilek.lottus.R
import kz.tilek.lottus.databinding.FragmentLoginBinding
import kz.tilek.lottus.viewmodels.AuthViewModel 
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loginState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false 
            binding.btnLogin.isEnabled = true 
            result.onSuccess {
                Toast.makeText(requireContext(), "Вход выполнен успешно!", Toast.LENGTH_SHORT).show()
                startMainActivity()
            }.onFailure { exception ->
                binding.tilUsername.error = null 
                binding.tilPassword.error = null
                Toast.makeText(requireContext(), exception.message ?: "Неизвестная ошибка", Toast.LENGTH_LONG).show()
            }
        })
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim() 
            val password = binding.etPassword.text.toString()
            binding.tilUsername.error = null 
            binding.tilPassword.error = null
            if (validateInput(username, password)) {
                binding.progressBar.isVisible = true
                binding.btnLogin.isEnabled = false
                viewModel.login(username, password)
            }
        }
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPassword)
        }
    }
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
        context?.let {
            val intent = Intent(it, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
