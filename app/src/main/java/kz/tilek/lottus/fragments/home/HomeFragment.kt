// ./app/src/main/java/kz/tilek/lottus/fragments/home/HomeFragment.kt
// ИЗМЕНЕННЫЙ ФАЙЛ (фрагмент observeViewModel)
package kz.tilek.lottus.fragments.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.tilek.lottus.R
import kz.tilek.lottus.adapters.AuctionAdapter
import kz.tilek.lottus.adapters.LoadingStateAdapter
import kz.tilek.lottus.databinding.FragmentHomeBinding
import kz.tilek.lottus.viewmodels.AuctionFilterType
import kz.tilek.lottus.viewmodels.HomeViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var auctionAdapter: AuctionAdapter
    private lateinit var loadingStateAdapter: LoadingStateAdapter
    private lateinit var concatAdapter: ConcatAdapter
    private var searchView: SearchView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchView()
        setupRecyclerView()
        setupChipGroup()
        setupFab()
        setupSwipeRefresh()
        observeViewModel()

        // Начальная проверка состояния чипов и загрузка, если необходимо
        val initialFilter = viewModel.getCurrentFilter()
        when (initialFilter) {
            AuctionFilterType.ACTIVE -> binding.chipGroupFilter.check(R.id.chipActive)
            AuctionFilterType.SCHEDULED -> binding.chipGroupFilter.check(R.id.chipScheduled)
            AuctionFilterType.ALL -> binding.chipGroupFilter.check(R.id.chipAll)
        }
        // ViewModel сама загрузит данные в init. Дополнительный вызов loadItems здесь обычно не нужен,
        // если только нет специфичной логики для восстановления состояния.
        // Если список пуст и состояние Idle, ViewModel уже должна была попытаться загрузить.
        // Можно добавить проверку, если ViewModel.itemsList.value.isNullOrEmpty() && viewModel.loadState.value == HomeViewModel.LoadState.Idle
        // то viewModel.loadItems(isRefresh = true)
        // Но это может привести к двойной загрузке, если init еще не отработал.
        // Лучше положиться на init ViewModel.
    }

    private fun setupSearchView() {
        searchView = binding.toolbarHome.findViewById(R.id.searchViewHome)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.performSearchNow(query)
                searchView?.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchTerm(newText)
                return true
            }
        })

        val currentSearchQuery = viewModel.getCurrentSearchTerm()
        if (!currentSearchQuery.isNullOrEmpty()) {
            searchView?.setQuery(currentSearchQuery, false)
            searchView?.isIconified = false
            // searchView?.clearFocus() // Не всегда нужно убирать фокус при восстановлении
        } else {
            searchView?.isIconified = true
        }

        searchView?.setOnCloseListener {
            viewModel.performSearchNow(null) // Используем performSearchNow для немедленной перезагрузки
            false
        }
    }


    private fun setupRecyclerView() {
        auctionAdapter = AuctionAdapter { clickedItem ->
            val action = HomeFragmentDirections.actionHomeFragmentToItemDetailFragment(clickedItem.id)
            findNavController().navigate(action)
        }
        loadingStateAdapter = LoadingStateAdapter()
        concatAdapter = ConcatAdapter(auctionAdapter, loadingStateAdapter)

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = concatAdapter
        // Убираем отступ снизу из RecyclerView, если FAB перекрывает контент,
        // лучше добавить padding к ConstraintLayout или SwipeRefreshLayout, если нужно.
        // binding.recyclerView.setPadding(0,0,0,0) // Пример, если нужно убрать все отступы

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                val isLoading = viewModel.isLoadingMore.value ?: false
                val isNotLoadingState = viewModel.loadState.value !is HomeViewModel.LoadState.Loading

                if (!isLoading && isNotLoadingState) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 // Увеличил порог для более ранней загрузки
                        && firstVisibleItemPosition >= 0
                        && totalItemCount > 0 // Убедимся, что список не пуст
                        && auctionAdapter.itemCount == totalItemCount - (if (loadingStateAdapter.itemCount > 0) 1 else 0) // Проверяем, что totalItemCount соответствует элементам + футер
                    ) {
                        viewModel.loadItems(isRefresh = false)
                    }
                }
            }
        })
    }

    private fun setupChipGroup() {
        // Устанавливаем начальное состояние чипов в соответствии с ViewModel
        // Это должно быть сделано до установки слушателя, чтобы избежать лишнего вызова при инициализации
        val currentVmFilter = viewModel.getCurrentFilter()
        val initialChipId = when (currentVmFilter) {
            AuctionFilterType.ACTIVE -> R.id.chipActive
            AuctionFilterType.SCHEDULED -> R.id.chipScheduled
            AuctionFilterType.ALL -> R.id.chipAll
            else -> R.id.chipActive // По умолчанию
        }
        if (binding.chipGroupFilter.checkedChipId != initialChipId) {
            binding.chipGroupFilter.check(initialChipId)
        }


        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = if (checkedIds.isNotEmpty()) checkedIds[0] else View.NO_ID

            val newFilter = when (checkedId) {
                R.id.chipActive -> AuctionFilterType.ACTIVE
                R.id.chipScheduled -> AuctionFilterType.SCHEDULED
                R.id.chipAll -> AuctionFilterType.ALL
                View.NO_ID -> {
                    // Если каким-то образом все чипы были сняты, принудительно выбираем "Активные"
                    // и используем этот фильтр.
                    Log.w("HomeFragment", "Все чипы сняты, принудительно выбираем 'Активные'")
                    binding.chipGroupFilter.check(R.id.chipActive) // Принудительно выбрать "Активные"
                    AuctionFilterType.ACTIVE // Установить фильтр на "Активные"
                }
                else -> viewModel.getCurrentFilter() // Не должно произойти
            }

            // Вызываем setFilter, только если фильтр действительно изменился,
            // или если был активен поисковый запрос (чтобы сбросить его).
            // viewModel.setFilter сама проверит, нужно ли делать loadItems.
            if (viewModel.getCurrentFilter() != newFilter || !viewModel.getCurrentSearchTerm().isNullOrEmpty() || checkedId == View.NO_ID) {
                // Если был активен поиск, очищаем поле поиска в UI
                if (searchView != null && !viewModel.getCurrentSearchTerm().isNullOrEmpty()) {
                    searchView?.setQuery("", false)
                    searchView?.isIconified = true
                }
                viewModel.setFilter(newFilter) // ViewModel сама сбросит currentSearchTerm
            }
        }
    }

    private fun setupFab() {
        binding.fabAddAuction.setOnClickListener {
            findNavController().navigate(R.id.createAuctionFragment)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadItems(isRefresh = true)
        }
    }

    private fun observeViewModel() {
        viewModel.itemsList.observe(viewLifecycleOwner, Observer { items ->
            // Передаем копию списка в адаптер, чтобы избежать проблем с модификацией
            auctionAdapter.submitList(items.toList()) {
                // Этот блок выполнится ПОСЛЕ того, как DiffUtil отработает
                // и RecyclerView обновится. Здесь auctionAdapter.itemCount будет актуальным.

                val currentLoadState = viewModel.loadState.value
                val currentSearchTerm = viewModel.getCurrentSearchTerm()
                val listIsEmpty = auctionAdapter.itemCount == 0

                binding.recyclerView.isVisible = !listIsEmpty

                if (listIsEmpty) {
                    // Список пуст, показываем сообщение
                    // Не показываем сообщение "нет данных", если идет загрузка или произошла ошибка
                    // (эти состояния обрабатываются в loadState.observe)
                    if (currentLoadState !is HomeViewModel.LoadState.Loading && currentLoadState !is HomeViewModel.LoadState.Error) {
                        binding.tvStatusMessage.text = if (currentSearchTerm.isNullOrEmpty()) {
                            if (currentLoadState is HomeViewModel.LoadState.Empty) "Аукционов пока нет"
                            else "Нет аукционов по текущему фильтру"
                        } else {
                            "По запросу \"$currentSearchTerm\" ничего не найдено"
                        }
                        binding.tvStatusMessage.isVisible = true
                    } else {
                        binding.tvStatusMessage.isVisible = false // Скрываем, если загрузка/ошибка
                    }
                } else {
                    // Список не пуст
                    binding.tvStatusMessage.isVisible = false
                }
            }
        })

        viewModel.loadState.observe(viewLifecycleOwner, Observer { state ->
            binding.swipeRefreshLayout.isRefreshing = false // Всегда останавливаем SwipeRefresh
            val currentSearchTerm = viewModel.getCurrentSearchTerm()

            // Главный ProgressBar: виден только при начальной загрузке (когда список еще пуст)
            binding.progressBarMain.isVisible = state is HomeViewModel.LoadState.Loading && auctionAdapter.itemCount == 0

            // Скрываем RecyclerView и tvStatusMessage по умолчанию, itemsList.observe уточнит их видимость
            // binding.recyclerView.isVisible = false // Управляется из itemsList.observe
            // binding.tvStatusMessage.isVisible = false // Управляется из itemsList.observe

            when (state) {
                is HomeViewModel.LoadState.Loading -> {
                    // Если идет загрузка и список пуст, progressBarMain уже должен быть видим.
                    // tvStatusMessage должен быть скрыт. recyclerView тоже.
                    if (auctionAdapter.itemCount == 0) {
                        binding.recyclerView.isVisible = false
                        binding.tvStatusMessage.isVisible = false
                    }
                    // Если список не пуст, progressBarMain не будет виден, старые данные отображаются.
                }
                is HomeViewModel.LoadState.Success -> {
                    // progressBarMain должен быть скрыт.
                    // Видимость recyclerView и tvStatusMessage определяется в itemsList.observe.
                    binding.progressBarMain.isVisible = false
                }
                is HomeViewModel.LoadState.Empty -> {
                    binding.progressBarMain.isVisible = false
                    binding.recyclerView.isVisible = false // Список точно пуст
                    binding.tvStatusMessage.text = if (currentSearchTerm.isNullOrEmpty()) {
                        "Аукционов пока нет"
                    } else {
                        "По запросу \"$currentSearchTerm\" ничего не найдено"
                    }
                    binding.tvStatusMessage.isVisible = true
                }
                is HomeViewModel.LoadState.Error -> {
                    binding.progressBarMain.isVisible = false
                    val errorText = "Ошибка: ${state.message}"
                    if (auctionAdapter.itemCount == 0) { // Если нет старых данных для отображения
                        binding.recyclerView.isVisible = false
                        binding.tvStatusMessage.text = errorText
                        binding.tvStatusMessage.isVisible = true
                    } else {
                        // Показываем старые данные, если есть, ошибку показываем через Toast
                        binding.recyclerView.isVisible = true
                        binding.tvStatusMessage.isVisible = false
                    }
                    Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
                }
                is HomeViewModel.LoadState.Idle -> {
                    binding.progressBarMain.isVisible = false
                    // Начальное состояние или после отмены загрузки.
                    // Видимость recyclerView и tvStatusMessage определяется в itemsList.observe.
                    if (auctionAdapter.itemCount == 0) {
                        binding.recyclerView.isVisible = false
                        binding.tvStatusMessage.text = if (currentSearchTerm.isNullOrEmpty()) {
                            "Нет аукционов"
                        } else {
                            "По запросу \"$currentSearchTerm\" ничего не найдено"
                        }
                        binding.tvStatusMessage.isVisible = true
                    } else {
                        binding.recyclerView.isVisible = true
                        binding.tvStatusMessage.isVisible = false
                    }
                }
            }
        })

        viewModel.isLoadingMore.observe(viewLifecycleOwner, Observer { isLoadingMore ->
            loadingStateAdapter.setLoading(isLoadingMore)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchView?.setOnQueryTextListener(null)
        searchView?.setOnCloseListener(null)
        searchView = null
        binding.recyclerView.adapter = null // Важно для предотвращения утечек ConcatAdapter
        _binding = null
    }
}
