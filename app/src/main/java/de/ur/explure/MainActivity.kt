package de.ur.explure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import de.ur.explure.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main activity of the single activity application.
 */

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initNavigation()
    }

    private fun initNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.main_fragment_container) as? NavHostFragment
        val navController = navHostFragment?.navController
        if (navController != null) {
            viewModel.initializeNavController(navController)
        } else {
            throw IllegalStateException("Couldn't find associated NavController for ${this::class.java.simpleName}!")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return viewModel.navigateUp()
    }
}
