package org.imozerov.streetartview.ui.add

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.realm.RealmList
import org.imozerov.streetartview.StreetArtViewApp
import org.imozerov.streetartview.storage.IDataSource
import org.imozerov.streetartview.storage.model.RealmArtObject
import org.imozerov.streetartview.storage.model.RealmAuthor
import org.imozerov.streetartview.storage.model.RealmLocation
import org.imozerov.streetartview.storage.model.RealmString
import javax.inject.Inject

/**
 * Created by imozerov on 10.03.16.
 */
class AddArtObjectPresenter : GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {
    @Inject
    lateinit var dataSource: IDataSource

    private var googleApiClient: GoogleApiClient? = null
    private var lastLocation: Location? = null

    var view: AddArtObjectView? = null

    fun bindToView(view: AddArtObjectView, app: StreetArtViewApp) {
        app.appComponent.inject(this)

        this.view = view

        googleApiClient = GoogleApiClient.Builder(app)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener({})
                .addApi(LocationServices.API)
                .build()
    }

    fun onStart() {
        googleApiClient?.connect()
    }

    fun onStop() {
        googleApiClient?.disconnect()
    }

    fun addArtObject(authorName: String, artObjectName: String, artObjectDescription: String, imageUrl: String) {
        val realmAuthor = RealmAuthor()
        with (realmAuthor) {
            id = SystemClock.currentThreadTimeMillis().toString()
            name = authorName
            photo = "http://photos.state.gov/libraries/media/788/images/500x500-sample.jpg"
        }

        val realmAuthors = RealmList<RealmAuthor>()
        realmAuthors.add(realmAuthor)

        val realmLocation = RealmLocation()
        with(realmLocation) {
            address = "Some address, 34"
            lat = lastLocation!!.latitude
            lng = lastLocation!!.longitude
        }

        val realmArtObject = RealmArtObject()
        with (realmArtObject) {
            authors = realmAuthors
            description = artObjectDescription
            name = artObjectName
            id = SystemClock.currentThreadTimeMillis().toString()
            thumbPicUrl = imageUrl
            picsUrls = RealmList<RealmString>()
            location = realmLocation
            //                        lat = 56.307872
            //                        lng = 44.076207
        }
        //        dataSource.addArtObject(realmArtObject)
        view!!.onArtObjectAdded()
    }

    override fun onConnected(connectionHint: Bundle?) {
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
    }

    override fun onConnectionSuspended(p0: Int) {
        throw UnsupportedOperationException()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        throw UnsupportedOperationException()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        val lastLatLng = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
        googleMap?.addMarker(MarkerOptions().position(lastLatLng).title("Here you are"));
        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(lastLatLng));
    }
}

