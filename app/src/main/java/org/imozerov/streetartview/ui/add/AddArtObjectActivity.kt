import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.content_add_art_object.*
import org.imozerov.streetartview.R
import org.imozerov.streetartview.StreetArtViewApp
import org.imozerov.streetartview.ui.add.AddArtObjectPresenter
import org.imozerov.streetartview.ui.add.AddArtObjectView
import org.imozerov.streetartview.ui.extensions.loadImage


/**
 * Created by sergei on 18.02.16.
 */
class AddArtObjectActivity : AppCompatActivity(), AddArtObjectView {
    val TAG = "AddArtObjectActivity"

    val RESULT_LOAD_IMAGE_FROM_GALLERY = 1
    val RESULT_CAPTURE_IMAGE_WITH_CAMERA = 2

    private var presenter: AddArtObjectPresenter? = null

    private var selectedImageUrl: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate($savedInstanceState)")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_art_object)

        presenter = AddArtObjectPresenter()
        presenter!!.bindToView(this, application as StreetArtViewApp)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.add_art_object_map) as SupportMapFragment
        mapFragment.getMapAsync(presenter)

        add_art_object_from_gallery_button.setOnClickListener{ pickImage () }
        add_art_object_camera_button.setOnClickListener{ takeImage () }

        add_art_object_save_button.setOnClickListener {
            presenter!!.addArtObject(
                    authorName = add_art_object_author.text.toString(),
                    artObjectName = add_art_object_name.text.toString(),
                    artObjectDescription = add_art_object_description.text.toString(),
                    imageUrl = selectedImageUrl.toString()
            )
        }
    }

    override fun onStart() {
        super.onStart()
        presenter!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter!!.onStop()
    }

    override fun onDestroy() {
        presenter = null
        super.onDestroy()
    }

    override fun onArtObjectAdded() {
        finish()
    }

    fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, RESULT_LOAD_IMAGE_FROM_GALLERY)
    }

    fun takeImage() {
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, RESULT_CAPTURE_IMAGE_WITH_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == RESULT_LOAD_IMAGE_FROM_GALLERY || requestCode == RESULT_CAPTURE_IMAGE_WITH_CAMERA)
                && resultCode == Activity.RESULT_OK && null != data) {
            add_art_object_image.loadImage(data.data!!)
        }
    }
}
