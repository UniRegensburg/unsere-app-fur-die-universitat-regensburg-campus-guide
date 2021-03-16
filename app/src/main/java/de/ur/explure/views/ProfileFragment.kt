package de.ur.explure.views

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.ACTION_IMAGE_CAPTURE
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.FragmentProfileBinding
import de.ur.explure.extensions.toDp
import de.ur.explure.viewmodel.ProfileViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val binding by viewBinding(FragmentProfileBinding::bind)

    private val viewModel: ProfileViewModel by viewModel()

    private val fireStorage: FirebaseStorage by inject()

    private var userId: String = ""
    private lateinit var imageUri: Uri

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        setOnClickListeners()

        observeUserModel()
        viewModel.getUserInfo()
    }

    private fun observeUserModel() {
        viewModel.user.observe(viewLifecycleOwner, { user ->
            if (user != null) {
                binding.userNameTextView.text = user.name
                userId = user.id
                if (user.profilePictureUrl.isNotEmpty()) {
                    try {
                        val gsReference =
                            fireStorage.getReferenceFromUrl(user.profilePictureUrl)
                        GlideApp.with(requireContext())
                            .load(gsReference)
                            .error(R.drawable.user_profile_picture)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.profilePicture)
                    } catch (_: Exception) {
                    }
                }
            }
        })
    }

    private fun setOnClickListeners() {
        binding.profilePicture.setOnClickListener {
            takePictureIntent()
        }

        binding.ownRoutesButton.setOnClickListener {
            viewModel.showCreatedRoutes()
        }

        binding.favoriteRoutesButton.setOnClickListener {
            viewModel.showFavoriteRoutes()
        }

        binding.statisticsButton.setOnClickListener {
            viewModel.showStatisticsFragment()
        }

        binding.logOutButton.setOnClickListener {
            viewModel.signOut()
        }
    }

    private fun takePictureIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            uploadImageAndSaveUri(imageBitmap)
        }
    }

    private fun uploadImageAndSaveUri(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        val storageRef = fireStorage.reference.child("profile_pictures/$userId")
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY_VALUE, baos)
        val image = baos.toByteArray()

        val upload = storageRef.putBytes(image)

        upload.addOnCompleteListener { uploadTask ->
            if (uploadTask.isSuccessful) {
                storageRef.downloadUrl.addOnCompleteListener { urlTask ->
                    urlTask.result?.let {
                        imageUri = it
                        binding.profilePicture.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.changeUserName) {
            showDialog()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this.requireContext())

        builder.setTitle(resources.getString(R.string.change_user_name))

        val constraintLayout = getEditUserNameLayout(this.requireContext())
        builder.setView(constraintLayout)

        val textInputLayout =
            constraintLayout.findViewWithTag<TextInputLayout>("textInputLayoutTag")
        val textInputEditText =
            constraintLayout.findViewWithTag<TextInputEditText>("textInputEditTextTag")

        builder.setPositiveButton(resources.getString(R.string.confirm)) { _, _ ->
            val name = textInputEditText.text
            viewModel.updateUserName(name.toString())
        }
        builder.setNegativeButton(resources.getString(R.string.abort), null)
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                // do nothing
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.isNullOrBlank()) {
                    textInputLayout.error = resources.getString(R.string.user_name_needed)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .isEnabled = false
                } else {
                    textInputLayout.error = ""
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .isEnabled = true
                }
            }
        })
    }

    private fun getEditUserNameLayout(context: Context): ConstraintLayout {
        val constraintLayout = ConstraintLayout(context)
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        constraintLayout.layoutParams = layoutParams
        constraintLayout.id = View.generateViewId()

        val textInputLayout = TextInputLayout(context)
        textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        layoutParams.setMargins(
            VERTICAL_MARGIN_EDIT_TEXT.toDp(context),
            HORIZONTAL_MARGIN_EDIT_TEXT.toDp(context),
            VERTICAL_MARGIN_EDIT_TEXT.toDp(context),
            HORIZONTAL_MARGIN_EDIT_TEXT.toDp(context)
        )
        textInputLayout.layoutParams = layoutParams
        textInputLayout.hint = resources.getString(R.string.new_user_name_hint)
        textInputLayout.id = View.generateViewId()
        textInputLayout.tag = "textInputLayoutTag"

        val textInputEditText = TextInputEditText(context)
        textInputEditText.id = View.generateViewId()
        textInputEditText.tag = "textInputEditTextTag"

        textInputLayout.addView(textInputEditText)

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintLayout.addView(textInputLayout)
        return constraintLayout
    }

    companion object {
        private const val VERTICAL_MARGIN_EDIT_TEXT = 8
        private const val HORIZONTAL_MARGIN_EDIT_TEXT = 32

        private const val REQUEST_IMAGE_CAPTURE = 100
        private const val QUALITY_VALUE = 100
    }
}
