package org.fossasia.openevent.general.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_search_location.view.currentLocationLinearLayout
import kotlinx.android.synthetic.main.fragment_search_location.view.locationProgressBar
import kotlinx.android.synthetic.main.fragment_search_location.view.locationSearchView
import kotlinx.android.synthetic.main.fragment_search_location.view.eventLocationLv
import kotlinx.android.synthetic.main.fragment_search_location.view.shimmerSearchEventTypes
import org.fossasia.openevent.general.R
import org.fossasia.openevent.general.utils.Utils
import org.fossasia.openevent.general.utils.extensions.nonNull
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.fossasia.openevent.general.utils.Utils.setToolbar
import org.jetbrains.anko.design.snackbar

const val LOCATION_PERMISSION_REQUEST = 1000

class SearchLocationFragment : Fragment() {
    private lateinit var rootView: View
    private val searchLocationViewModel by viewModel<SearchLocationViewModel>()
    private val geoLocationViewModel by viewModel<GeoLocationViewModel>()
    private val safeArgs: SearchLocationFragmentArgs by navArgs()
    private val eventLocationList: MutableList<String> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_search_location, container, false)
        setToolbar(activity, hasUpEnabled = true, show = true)
        setHasOptionsMenu(true)
        searchLocationViewModel.loadEventsLocation()
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, eventLocationList)
        rootView.eventLocationLv.adapter = adapter
        searchLocationViewModel.showShimmer
            .nonNull()
            .observe(viewLifecycleOwner, Observer { shouldShowShimmer ->
                if (shouldShowShimmer) {
                    rootView.shimmerSearchEventTypes.startShimmer()
                    adapter.clear()
                } else {
                    rootView.shimmerSearchEventTypes.stopShimmer()
                }
                rootView.shimmerSearchEventTypes.isVisible = shouldShowShimmer
            })
        searchLocationViewModel.eventLocations
            .nonNull()
            .observe(this, Observer { list ->
                list.forEach {
                    eventLocationList.add(it.name ?: "")
                }
                adapter.notifyDataSetChanged()
            })
        rootView.eventLocationLv.setOnItemClickListener { parent, view, position, id ->
            searchLocationViewModel.saveSearch(eventLocationList[position])
            redirectToMain()
        }

        geoLocationViewModel.currentLocationVisibility.observe(viewLifecycleOwner, Observer {
            rootView.currentLocationLinearLayout.visibility = View.GONE
        })

        rootView.currentLocationLinearLayout.setOnClickListener {
            checkLocationPermission()
            geoLocationViewModel.configure()
            rootView.locationProgressBar.visibility = View.VISIBLE
        }

        geoLocationViewModel.location.observe(viewLifecycleOwner, Observer { location ->
            searchLocationViewModel.saveSearch(location)
            redirectToMain()
        })

        rootView.locationSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchLocationViewModel.saveSearch(query)
                redirectToMain()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        return rootView
    }

    override fun onResume() {
        super.onResume()
        Utils.showSoftKeyboard(context, rootView.locationSearchView)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Utils.hideSoftKeyboard(context, rootView)
                activity?.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkLocationPermission() {
        val permission =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    private fun redirectToMain() {
        val fragmentId = if (safeArgs.fromSearchFragment) R.id.searchFragment else R.id.eventsFragment
        Navigation.findNavController(rootView).popBackStack(fragmentId, false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    geoLocationViewModel.configure()
                } else {
                    rootView.snackbar(R.string.cannot_fetch_location)
                    rootView.locationProgressBar.visibility = View.GONE
                }
            }
        }
    }
}
