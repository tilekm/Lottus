package kz.tilek.lottus.fragments.profile
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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import kz.tilek.lottus.R
import kz.tilek.lottus.adapters.ReviewAdapter
import kz.tilek.lottus.data.TokenManager
import kz.tilek.lottus.databinding.FragmentUserProfileBinding
import kz.tilek.lottus.fragments.dialogs.CreateReviewDialogFragment 
import kz.tilek.lottus.models.User
import kz.tilek.lottus.viewmodels.UserProfileViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
class UserProfileFragment : Fragment() {
    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserProfileViewModel by viewModels()
    private val args: UserProfileFragmentArgs by navArgs()
    private lateinit var reviewAdapter: ReviewAdapter
    private var currentReviewedUserId: String? = null 
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentReviewedUserId = args.userId 
        setupToolbar()
        setupRecyclerView()
        observeViewModel() 
        binding.btnLeaveReview.setOnClickListener {
            currentReviewedUserId?.let { userId ->
                if (userId == TokenManager.userId) {
                    Toast.makeText(requireContext(), "Вы не можете оставить отзыв о себе.", Toast.LENGTH_LONG).show()
                } else {
                    val dialog = CreateReviewDialogFragment.newInstance(userId)
                    dialog.show(childFragmentManager, CreateReviewDialogFragment.TAG)
                }
            }
        }
        currentReviewedUserId?.let {
            viewModel.loadUserProfileAndReviews(it)
        }
    }
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter(emptyList())
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReviews.adapter = reviewAdapter
        binding.rvReviews.isNestedScrollingEnabled = false
    }
    private fun observeViewModel() {
        viewModel.profileDetailsState.observe(viewLifecycleOwner, Observer { result ->
            binding.progressBar.isVisible = false 
            Log.d("UserProfileFragment", "Profile details state: $result")
            result.onSuccess { details ->
                bindUserData(details.user)
                reviewAdapter.updateData(details.reviews)
                binding.rvReviews.isVisible = details.reviews.isNotEmpty()
                binding.tvNoReviews.isVisible = details.reviews.isEmpty()
                val canLeaveReview = details.user.id != TokenManager.userId
                Log.d("UserProfileFragment", "Can leave review: $canLeaveReview")
                Log.d("UserProfileFragment", "Current user ID: ${TokenManager.userId}")
                Log.d("UserProfileFragment", "Reviewed user ID: ${details.user.id}")
                binding.btnLeaveReview.isVisible = canLeaveReview
            }.onFailure { exception ->
                Toast.makeText(requireContext(), "Ошибка: ${exception.message}", Toast.LENGTH_LONG).show()
                Log.d("UserProfileFragment", "Ошибка: ${exception.message}", exception)
                binding.toolbar.title = "Ошибка"
                binding.ivAvatar.isVisible = false
                binding.tvUsername.isVisible = false
                binding.tvRating.isVisible = false
                binding.tvJoinDate.isVisible = false
                binding.rvReviews.isVisible = false
                binding.tvReviewsLabel.isVisible = false
                binding.tvNoReviews.isVisible = true
                binding.tvNoReviews.text = "Не удалось загрузить профиль"
                binding.btnLeaveReview.isVisible = false
            }
        })
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (viewModel.isCreatingReview.value != true) {
                binding.progressBar.isVisible = isLoading
            }
        })
        viewModel.createReviewState.observe(viewLifecycleOwner, Observer { result ->
            result?.onSuccess {
            }
            result?.onFailure {
            }
        })
    }
    private fun bindUserData(user: User) {
        binding.toolbar.title = user.username
        binding.tvUsername.text = user.username
        binding.tvRating.text = user.rating.toString()
        Glide.with(this)
            .load(user.profilePictureUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .circleCrop()
            .into(binding.ivAvatar)
        try {
            val createdAtInstant = Instant.parse(user.createdAt)
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale("ru", "RU"))
                .withZone(ZoneId.systemDefault())
            binding.tvJoinDate.text = "На Lottus с: ${formatter.format(createdAtInstant)}"
        } catch (e: Exception) {
            binding.tvJoinDate.text = "Дата регистрации неизвестна"
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
