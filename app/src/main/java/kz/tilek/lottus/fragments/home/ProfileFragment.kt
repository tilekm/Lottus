// ./app/src/main/java/kz/tilek/lottus/fragments/home/ProfileFragment.kt
package kz.tilek.lottus.fragments.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController // Импорт для навигации
import com.bumptech.glide.Glide // Импорт Glide
import kz.tilek.lottus.AuthActivity
import kz.tilek.lottus.R // Импорт R для доступа к ID
import kz.tilek.lottus.databinding.FragmentProfileBinding
import kz.tilek.lottus.viewmodels.AuthViewModel
import kz.tilek.lottus.viewmodels.ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileViewModel.userProfileState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false

            result.onSuccess { user ->
                binding.tvUsername.text = user.username
                binding.tvUserEmail.text = user.email

                // Загрузка аватара с помощью Glide
                Glide.with(this)
                    .load(user.profilePictureUrl) // Загружаем URL
                    .placeholder(R.drawable.ic_person) // Плейсхолдер по умолчанию
                    .error(R.drawable.ic_person) // Плейсхолдер при ошибке
                    .circleCrop() // Делаем изображение круглым
                    .into(binding.ivProfileAvatar)

                // binding.tvUserRating.text = "Рейтинг: ${user.rating}"
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка загрузки профиля: ${exception.message}", Toast.LENGTH_LONG).show()
                binding.tvUsername.text = "Не удалось загрузить"
                binding.tvUserEmail.text = ""
                binding.ivProfileAvatar.setImageResource(R.drawable.ic_person) // Сброс аватара
            }
        })

        profileViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.ivProfileAvatar.isVisible = !isLoading
            binding.tvUsername.isVisible = !isLoading
            binding.tvUserEmail.isVisible = !isLoading
            binding.btnEditProfile.isEnabled = !isLoading // Отключаем кнопку редактирования
            binding.btnLogout.isEnabled = !isLoading
        })

        profileViewModel.loadCurrentUserProfile()

        // --- НОВЫЙ ОБРАБОТЧИК ---
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_userSettingsFragment)
        }
        // -----------------------

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { _, _ ->
                authViewModel.logout()
                context?.let {
                    val intent = Intent(it, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
