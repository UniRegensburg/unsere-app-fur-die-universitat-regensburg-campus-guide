package de.ur.explure

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import de.ur.explure.databinding.ActivityMainBinding
import de.ur.explure.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Main activity of the single activity application.
 */

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initNavigation()
    }

    private fun initNavigation() {
        viewModel.setNavigationGraph(findNavController(R.id.main_fragment_container))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.w("Unhandled Configuration Change occured!")
    }
}
