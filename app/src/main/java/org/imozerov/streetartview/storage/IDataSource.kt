package org.imozerov.streetartview.storage

import org.imozerov.streetartview.network.model.Artwork
import org.imozerov.streetartview.ui.model.ArtObjectUi
import rx.Observable

/**
 * Created by imozerov on 04.03.16.
 */
interface IDataSource {
    fun insert(artworks: List<Artwork>)
    fun listArtObjects(): Observable<List<ArtObjectUi>>
    fun getArtObject(id: String): ArtObjectUi
    fun listFavourites(): Observable<List<ArtObjectUi>>
    fun setFavourite(artObjectId: String, isFavourite: Boolean)
}