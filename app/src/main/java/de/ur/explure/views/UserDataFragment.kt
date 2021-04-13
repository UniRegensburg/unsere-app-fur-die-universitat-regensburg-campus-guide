package de.ur.explure.views

import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.R
import de.ur.explure.databinding.FragmentUserDataBinding
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.UserDataViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class UserDataFragment : Fragment(R.layout.fragment_user_data) {

    private val binding by viewBinding(FragmentUserDataBinding::bind)
    private val userDataViewModel: UserDataViewModel by viewModel()

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                if (Build.VERSION.SDK_INT >= SDK_VERSION_FOR_IMAGE_DECODER) {
                    val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source)
                    userDataViewModel.updateProfilePicture(bitmap, QUALITY_VALUE)
                    binding.setProfilePicture.setImageBitmap(bitmap)
                } else {
                    val bitmap =
                        MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                    userDataViewModel.updateProfilePicture(bitmap, QUALITY_VALUE)
                    binding.setProfilePicture.setImageBitmap(bitmap)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListener()
        setErrorObserver()
    }

    private fun setOnClickListener() {
        setProfilePictureListener()
        setCreateProfileListener()
    }

    private fun setProfilePictureListener() {
        binding.setProfilePicture.setOnClickListener {
            pickImages.launch("image/*")
        }
    }

    private fun setCreateProfileListener() {
        binding.createProfile.setOnClickListener {
            val userName = binding.edUserName.text.toString()
            if (userName.isNotEmpty()) {
                userDataViewModel.createProfileAndNavigateToMain(userName)
            } else {
                binding.edUserName.error = resources.getString(R.string.empty_username)
            }
        }
    }

    private fun setErrorObserver() {
        userDataViewModel.showErrorMessage.observe(viewLifecycleOwner, { showError ->
            if (showError == true) {
                showSnackbar(
                    requireActivity(),
                    R.string.user_data_error,
                    R.id.user_data_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorWarning
                )
            }
        })
    }

    companion object {
        private const val QUALITY_VALUE = 100
        private const val SDK_VERSION_FOR_IMAGE_DECODER = 29
    }
}
