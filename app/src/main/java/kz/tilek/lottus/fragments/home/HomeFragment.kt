// ./app/src/main/java/kz/tilek/lottus/fragments/home/HomeFragment.kt
package kz.tilek.lottus.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Используем viewModels делегат
import androidx.lifecycle.Observer // Импорт Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kz.tilek.lottus.R
import kz.tilek.lottus.adapters.AuctionAdapter
import kz.tilek.lottus.databinding.FragmentHomeBinding
import kz.tilek.lottus.viewmodels.HomeViewModel // Импорт ViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Получаем ViewModel через делегат
    private val viewModel: HomeViewModel by viewModels()

    // Адаптер теперь инициализируется позже, с пустым списком
    private lateinit var adapter: AuctionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView() // Настраиваем RecyclerView

        // Обработчик нажатия на FAB
        binding.fabAddAuction.setOnClickListener {
            findNavController().navigate(R.id.createAuctionFragment)
        }

        // Наблюдаем за состоянием загрузки лотов
        viewModel.activeItemsState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false // Скрываем прогрессбар по завершении

            result.onSuccess { items ->
                // Успешно загрузили, обновляем адаптер
                binding.tvEmptyList.isVisible = items.isEmpty() // Показываем/скрываем текст "пусто"
                binding.recyclerView.isVisible = items.isNotEmpty()
                adapter.updateData(items) // Передаем данные в адаптер
            }.onFailure { exception ->
                // Ошибка загрузки
                binding.tvEmptyList.isVisible = true // Показываем текст "пусто" или сообщение об ошибке
                binding.recyclerView.isVisible = false
                binding.tvEmptyList.text = "Ошибка загрузки: ${exception.message}" // Показываем ошибку
                Toast.makeText(requireContext(), "Ошибка: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        })

        // Наблюдаем за индикатором загрузки (если используем)
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading // Показываем/скрываем ProgressBar
            // Можно отключать/включать другие элементы UI во время загрузки
            // binding.fabAddAuction.isEnabled = !isLoading
        })

        // Загружаем данные при создании или принудительном обновлении
        loadData()

        // Настраиваем SwipeRefreshLayout для обновления списка
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData() // Загружаем данные снова
            binding.swipeRefreshLayout.isRefreshing = false // Останавливаем анимацию обновления
        }
    }

    private fun setupRecyclerView() {
        adapter = AuctionAdapter(emptyList()) { clickedItem ->
            // Обработка клика - навигация к деталям с передачей ID
            val action = HomeFragmentDirections.actionHomeFragmentToItemDetailFragment(clickedItem.id) // Используем Safe Args
            findNavController().navigate(action) // Переход
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    // Метод для запуска загрузки данных
    private fun loadData() {
        // Сбрасываем текст ошибки перед загрузкой
        binding.tvEmptyList.text = "Нет активных аукционов" // Возвращаем стандартный текст
        viewModel.loadActiveItems() // Вызываем метод ViewModel
    }

    // Убираем onResume, так как загрузка теперь управляется через ViewModel и SwipeRefresh
    // override fun onResume() { ... }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очищаем binding
    }
}
