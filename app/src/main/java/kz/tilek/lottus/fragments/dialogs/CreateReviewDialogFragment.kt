package kz.tilek.lottus.fragments.dialogs
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import kz.tilek.lottus.databinding.DialogCreateReviewBinding
import kz.tilek.lottus.viewmodels.UserProfileViewModel
import java.math.BigDecimal
class CreateReviewDialogFragment : DialogFragment() {
    private var _binding: DialogCreateReviewBinding? = null
    private val binding get() = _binding!!
    private val userProfileViewModel: UserProfileViewModel by viewModels({ requireParentFragment() })
    companion object {
        const val TAG = "CreateReviewDialog"
        private const val ARG_REVIEWED_USER_ID = "reviewed_user_id"
        fun newInstance(reviewedUserId: String): CreateReviewDialogFragment {
            val args = Bundle()
            args.putString(ARG_REVIEWED_USER_ID, reviewedUserId)
            val fragment = CreateReviewDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
    private lateinit var reviewedUserId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            reviewedUserId = it.getString(ARG_REVIEWED_USER_ID) ?: throw IllegalArgumentException("Reviewed User ID is required")
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateReviewBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .setPositiveButton("Отправить", null) 
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }
        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val rating = binding.rbReviewRating.rating
                val comment = binding.etReviewComment.text.toString().trim().ifEmpty { null }
                if (rating == 0f) {
                    Toast.makeText(context, "Пожалуйста, поставьте оценку", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                userProfileViewModel.createReview(reviewedUserId, BigDecimal.valueOf(rating.toDouble()), comment)
            }
        }
        userProfileViewModel.isCreatingReview.observe(this) { isLoading ->
            binding.pbReviewSubmit.isVisible = isLoading
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !isLoading
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !isLoading
            binding.rbReviewRating.isEnabled = !isLoading
            binding.etReviewComment.isEnabled = !isLoading
        }
        userProfileViewModel.createReviewState.observe(this) { result ->
            result?.let {
                it.onSuccess {
                    Toast.makeText(requireContext(), "Отзыв успешно добавлен!", Toast.LENGTH_SHORT).show()
                    dismiss() 
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Ошибка: ${error.message}", Toast.LENGTH_LONG).show()
                     if (error.message?.contains("Вы уже оставили отзыв", ignoreCase = true) == true ||
                         error.message?.contains("оставить отзыв о себе", ignoreCase = true) == true) {
                         dismiss()
                     }
                }
            }
        }
        return dialog
    }
    override fun onDestroyView() {
        super.onDestroyView()
        userProfileViewModel.clearCreateReviewState() 
        _binding = null
    }
}
