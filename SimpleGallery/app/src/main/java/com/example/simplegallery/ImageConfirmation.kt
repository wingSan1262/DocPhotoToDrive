package com.example.simplegallery

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import android.util.DisplayMetrics
import android.widget.ProgressBar
import android.widget.TextView
import com.example.simplegallery.DriveSingleton.Companion.mDriveService
import com.google.android.material.tabs.TabLayout
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.android.synthetic.main.confirmation_image_child.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList


class ImageConfirmation : AppCompatActivity(), UpdateListener {
    override fun updateList(int : Int, string : String) {
        val currentTime = Calendar.getInstance().getTime()
//        mArrayFileNameList.add(int, string + currentTime.toString() + int.toString())//To change body of created functions use File | Settings | File Templates.
        mArrayFileNameList[int] = "$string - $currentTime$int"
    }

    var currentDialog : Dialog? = null

    val mArrayFileNameList = ArrayList<String>()
    var mArrayImageList = ArrayList<LoadingFragment.Images>()

    var mHandler : Handler? = null

    var context : Context? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_confirmation_activity)
        val viewPager = findViewById<ViewPager>(R.id.view_pager)
        val tabLayout= findViewById<TabLayout>(R.id.tab_layout)
        context = this
        mArrayImageList = intent.getSerializableExtra(ConstantsDefine().CHOSEN_IMAGES_MODEL_LIST) as ArrayList<LoadingFragment.Images>
        for (i in 0..mArrayImageList.size-1){
            val currentTime = Calendar.getInstance().getTime()
            mArrayFileNameList.add("- $currentTime$i")
        }

        mHandler = object : Handler(this.mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    ConstantsDefine().UPLOAD_DONE -> {

                        val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(context)
                        val view = layoutInflater.inflate(R.layout.loading_fragment, null)
                        view.findViewById<TextView>(R.id.text_info).setText("Uploaded, Going Back.")
                        view.findViewById<ProgressBar>(R.id.progress_circular).setVisibility(View.INVISIBLE)
                        alertDialogBuilder.setView(view)
                        currentDialog = alertDialogBuilder.show()
                        currentDialog!!.setCanceledOnTouchOutside(false)
                        currentDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                        mHandler?.postDelayed({
                            currentDialog!!.dismiss()
                            finish()
                        }, 1000)
                    }
                }
            }
        }
        val mAdapter = DemoCollectionPagerAdapter (this.supportFragmentManager, mArrayImageList)
        mAdapter.mUpdateListener1 = this
        viewPager.adapter = mAdapter
        tabLayout.setupWithViewPager(viewPager)

        upload_file.setOnClickListener {
            val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.loading_fragment, null)
            view.findViewById<TextView>(R.id.text_info).setText("Uploading")
            alertDialogBuilder.setView(view)
            currentDialog = alertDialogBuilder.show()
            currentDialog!!.setCanceledOnTouchOutside(false)
            currentDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            uploadFile()
        }
    }

    private fun uploadFile() {
        var output : FileOutputStream? = null
        var bitmap : Bitmap? = null
        for (i in 0..mArrayImageList.size-1){
            output = FileOutputStream(getExternalFilesDir(null).toString()+ "/" + mArrayFileNameList[i]+".jpg")
            bitmap = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(this.contentResolver, Uri.parse(mArrayImageList?.get(i).uri))
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    Uri.parse(mArrayImageList.get(i).uri)
                )
            }

            bitmap?.compress(Bitmap.CompressFormat.JPEG, 50, output)
        }


        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {

            // list all the folder
            var targetFolderName : String? = null
            var targetFolderId : String? = null

            var pageToken : String? = null
            do {
                val result = mDriveService?.files()?.list()
                    ?.setQ("mimeType='application/vnd.google-apps.folder'")
                    ?.setSpaces("drive")
                    ?.setFields("nextPageToken, files(id, name)")
                    ?.setPageToken(pageToken)
                    ?.execute()
                if (result != null) {
                    for (file in result.files){
                        System.out.printf("Found file: %s (%s)\n",
                            file.getName(), file.getId());
                        if (file.name.equals("QuickDocumentDrive")){
                            targetFolderId = file.id
                            targetFolderName = file.name
                            pageToken = null
                            break
                        } else {
                            pageToken = result.nextPageToken
                        }
                    }
                }
            } while (pageToken != null)

            // create file
            for (i in 0..mArrayImageList.size-1){
                val fileMetaData = File()
                fileMetaData.setName(mArrayFileNameList[i] + ".jpg")
                fileMetaData.setParents(Collections.singletonList(targetFolderId))

                val filePath = java.io.File(getExternalFilesDir(null).toString()+ "/"+mArrayFileNameList[i]+".jpg")
                val mediaContent = FileContent("image/jpeg", filePath)
                val file = mDriveService?.files()?.create(fileMetaData, mediaContent)
                    ?.setFields("id, parents")
                    ?.execute()
                System.out.println("File ID: " + file?.getId())
                mHandler?.sendEmptyMessage(ConstantsDefine().UPLOAD_DONE)
            }

        }
    }

    override fun onBackPressed() {
        setResult(332, intent)
        finish()
        super.onBackPressed()
    }

}

class DemoCollectionPagerAdapter(fm: FragmentManager,
                                 imageModelsList : ArrayList<LoadingFragment.Images>) : FragmentStatePagerAdapter(fm), UpdateListener {
    override fun updateList(int: Int, string: String) {
        mUpdateListener1?.updateList(int, string)//To change body of created functions use File | Settings | File Templates.
    }

    var mUpdateListener1 : UpdateListener? = null

    var mImageModelsList : ArrayList<LoadingFragment.Images>? = null
    init {
        mImageModelsList = imageModelsList
    }
    override fun getCount(): Int = (
            mImageModelsList!!.size)

    override fun getItem(i: Int): Fragment {
        val fragment = DemoObjectFragment()
        fragment.mUpdateListener2 = this
        fragment.arguments = Bundle().apply {
            // Our object is just an integer :-P
            putInt(ConstantsDefine().FRAGMENT_POSITION, i)
            putSerializable(ConstantsDefine().CHOSEN_IMAGES_MODEL_LIST, mImageModelsList)
        }
        return fragment
    }


}

class DemoObjectFragment  : Fragment() {

    var mUpdateListener2 : UpdateListener? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val mView = inflater.inflate(R.layout.confirmation_image_child, container, false)
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(ConstantsDefine().FRAGMENT_POSITION) and
                it.containsKey(ConstantsDefine().CHOSEN_IMAGES_MODEL_LIST)}?.apply {
            var mImageView : ImageView = view.findViewById(R.id.image_view_in_pager)
            var bitmap : Bitmap? = null
            var position = getInt(ConstantsDefine().FRAGMENT_POSITION)
            var listImagesModel = getSerializable(ConstantsDefine().CHOSEN_IMAGES_MODEL_LIST) as ArrayList<LoadingFragment.Images>
            file_name.addTextChangedListener(object : TextWatcher{
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    //To change body of created functions use File | Settings | File Templates.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    mUpdateListener2?.updateList(position, s.toString())
                }
            })

            bitmap = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context!!.contentResolver, Uri.parse(listImagesModel?.get(position).uri))
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(
                    context?.contentResolver,
                    Uri.parse(listImagesModel?.get(position).uri)
                )
            }

            val displaymetrics = DisplayMetrics()
            activity?.getWindowManager()?.getDefaultDisplay()?.getMetrics(displaymetrics)
            val width = displaymetrics.widthPixels
            val bitmapHeight = bitmap?.height
            val bitmapWidth = bitmap?.width
            val factor : Float = width.toFloat() / bitmapWidth!!.toFloat()
            val factoredHeight = bitmapHeight?.toFloat()?.times(factor)
            bitmap = Bitmap.createScaledBitmap(bitmap!!, width, factoredHeight!!.toInt(), false)
            mImageView.setImageBitmap(bitmap)
        }
    }
}

//Drive Activity
interface UpdateListener {
    fun updateList(int: Int, string : String)
}
