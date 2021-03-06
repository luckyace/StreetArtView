package org.imozerov.streetartview.ui.explore

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.jakewharton.rxbinding.support.v7.widget.RxSearchView
import com.jakewharton.rxbinding.view.RxView
import kotlinx.android.synthetic.main.activity_explore_art.*
import org.imozerov.streetartview.BuildConfig
import org.imozerov.streetartview.R
import org.imozerov.streetartview.StreetArtViewApp
import org.imozerov.streetartview.ui.detail.DetailArtObjectActivity
import org.imozerov.streetartview.ui.detail.interfaces.ArtObjectDetailOpener
import org.imozerov.streetartview.ui.explore.all.ArtListFragment
import org.imozerov.streetartview.ui.explore.favourites.FavouritesListFragment
import org.imozerov.streetartview.ui.explore.interfaces.Filterable
import org.imozerov.streetartview.ui.explore.map.ArtMapFragment
import org.imozerov.streetartview.ui.explore.sort.SortOrder
import org.imozerov.streetartview.ui.explore.sort.getSortOrder
import org.imozerov.streetartview.ui.explore.sort.swapSortOrder
import org.imozerov.streetartview.ui.extensions.addAll
import org.imozerov.streetartview.ui.extensions.animateToGone
import org.imozerov.streetartview.ui.extensions.animateToVisible
import org.imozerov.streetartview.ui.extensions.getDrawableSafely
import org.jetbrains.anko.toast
import rx.subscriptions.CompositeSubscription
import java.util.*
import javax.inject.Inject

class ExploreArtActivity : AppCompatActivity(), ArtObjectDetailOpener {

    val TAG = "ExploreArtActivity"

    val compositeSubscription = CompositeSubscription()

    @Inject
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        (application as StreetArtViewApp).appComponent!!.inject(this)
        setContentView(R.layout.activity_explore_art)
        initTabs()
        initSortOrderIcon(prefs.getSortOrder())
    }

    override fun onStart() {
        super.onStart()
        initRxSubscriptions()
    }

    override fun onStop() {
        super.onStop()
        compositeSubscription.clear()
    }

    override fun onBackPressed() {
        if (getMapFragmentIfCurrent()?.onBackPressed() == true) {
            return
        }

        if (search_view.visibility == View.VISIBLE) {
            closeSearchView()
            return
        }
        super.onBackPressed()
    }

    override fun openArtObjectDetails(id: String?) {
        Log.d(TAG, "openArtObjectDetails($id)")
        val intent = Intent(this, DetailArtObjectActivity::class.java)
        intent.putExtra(DetailArtObjectActivity.EXTRA_KEY_ART_OBJECT_DETAIL_ID, id)
        startActivity(intent)
    }

    private fun initTabs() {
        val adapter = Adapter(supportFragmentManager)
        adapter.addFragment(ArtMapFragment.newInstance(), getString(R.string.map_fragment_pager_label))
        adapter.addFragment(ArtListFragment.newInstance(), getString(R.string.list_fragment_pager_label))
        adapter.addFragment(FavouritesListFragment.newInstance(), getString(R.string.favourites_fragment_pager_label))
        viewpager.adapter = adapter
        viewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageScrollStateChanged(pageIndex: Int) {
                if (viewpager.currentItem == 0) {
                    ((viewpager.adapter as FragmentPagerAdapter).getItem(0) as ArtMapFragment).startLocationTracking()
                } else {
                    ((viewpager.adapter as FragmentPagerAdapter).getItem(0) as ArtMapFragment).stopLocationTracking()
                }
                compositeSubscription.clear()
                initRxSubscriptions()
            }
        })
        viewpager.offscreenPageLimit = 3

        tabs.setupWithViewPager(viewpager)
        tabs.getTabAt(0)?.icon = getDrawableSafely(R.drawable.ic_explore_white_36dp)
        tabs.getTabAt(1)?.icon = getDrawableSafely(R.drawable.ic_visibility_white_36dp)
        tabs.getTabAt(2)?.icon = getDrawableSafely(R.drawable.ic_favorite_white_36dp)

        viewpager.currentItem = 1
    }

    private fun initSortOrderIcon(sortOrder: Int) {
        if (sortOrder == SortOrder.byDate) {
            explore_floating_action_button_sort_by.setImageDrawable(getDrawableSafely(R.drawable.ic_schedule_black_24dp))
        } else if (sortOrder == SortOrder.byDistance) {
            explore_floating_action_button_sort_by.setImageDrawable(getDrawableSafely(R.drawable.ic_location_on_black_24dp))
        } else {
            val errorMsg = "Unknown sort order: $sortOrder"
            if (BuildConfig.DEBUG) {
                throw RuntimeException(errorMsg)
            }
            Log.e(TAG, errorMsg)
        }
    }

    private fun initRxSubscriptions() {
        compositeSubscription.addAll(
                RxView.clicks(explore_floating_action_button_expand).subscribe { swapFloatingActionButtonsVisibility() },
                RxView.clicks(explore_floating_action_button_search).subscribe { openSearchView() },
                RxView.clicks(explore_floating_action_button_sort_by).subscribe { changeSortOrder() },
                RxView.clicks(search_view.findViewById(R.id.search_close_btn)).subscribe { closeSearchView() },
                RxSearchView.queryTextChanges(search_view).subscribe { applyFilter(it) }
        )
    }

    private fun applyFilter(query: CharSequence) {
        val fragment = supportFragmentManager
                .findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewpager.currentItem)
        (fragment as? Filterable)?.applyFilter(query.toString())
    }

    private fun changeSortOrder() {
        val newSortOrder = prefs.swapSortOrder()
        initSortOrderIcon(newSortOrder)
        toast(SortOrder.getString(newSortOrder))
    }

    private fun swapFloatingActionButtonsVisibility() {
        if (!explore_floating_action_button_sort_by.isShown) {
            if (search_view.visibility != View.VISIBLE) {
                explore_floating_action_button_search.show()
            }
            explore_floating_action_button_sort_by.show()
            explore_floating_action_button_expand.setImageDrawable(getDrawableSafely(R.drawable.ic_remove_black_24dp))
        } else {
            explore_floating_action_button_search.hide()
            explore_floating_action_button_sort_by.hide()
            explore_floating_action_button_expand.setImageDrawable(getDrawableSafely(R.drawable.ic_build_black_24dp))
        }
    }

    private fun openSearchView() {
        getMapFragmentIfCurrent()?.hideArtObjectDigest()
        explore_floating_action_button_search.hide()
        search_view.animateToVisible()
    }

    private fun closeSearchView() {
        hideKeyboard()
        if (explore_floating_action_button_sort_by.isShown) {
            explore_floating_action_button_search.show()
        }
        search_view.animateToGone()
    }

    private fun hideKeyboard() {
        if (currentFocus != null) {
            val inputMgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMgr.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun getMapFragmentIfCurrent(): ArtMapFragment? {
        val currentFragment = (viewpager.adapter as FragmentPagerAdapter).getItem(viewpager.currentItem)
        if (currentFragment is ArtMapFragment) {
            return currentFragment
        }

        return null
    }
}

private class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    private val mFragments = ArrayList<Fragment>()
    private val mFragmentTitles = ArrayList<String>()

    fun addFragment(fragment: Fragment, title: String) {
        mFragments.add(fragment)
        mFragmentTitles.add(title)
    }

    override fun getItem(position: Int): Fragment {
        return mFragments[position]
    }

    override fun getCount(): Int {
        return mFragments.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return ""
    }
}
