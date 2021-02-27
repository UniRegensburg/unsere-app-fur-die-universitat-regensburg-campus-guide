package de.ur.explure.views

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.ur.explure.R
import de.ur.explure.viewmodel.ProfileViewModel
import kotlinx.android.synthetic.main.fragment_profile.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClickListeners()

        observeUserModel()
        viewModel.getUserInfo()
    }

    private fun observeUserModel() {
        viewModel.user.observe(viewLifecycleOwner, { user ->
            if (user != null) {
                userNameTextView.text = user.name
            }
        })
    }

    private fun setOnClickListeners() {
        ownRoutesButton.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.createdRoutesFragment)
        }

        favoriteRoutesButton.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.favoriteRoutesFragment)
        }

        statisticsButton.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.statisticsFragment)
        }

        logOutButton.setOnClickListener {
            Toast.makeText(activity, "Still to come!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this.requireContext())

        builder.setTitle(String.format(getResources().getString(R.string.change_user_name)))

        val constraintLayout = getEditUserNameLayout(this.requireContext())
        builder.setView(constraintLayout)

        val textInputLayout = constraintLayout.findViewWithTag<TextInputLayout>("textInputLayoutTag")
        val textInputEditText = constraintLayout.findViewWithTag<TextInputEditText>("textInputEditTextTag")

        builder.setPositiveButton(String.format(getResources().getString(R.string.confirm))) { _, which ->
            val name = textInputEditText.text
            viewModel.updateUserName(name.toString())
        }
        builder.setNegativeButton(String.format(getResources().getString(R.string.abort)), null)
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
                    textInputLayout.error = String.format(getResources().getString(R.string.user_name_needed))
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
        textInputLayout.hint = String.format(getResources().getString(R.string.new_user_name_hint))
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

    private fun Int.toDp(context: Context): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
    ).toInt()

    companion object {
        private const val VERTICAL_MARGIN_EDIT_TEXT = 8
        private const val HORIZONTAL_MARGIN_EDIT_TEXT = 32
    }
}
