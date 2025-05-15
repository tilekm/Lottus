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
        val initialFilter = viewModel.getCurrentFilter()
        when (initialFilter) {
            AuctionFilterType.ACTIVE -> binding.chipGroupFilter.check(R.id.chipActive)
            AuctionFilterType.SCHEDULED -> binding.chipGroupFilter.check(R.id.chipScheduled)
            AuctionFilterType.ALL -> binding.chipGroupFilter.check(R.id.chipAll)
        }
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
        } else {
            searchView?.isIconified = true
        }
        searchView?.setOnCloseListener {
            viewModel.performSearchNow(null) 
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
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val isLoading = viewModel.isLoadingMore.value ?: false
                val isNotLoadingState = viewModel.loadState.value !is HomeViewModel.LoadState.Loading
                if (!isLoading && isNotLoadingState) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 
                        && firstVisibleItemPosition >= 0
                        && totalItemCount > 0 
                        && auctionAdapter.itemCount == totalItemCount - (if (loadingStateAdapter.itemCount > 0) 1 else 0) 
                    ) {
                        viewModel.loadItems(isRefresh = false)
                    }
                }
            }
        })
    }
    private fun setupChipGroup() {
        val currentVmFilterInitial = viewModel.getCurrentFilter()
        val initialChipId = when (currentVmFilterInitial) {
            AuctionFilterType.ACTIVE -> R.id.chipActive
            AuctionFilterType.SCHEDULED -> R.id.chipScheduled
            AuctionFilterType.ALL -> R.id.chipAll
            else -> R.id.chipActive 
        }
        if (binding.chipGroupFilter.checkedChipId != initialChipId) {
            binding.chipGroupFilter.check(initialChipId)
        }
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val activeFilterBeforeUserAction = viewModel.getCurrentFilter()
            val checkedId = checkedIds.firstOrNull() 
            if (checkedId == null) { 
                val chipIdToReselect = when (activeFilterBeforeUserAction) {
                    AuctionFilterType.ACTIVE -> R.id.chipActive
                    AuctionFilterType.SCHEDULED -> R.id.chipScheduled
                    AuctionFilterType.ALL -> R.id.chipAll
                    else -> R.id.chipActive 
                }
                group.check(chipIdToReselect) 
                return@setOnCheckedStateChangeListener 
            }
            val newFilter = when (checkedId) {
                R.id.chipActive -> AuctionFilterType.ACTIVE
                R.id.chipScheduled -> AuctionFilterType.SCHEDULED
                R.id.chipAll -> AuctionFilterType.ALL
                else -> activeFilterBeforeUserAction
            }
            if (activeFilterBeforeUserAction != newFilter || !viewModel.getCurrentSearchTerm().isNullOrEmpty()) {
                if (searchView != null && !viewModel.getCurrentSearchTerm().isNullOrEmpty()) {
                    searchView?.setQuery("", false)
                    searchView?.isIconified = true
                }
                viewModel.setFilter(newFilter) 
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
            auctionAdapter.submitList(items.toList()) {
                val currentLoadState = viewModel.loadState.value
                val currentSearchTerm = viewModel.getCurrentSearchTerm()
                val listIsEmpty = auctionAdapter.itemCount == 0
                binding.recyclerView.isVisible = !listIsEmpty
                if (listIsEmpty) {
                    if (currentLoadState !is HomeViewModel.LoadState.Loading && currentLoadState !is HomeViewModel.LoadState.Error) {
                        binding.tvStatusMessage.text = if (currentSearchTerm.isNullOrEmpty()) {
                            if (currentLoadState is HomeViewModel.LoadState.Empty) "Аукционов пока нет"
                            else "Нет аукционов по текущему фильтру"
                        } else {
                            "По запросу \"$currentSearchTerm\" ничего не найдено"
                        }
                        binding.tvStatusMessage.isVisible = true
                    } else {
                        binding.tvStatusMessage.isVisible = false 
                    }
                } else {
                    binding.tvStatusMessage.isVisible = false
                }
            }
        })
        viewModel.loadState.observe(viewLifecycleOwner, Observer { state ->
            binding.swipeRefreshLayout.isRefreshing = false 
            val currentSearchTerm = viewModel.getCurrentSearchTerm()
            binding.progressBarMain.isVisible = state is HomeViewModel.LoadState.Loading && auctionAdapter.itemCount == 0
            when (state) {
                is HomeViewModel.LoadState.Loading -> {
                    if (auctionAdapter.itemCount == 0) {
                        binding.recyclerView.isVisible = false
                        binding.tvStatusMessage.isVisible = false
                    }
                }
                is HomeViewModel.LoadState.Success -> {
                    binding.progressBarMain.isVisible = false
                }
                is HomeViewModel.LoadState.Empty -> {
                    binding.progressBarMain.isVisible = false
                    binding.recyclerView.isVisible = false 
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
                    if (auctionAdapter.itemCount == 0) { 
                        binding.recyclerView.isVisible = false
                        binding.tvStatusMessage.text = errorText
                        binding.tvStatusMessage.isVisible = true
                    } else {
                        binding.recyclerView.isVisible = true
                        binding.tvStatusMessage.isVisible = false
                    }
                    Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
                }
                is HomeViewModel.LoadState.Idle -> {
                    binding.progressBarMain.isVisible = false
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
        binding.recyclerView.adapter = null 
        _binding = null
    }
}
