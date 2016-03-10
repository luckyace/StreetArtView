package org.imozerov.streetartview.ui.extensions

import android.net.Uri
import android.widget.ImageView
import com.squareup.picasso.Picasso

/**
 * Created by imozerov on 29.02.16.
 */
fun ImageView.loadImage(imagePath: String) {
    if (imagePath.isNotBlank()) {
        Picasso.with(context).load(imagePath).into(this)
    }
}

fun ImageView.loadImage(imageUri: Uri) {
    Picasso.with(context).load(imageUri).into(this)
}
