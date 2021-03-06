package org.imozerov.streetartview.ui.explore.base

import android.app.Application
import android.content.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.analytics.Tracker
import com.google.android.gms.maps.model.LatLng
import org.imozerov.streetartview.StreetArtViewApp
import org.imozerov.streetartview.network.FetchService
import org.imozerov.streetartview.storage.IDataSource
import org.imozerov.streetartview.ui.explore.interfaces.ArtView
import org.imozerov.streetartview.ui.explore.sort.SortOrder
import org.imozerov.streetartview.ui.explore.sort.getSortOrder
import org.imozerov.streetartview.ui.extensions.distanceTo
import org.imozerov.streetartview.ui.extensions.getCurrentLocation
import org.imozerov.streetartview.ui.extensions.sendScreen
import org.imozerov.streetartview.ui.model.ArtObjectUi
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by imozerov on 11.02.16.
 */
abstract class ArtListPresenter : SharedPreferences.OnSharedPreferenceChangeListener {
    val TAG = "ArtListPresenter"

    private var view: ArtView? = null
    private var fetchSubscription: Subscription? = null
    private var filterQuery: String = ""
    private var sortOrder: Int? = SortOrder.byDate

    private val currentLocation by lazy { getCurrentLocation(application) }

    private val fetchFinishedBroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == FetchService.EVENT_FETCH_FINISHED) {
                    view?.stopRefresh()
                }
            }
        }
    }

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var dataSource: IDataSource

    @Inject
    lateinit var application: Application

    @Inject
    lateinit var prefs: SharedPreferences

    fun bindView(artView: ArtView, context: Context) {
        view = artView
        (context.applicationContext as StreetArtViewApp).appComponent!!.inject(this)

        sortOrder = prefs.getSortOrder()

        prefs.registerOnSharedPreferenceChangeListener(this)

        tracker.sendScreen(artView.javaClass.simpleName)

        fetchSubscription = createDataFetchSubscription()

        val intentFilter = IntentFilter()
        intentFilter.addAction(FetchService.EVENT_FETCH_FINISHED)
        LocalBroadcastManager.getInstance(application).registerReceiver(fetchFinishedBroadcastReceiver, intentFilter)
    }

    fun unbindView() {
        fetchSubscription!!.unsubscribe()

        prefs.unregisterOnSharedPreferenceChangeListener(this)
        LocalBroadcastManager.getInstance(application).unregisterReceiver(fetchFinishedBroadcastReceiver)
        view = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (SortOrder.KEY.equals(key) && sharedPreferences != null) {
            Log.v(TAG, "changing sort order")
            sortOrder = sharedPreferences.getSortOrder()
            fetchSubscription?.unsubscribe()
            fetchSubscription = createDataFetchSubscription()
        }
    }

    fun applyFilter(query: String) {
        if (query == filterQuery) {
            return
        }
        Log.v(TAG, "Applying filter $query")
        filterQuery = query
        fetchSubscription?.unsubscribe()
        fetchSubscription = createDataFetchSubscription()
    }

    fun getArtObject(id: String): ArtObjectUi {
        return dataSource.getArtObject(id)
    }

    fun refreshData() {
        FetchService.startFetch(application)
        tracker.sendScreen("Refresh data requested")
    }

    private fun createDataFetchSubscription(): Subscription {
        return fetchData()
                .debounce(200, TimeUnit.MILLISECONDS)
                .map {
                    if (filterQuery.isNotBlank()) {
                        it.filter { it.matches(filterQuery) }
                    } else {
                        it
                    }
                }
                .map {
                    if (sortOrder == SortOrder.byDistance) {
                        it.sortedWith(Comparator<ArtObjectUi> {
                            lhs, rhs ->
                            LatLng(lhs.lat, lhs.lng).distanceTo(currentLocation).toInt() - LatLng(rhs.lat, rhs.lng).distanceTo(currentLocation).toInt()
                        })
                    } else {
                        it
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view?.showArtObjects(it)
                }
    }

    abstract fun fetchData(): Observable<List<ArtObjectUi>>
}