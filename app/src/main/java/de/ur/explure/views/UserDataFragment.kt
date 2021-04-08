package de.ur.explure.views

import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.databinding.FragmentUserDataBinding
import de.ur.explure.viewmodel.ProfileViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class UserDataFragment : Fragment(R.layout.fragment_user_data) {

    private val binding by viewBinding(FragmentUserDataBinding::bind)
    private val viewModel: ProfileViewModel by viewModel()

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            if (Build.VERSION.SDK_INT >= SDK_VERSION_FOR_IMAGE_DECODER) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)
                viewModel.updateProfilePicture(bitmap, QUALITY_VALUE)
                binding.setProfilePicture.setImageBitmap(bitmap)
            } else {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                viewModel.updateProfilePicture(bitmap, QUALITY_VALUE)
                binding.setProfilePicture.setImageBitmap(bitmap)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUsername()
        setOnClickListener()
    }

    private fun setUsername() {
    }

    private fun setOnClickListener() {
        binding.setProfilePicture.setOnClickListener {
            pickImages.launch("image/*")
        }
        binding.createProfile.setOnClickListener {
            // navigate to MainApp
        }
    }

    companion object {
        private const val QUALITY_VALUE = 100
        private const val SDK_VERSION_FOR_IMAGE_DECODER = 29
    }
}
