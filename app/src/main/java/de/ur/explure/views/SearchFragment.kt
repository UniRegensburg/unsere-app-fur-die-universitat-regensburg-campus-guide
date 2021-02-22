package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import de.ur.explure.R
import de.ur.explure.navigation.MainAppRouter
import kotlinx.android.synthetic.main.fragment_search.*
import org.koin.android.ext.android.inject

class SearchFragment : Fragment(R.layout.fragment_search) {

    private val appRouter: MainAppRouter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView.setOnClickListener {
            appRouter.getNavigationController()!!.navigate(R.id.testFragment)
        }
    }
}
