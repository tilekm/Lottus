// ./app/src/main/java/kz/tilek/lottus/fragments/notifications/NotificationsFragment.kt
package kz.tilek.lottus.fragments.notifications

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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kz.tilek.lottus.adapters.NotificationAdapter
import kz.tilek.lottus.databinding.FragmentNotificationsBinding
import kz.tilek.lottus.viewmodels.NotificationViewModel
import kz.tilek.lottus.websocket.WebSocketManager // Для прослушивания новых уведомлений

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // Наблюдаем за списком уведомлений
        viewModel.notificationsState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false
            binding.swipeRefreshLayout.isRefreshing = false // Останавливаем обновление

            result.onSuccess { notifications ->
                binding.tvEmptyNotifications.isVisible = notifications.isEmpty()
                binding.rvNotifications.isVisible = notifications.isNotEmpty()
                adapter.updateData(notifications)
            }.onFailure { exception ->
                binding.tvEmptyNotifications.isVisible = true
                binding.rvNotifications.isVisible = false
                binding.tvEmptyNotifications.text = "Ошибка загрузки: ${exception.message}"
                Toast.makeText(requireContext(), "Ошибка: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        })

        // Наблюдаем за состоянием загрузки
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // Показываем ProgressBar только при первичной загрузке, не при обновлении через SwipeRefresh
            if (!binding.swipeRefreshLayout.isRefreshing) {
                binding.progressBar.isVisible = isLoading
            }
        })

        // Наблюдаем за результатом пометки как прочитанного (опционально)
        viewModel.markAsReadState.observe(viewLifecycleOwner, Observer { result ->
            result.onFailure { exception ->
                Toast.makeText(requireContext(), "Не удалось отметить: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
            // При успехе список перезагрузится
        })

        // Наблюдаем за новыми уведомлениями из WebSocket
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            WebSocketManager.notificationMessagesFlow.collectLatest { notification ->
                if (notification != null) {
                    Log.d("NotificationsFragment", "Получено новое уведомление через WS: ${notification.message}")
                    // Просто перезагружаем список, чтобы показать новое уведомление
                    // TODO: Можно реализовать более умное добавление в начало списка без полной перезагрузки
                    viewModel.loadNotifications()
                    // Сбрасываем значение в Flow, чтобы не обрабатывать повторно
                    // WebSocketManager.notificationMessagesFlow.value = null // Осторожно с этим, если Flow используется где-то еще
                }
            }
        }

        // Настраиваем SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadNotifications() // Загружаем данные при обновлении
        }

        // Загружаем уведомления при первом запуске
        viewModel.loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(emptyList()) { notification ->
            // Обработчик клика - помечаем как прочитанное
            viewModel.markNotificationAsRead(notification.id)
        }
        binding.rvNotifications.adapter = adapter
        // LayoutManager уже задан в XML
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
