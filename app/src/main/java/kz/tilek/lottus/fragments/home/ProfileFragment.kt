// ./app/src/main/java/kz/tilek/lottus/fragments/home/ProfileFragment.kt
package kz.tilek.lottus.fragments.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
// import androidx.appcompat.app.AppCompatActivity // Больше не нужен
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Используем activityViewModels для AuthViewModel
import androidx.fragment.app.viewModels // Используем viewModels для ProfileViewModel
import androidx.lifecycle.Observer
import kz.tilek.lottus.AuthActivity
import kz.tilek.lottus.databinding.FragmentProfileBinding
import kz.tilek.lottus.viewmodels.AuthViewModel // Импорт AuthViewModel
import kz.tilek.lottus.viewmodels.ProfileViewModel // Импорт ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ViewModel для данных профиля
    private val profileViewModel: ProfileViewModel by viewModels()
    // ViewModel для аутентификации (используем activityViewModels, чтобы получить тот же экземпляр, что и в других фрагментах/активити)
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Наблюдаем за состоянием загрузки профиля
        profileViewModel.userProfileState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false // Скрываем прогрессбар

            result.onSuccess { user ->
                // Успешно загрузили, обновляем UI
                binding.tvUsername.text = user.username
                binding.tvUserEmail.text = user.email
                // TODO: Загрузить аватар, если он есть (например, с помощью Glide/Picasso)
                // binding.tvUserRating.text = "Рейтинг: ${user.rating}" // Если добавили рейтинг
            }.onFailure { exception ->
                // Ошибка загрузки профиля
                Toast.makeText(requireContext(), "Ошибка загрузки профиля: ${exception.message}", Toast.LENGTH_LONG).show()
                // Можно показать заглушки или сообщение об ошибке в UI
                binding.tvUsername.text = "Не удалось загрузить"
                binding.tvUserEmail.text = ""
            }
        })

        // Наблюдаем за индикатором загрузки
        profileViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading
            // Можно скрыть основные элементы UI во время загрузки
            binding.ivProfileAvatar.isVisible = !isLoading
            binding.tvUsername.isVisible = !isLoading
            binding.tvUserEmail.isVisible = !isLoading
            binding.btnLogout.isEnabled = !isLoading
        })

        // Загружаем данные текущего пользователя при создании фрагмента
        profileViewModel.loadCurrentUserProfile()

        // Обработчик кнопки выхода
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { _, _ ->
                // Вызываем метод logout из AuthViewModel
                authViewModel.logout()

                // Переходим на экран аутентификации
                // Убедимся, что context не null
                context?.let {
                    val intent = Intent(it, AuthActivity::class.java).apply {
                        // Очищаем стек активностей
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
                // activity?.finish() // finish() не нужен
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
