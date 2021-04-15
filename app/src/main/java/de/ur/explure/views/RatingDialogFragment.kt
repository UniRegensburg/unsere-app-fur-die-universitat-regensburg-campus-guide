package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.R
import de.ur.explure.databinding.FragmentRatingDialogBinding
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.RatingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class RatingDialogFragment : DialogFragment(R.layout.fragment_rating_dialog) {

   private val binding by viewBinding(FragmentRatingDialogBinding::bind)
   private val ratingViewModel: RatingViewModel by viewModel()

   private val args: RatingDialogFragmentArgs by navArgs()
   private lateinit var routeId: String

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      this.isCancelable = false // don't allow cancelling this dialog fragment by clicking outside!

      routeId = args.routeID

      setOnClickListener()
      setErrorObserver()
   }

   private fun setOnClickListener() {
      binding.discardRating.setOnClickListener {
         dismiss()
      }
       binding.submitRating.setOnClickListener {
          val rating = binding.setRatingBar.rating.toInt()
          ratingViewModel.setRating(rating, routeId) {
             dismiss()
          }
       }
   }

   private fun setErrorObserver() {
      ratingViewModel.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
      if (errorMessage == true) {
         showSnackbar(
            requireActivity(),
            R.string.rating_error,
            R.id.set_rating_container,
            Snackbar.LENGTH_LONG,
            colorRes = R.color.colorError
         )
      }
      })
   }
}
