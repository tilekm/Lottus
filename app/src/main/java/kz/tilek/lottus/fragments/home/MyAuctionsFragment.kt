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
import kz.tilek.lottus.databinding.FragmentMyAuctionsBinding
import kz.tilek.lottus.viewmodels.MyAuctionFilterType
import kz.tilek.lottus.viewmodels.MyAuctionsViewModel
class MyAuctionsFragment : Fragment() {
    private var _binding: FragmentMyAuctionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyAuctionsViewModel by viewModels()
    private lateinit var auctionAdapter: AuctionAdapter
    private lateinit var loadingStateAdapter: LoadingStateAdapter
    private lateinit var concatAdapter: ConcatAdapter
    private var searchView: SearchView? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAuctionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarAndSearchView()
        setupRecyclerView()
        setupChipGroup()
        setupSwipeRefresh()
        observeViewModel()
        viewModel.currentFilterType.value?.let { updateChipSelection(it) }
        viewModel.currentSearchTerm.value?.let {
            if (it.isNotEmpty()) {
                searchView?.setQuery(it, false) 
                searchView?.isIconified = false
            }
        }
    }
    private fun setupToolbarAndSearchView() {
        searchView = binding.searchViewMyAuctions
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
        searchView?.setOnCloseListener {
            viewModel.performSearchNow(null) 
            false 
        }
    }
    private fun setupRecyclerView() {
        auctionAdapter = AuctionAdapter { clickedItem ->
            val action = MyAuctionsFragmentDirections.actionMyAuctionsFragmentToItemDetailFragment(clickedItem.id)
            findNavController().navigate(action)
        }
        loadingStateAdapter = LoadingStateAdapter()
        concatAdapter = ConcatAdapter(auctionAdapter, loadingStateAdapter)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMyAuctions.layoutManager = layoutManager
        binding.recyclerViewMyAuctions.adapter = concatAdapter
        binding.recyclerViewMyAuctions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val isLoading = viewModel.isLoadingMore.value ?: false
                val isNotLoadingState = viewModel.loadState.value !is MyAuctionsViewModel.LoadState.Loading
                if (!isLoading && isNotLoadingState) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0
                        && totalItemCount > 0
                        && auctionAdapter.itemCount == totalItemCount - (if (loadingStateAdapter.itemCount > 0) 1 else 0)
                    ) {
                        viewModel.loadMyAuctions(isRefresh = false)
                    }
                }
            }
        })
    }
    private fun setupChipGroup() {
        binding.chipGroupFilterMyAuctions.setOnCheckedStateChangeListener { group, checkedIds ->
            val activeFilterBeforeUserAction = viewModel.getCurrentFilterValue()
            val checkedId = checkedIds.firstOrNull() 
            if (checkedId == null) { 
                val chipIdToReselect = when (activeFilterBeforeUserAction) {
                    MyAuctionFilterType.PARTICIPATING -> R.id.chipParticipating
                    MyAuctionFilterType.WON -> R.id.chipWon
                    MyAuctionFilterType.CREATED -> R.id.chipCreated
                }
                group.check(chipIdToReselect) 
                return@setOnCheckedStateChangeListener 
            }
            val newFilter = when (checkedId) {
                R.id.chipParticipating -> MyAuctionFilterType.PARTICIPATING
                R.id.chipWon -> MyAuctionFilterType.WON
                R.id.chipCreated -> MyAuctionFilterType.CREATED
                else -> activeFilterBeforeUserAction
            }
            if (activeFilterBeforeUserAction != newFilter || !viewModel.getCurrentSearchTermValue().isNullOrEmpty()) {
                if (searchView != null && !viewModel.getCurrentSearchTermValue().isNullOrEmpty()) {
                    searchView?.setQuery("", false)
                    searchView?.isIconified = true
                }
                viewModel.setFilter(newFilter)
            }
        }
    }
    private fun updateChipSelection(filterType: MyAuctionFilterType) {
        val chipId = when (filterType) {
            MyAuctionFilterType.PARTICIPATING -> R.id.chipParticipating
            MyAuctionFilterType.WON -> R.id.chipWon
            MyAuctionFilterType.CREATED -> R.id.chipCreated
        }
        if (binding.chipGroupFilterMyAuctions.checkedChipId != chipId) {
            binding.chipGroupFilterMyAuctions.check(chipId)
        }
    }
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayoutMyAuctions.setOnRefreshListener {
            viewModel.loadMyAuctions(isRefresh = true)
        }
    }
    private fun observeViewModel() {
        viewModel.myAuctionsList.observe(viewLifecycleOwner, Observer { items ->
            auctionAdapter.submitList(items.toList()) {
                val currentLoadState = viewModel.loadState.value
                val currentSearchTerm = viewModel.getCurrentSearchTermValue()
                val listIsEmpty = auctionAdapter.itemCount == 0
                binding.recyclerViewMyAuctions.isVisible = !listIsEmpty
                if (listIsEmpty) {
                    if (currentLoadState !is MyAuctionsViewModel.LoadState.Loading && currentLoadState !is MyAuctionsViewModel.LoadState.Error) {
                        binding.tvStatusMessageMyAuctions.text = if (currentSearchTerm.isNullOrEmpty()) {
                            if (currentLoadState is MyAuctionsViewModel.LoadState.Empty) "Аукционов пока нет"
                            else "Нет аукционов по текущему фильтру"
                        } else {
                            "По запросу \"$currentSearchTerm\" ничего не найдено"
                        }
                        binding.tvStatusMessageMyAuctions.isVisible = true
                    } else {
                        binding.tvStatusMessageMyAuctions.isVisible = false
                    }
                } else {
                    binding.tvStatusMessageMyAuctions.isVisible = false
                }
            }
        })
        viewModel.loadState.observe(viewLifecycleOwner, Observer { state ->
            binding.swipeRefreshLayoutMyAuctions.isRefreshing = false
            val currentSearchTerm = viewModel.getCurrentSearchTermValue()
            binding.progressBarMainMyAuctions.isVisible = state is MyAuctionsViewModel.LoadState.Loading && auctionAdapter.itemCount == 0
            when (state) {
                is MyAuctionsViewModel.LoadState.Loading -> {
                    if (auctionAdapter.itemCount == 0) {
                        binding.recyclerViewMyAuctions.isVisible = false
                        binding.tvStatusMessageMyAuctions.isVisible = false
                    }
                }
                is MyAuctionsViewModel.LoadState.Success -> {
                    binding.progressBarMainMyAuctions.isVisible = false
                }
                is MyAuctionsViewModel.LoadState.Empty -> {
                    binding.progressBarMainMyAuctions.isVisible = false
                    binding.recyclerViewMyAuctions.isVisible = false
                    binding.tvStatusMessageMyAuctions.text = if (currentSearchTerm.isNullOrEmpty()) {
                        "Аукционов пока нет"
                    } else {
                        "По запросу \"$currentSearchTerm\" ничего не найдено"
                    }
                    binding.tvStatusMessageMyAuctions.isVisible = true
                }
                is MyAuctionsViewModel.LoadState.Error -> {
                    binding.progressBarMainMyAuctions.isVisible = false
                    val errorText = "Ошибка: ${state.message}"
                    if (auctionAdapter.itemCount == 0) {
                        binding.recyclerViewMyAuctions.isVisible = false
                        binding.tvStatusMessageMyAuctions.text = errorText
                        binding.tvStatusMessageMyAuctions.isVisible = true
                    } else {
                        binding.recyclerViewMyAuctions.isVisible = true
                        binding.tvStatusMessageMyAuctions.isVisible = false
                    }
                    Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
                }
                is MyAuctionsViewModel.LoadState.Idle -> {
                    binding.progressBarMainMyAuctions.isVisible = false
                    if (auctionAdapter.itemCount == 0) {
                        binding.recyclerViewMyAuctions.isVisible = false
                        binding.tvStatusMessageMyAuctions.text = if (currentSearchTerm.isNullOrEmpty()) {
                            "Нет аукционов"
                        } else {
                            "По запросу \"$currentSearchTerm\" ничего не найдено"
                        }
                        binding.tvStatusMessageMyAuctions.isVisible = true
                    } else {
                        binding.recyclerViewMyAuctions.isVisible = true
                        binding.tvStatusMessageMyAuctions.isVisible = false
                    }
                }
            }
        })
        viewModel.isLoadingMore.observe(viewLifecycleOwner, Observer { isLoadingMore ->
            loadingStateAdapter.setLoading(isLoadingMore)
        })
        viewModel.currentFilterType.observe(viewLifecycleOwner) { filter ->
            updateChipSelection(filter)
        }
        viewModel.currentSearchTerm.observe(viewLifecycleOwner) { term ->
            if (searchView?.query.toString() != term) {
                searchView?.setQuery(term, false)
            }
            if (term.isNullOrEmpty() && searchView?.isIconified == false) {
                searchView?.isIconified = true
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        searchView?.setOnQueryTextListener(null)
        searchView?.setOnCloseListener(null)
        searchView = null
        binding.recyclerViewMyAuctions.adapter = null
        _binding = null
    }
}
