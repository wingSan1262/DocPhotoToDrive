package com.example.simplegallery

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.GridView
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_gallery.*
import android.provider.MediaStore
import android.net.Uri
import android.os.Environment
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File


class GalleryActivity : AppCompatActivity() {

    var gridView:GridView? = null
    var mHandler : Handler? = null;
    var mConstans : ConstantsDefine? = null
    var currentFragment : Fragment? = null
    var isMultipleChoosing = false
    val imageList = arrayListOf<LoadingFragment.Images>()
    var floatingActionButton : FloatingActionButton? = null
    var floatingActionButtonRefresh : FloatingActionButton? = null
    val chosenImageViewList = arrayListOf<View>()
    var currentDialog : Dialog? = null
    var tempCameraImageHolder : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        mConstans = ConstantsDefine()
        floatingActionButton = findViewById(R.id.floating_action_but_next)
        floatingActionButton?.setImageResource(R.drawable.ic_navigate_next_black_24dp)
        floatingActionButtonRefresh = findViewById(R.id.reset_multiple_floating_button)
        floatingActionButtonRefresh?.setImageResource(R.drawable.ic_refresh_black_24dp)
        gridView = findViewById(R.id.grid_view)
        mHandler = object : Handler(this.mainLooper){
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                if (msg != null) {
                    when (msg.what) {
                        mConstans!!.RETRIEVING_IMAGES_LIST_END -> {
                            val mImagesList = msg.obj
                            //TODO : check mImageList getting image
                            loadGalleryView(mImagesList as MutableList<LoadingFragment.Images>)
                        }
                        }
                    }
            }
        }


        bottomNavigationView.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.home -> {
                    //do nothing ? ? ?}
                }
                R.id.camera -> {
                    // camera mediastore tools
                    val dialogBuilder = AlertDialog.Builder(this)
                    val view = View.inflate(this, R.layout.camera_confirmation_dialog, null)
                    dialogBuilder.setView(view)
                    view.findViewById<TextView>(R.id.ok_button).setOnClickListener{
                        startCamera()
                        currentDialog?.dismiss()
                        currentDialog = null
                    }
                    view.findViewById<TextView>(R.id.cancel).setOnClickListener {
                        val button = bottomNavigationView.findViewById<View>(R.id.home)
                        button.performClick()
                        currentDialog?.dismiss()
                        currentDialog = null
                    }
                    currentDialog = dialogBuilder.show()
                }
                R.id.service -> {}
                R.id.info -> {}
            }
            true
        }


        gridView?.onItemLongClickListener = OnItemLongClickListener { arg0, arg1, pos, id ->
            if(imageList.size == 0 || isMultipleChoosing){
                val mView : View = arg1
                val mCheckCircle = mView.findViewById<ImageView>(R.id.chosen_check)
                if (mCheckCircle.visibility == View.GONE){
                    mCheckCircle.visibility = View.VISIBLE
                    isMultipleChoosing = true
                    val chosenImageAdapter = arg0.adapter as GridAdapter
                    val imageModel = chosenImageAdapter.listImagesModel?.get(pos) as LoadingFragment.Images
                    imageList.add(imageModel)
                    chosenImageViewList.add(mView)
                    floatingActionButton!!.visibility = View.VISIBLE
                    floatingActionButtonRefresh!!.visibility = View.VISIBLE
                } else {
                    // delete the list
                    val chosenImageAdapter = arg0.adapter as GridAdapter
                    val imageModel = chosenImageAdapter.listImagesModel?.get(pos) as LoadingFragment.Images
                    if (imageList.contains(imageModel)){
                        imageList.remove(imageModel)
                        mCheckCircle.visibility = View.GONE
                        chosenImageViewList.remove(mView)
                    }
                    if(imageList.size == 0){
                        isMultipleChoosing = false
                        floatingActionButton!!.visibility = GONE
                        floatingActionButtonRefresh!!.visibility = View.GONE
                    }
                }
            }

            true
        }

        gridView?.onItemClickListener = AdapterView.OnItemClickListener { arg0, arg1, pos, id ->
            // TODO Auto-generated method stub
            if (imageList.size == 0 || !isMultipleChoosing ){
                isMultipleChoosing = false
                val chosenImageAdapter = arg0.adapter as GridAdapter
                val imageModel = chosenImageAdapter.listImagesModel?.get(pos) as LoadingFragment.Images
                imageList.add(imageModel)
                startConfirmationActivity()
            }
        }
        floatingActionButton!!.setOnClickListener {
            startConfirmationActivity()
        }
        floatingActionButtonRefresh!!.setOnClickListener {
            resetChosenImage()
            floatingActionButton!!.visibility = View.GONE
            floatingActionButtonRefresh!!.visibility = View.GONE
        }


        retrieveImageGalleryObject(mHandler as Handler)

    }

    private fun startCamera() {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("temp", ".jpg", storageDir)
        tempCameraImageHolder = imageFile.absolutePath
        val photoURI: Uri = FileProvider.getUriForFile(this, "com.example.simplegallery.fileprovider", imageFile)
        val cInt = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cInt.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(cInt, ConstantsDefine().CAMERA_ACTIVITY)
    }

    fun loadGalleryView(mImageList : MutableList<LoadingFragment.Images>){

        gridView?.numColumns = 3
        gridView?.adapter = GridAdapter(mImageList, this)
        // removing current fragment, because previously call dialog fragment
        removeFragmentDialog()
    }

    fun retrieveImageGalleryObject(handler: Handler) {
        val fragmentManager = supportFragmentManager
        val getImagesDialogFragmet : LoadingFragment = LoadingFragment(handler)
        currentFragment = getImagesDialogFragmet
        getImagesDialogFragmet.show(fragmentManager, "dialog")
    }

    fun removeFragmentDialog() {
        currentFragment?.let { this.supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss() }
    }

    fun startConfirmationActivity () {
        val intent = Intent(this, ImageConfirmation::class.java)
        intent.putExtra(ConstantsDefine().CHOSEN_IMAGES_MODEL_LIST, imageList)
        startActivityForResult(intent,ConstantsDefine().CONFIRMATION_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            ConstantsDefine().CONFIRMATION_IMAGE -> {
                //TODO i am not sure with this
                if (resultCode == ConstantsDefine().RESULT_OK){
                    resetChosenImage()
                } else if (resultCode == ConstantsDefine().RESULT_CANCEL) {
                    resetChosenImage()
                } else {
                    //
                }
            }

            ConstantsDefine().CAMERA_ACTIVITY ->{
                if (resultCode == RESULT_OK){
                    val file : File? = File(tempCameraImageHolder!!)
                    val image = LoadingFragment.Images(Uri.fromFile(file).toString(), "", 0, -1, -1)
                    imageList.add(image)
                    startConfirmationActivity()
                }
            }
        }
    }

    fun resetChosenImage(){
        chosenImageViewList.forEach {
            it.findViewById<ImageView>(R.id.chosen_check).visibility = View.GONE
        }
        chosenImageViewList.clear()
        imageList.clear()
        val button = bottomNavigationView.findViewById<View>(R.id.home)
        button.performClick()
    }
}
