package de.ur.explure.views

import de.ur.explure.BuildConfig
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.adapter.AboutAdapter
import de.ur.explure.databinding.FragmentAboutBinding
import de.ur.explure.model.AttributionItem

@Suppress("StringLiteralDuplication")
class AboutFragment : Fragment(R.layout.fragment_about) {

    private val binding by viewBinding(FragmentAboutBinding::bind)
    private lateinit var aboutAdapter: AboutAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        // set app name and version
        binding.aboutAppName.text = getString(R.string.app_name)
        binding.aboutAppVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        // setup the icon list
        aboutAdapter = AboutAdapter().apply {
            attributionList = allAttributions
        }

        binding.aboutAttributionList.apply {
            setHasFixedSize(true)
            adapter = aboutAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear() // don't show any menu in this fragment!
    }

    companion object {
        val allAttributions = listOf(
            AttributionItem(
                R.drawable.arbeiten,
                "Work by Kraya from the Noun Project (https://thenounproject.com/icon/3497800/)"
            ),
            AttributionItem(
                R.drawable.books,
                "books by Jakub Čaja from the Noun Project (https://thenounproject.com/icon/137857/)"
            ),
            AttributionItem(
                R.drawable.sport,
                "Sport by Adrien Coquet from the Noun Project (https://thenounproject.com/icon/2978801/)"
            ),
            AttributionItem(
                R.drawable.chillen,
                "leisure by Adrien Coquet from the Noun Project (https://thenounproject.com/icon/3053718/)"
            ),
            AttributionItem(
                R.drawable.verwaltung,
                "management by ProSymbols from the Noun Project (https://thenounproject.com/icon/1871833/)"
            ),
            AttributionItem(
                R.drawable.cafeten,
                "Food Container by dDara from the Noun Project (https://thenounproject.com/icon/1715476/)"
            ),
            AttributionItem(
                R.drawable.ic_created_comments_statistic_icon,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_created_landmarks_statistic_icon,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_created_routes_statistic_icon,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_own_routes_icon,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_covered_track_statistic_icon,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_started_routes_statistic_icon,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_finished_routes_statistic_icon,
                "Icon made by \"Smashicons\" from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_statistics_icon,
                "Icon made by \"phatplus\" from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.user_profile_picture,
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSvdgoD2X2IRxDvjN2cWTigr0pXX-g7j2ylqw&usqp=CAU"
            ), // TODO replace this icon with the default android material icon!!
            AttributionItem(
                R.drawable.ic_audio,
                "Icon made by \"Pixel perfect\" from https://www.flaticon.com/"
            ),
            AttributionItem(
                R.drawable.ic_distance,
                "distance by dewadesign from the Noun Project (https://thenounproject.com/icon/2587693/)"
            ),
            AttributionItem(
                R.drawable.ic_duration,
                "Time by Ned from the Noun Project (https://thenounproject.com/icon/3541380/)"
            ),
            AttributionItem(
                R.drawable.ic_favorite_routes_icon,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_layers,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_magnet,
                "Icon made by Freepik from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_pencil,
                "Icon made by Pixel perfect from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_pencil_delete,
                "Icon made by Pixel perfect from www.flaticon.com"
            ),
            AttributionItem(
                R.drawable.ic_move_filled,
                "Icon made by Freepik from www.flaticon.com"
            )
        )
    }
}
