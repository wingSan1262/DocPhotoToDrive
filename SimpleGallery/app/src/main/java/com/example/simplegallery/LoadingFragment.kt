package com.example.simplegallery

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.*
import java.io.Serializable

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [LoadingFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [LoadingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoadingFragment(mHandler : Handler) : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var mView : View? = null
    private var mHandlerCallBack : Handler? = null

    // video data object
    data class Images (val uri: String,
                       val name: String,
                       val takenDate: Int,
                       val size: Int,
                       val id: Long
    ) : Serializable

    var imagesList = mutableListOf<Images>()

    init {
        mHandlerCallBack = mHandler
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.loading_fragment, container, false)
    }

    fun closeDialog(myImageList : MutableList<Images>){
        val message = mHandlerCallBack?.obtainMessage(ConstantsDefine().RETRIEVING_IMAGES_LIST_END, myImageList)
        if (message != null) {
            mHandlerCallBack?.sendMessage(message)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mView = view
        isCancelable = false
        this.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // querying start
        val queryImage = QueryImage(this)
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            val myImagesList = queryImage.queryingMyImages()
            myImagesList.let {
                closeDialog(myImagesList)
            }
        }
    }

    // class querying media store, run using coroutine
    class QueryImage(fragment: LoadingFragment) {
        var mContext : Context? = null
        var mFragment : LoadingFragment? = null

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE
        )

        var query : Cursor? = null

        // initializer
        init {
            mContext = fragment.context
            mFragment = fragment
            query = mContext?.contentResolver?.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null)
        }

        fun queryingMyImages() : MutableList<Images> {
            val mLocalImageList = mutableListOf<Images>()
            query?.use { cursor ->
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateTaken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val takenDate = cursor.getInt(dateTaken)
                    val size = cursor.getInt(sizeColumn)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    // Stores column values and the contentUri in a local object
                    // that represents the media file.
                    mLocalImageList.add(Images(contentUri.toString(), name, takenDate, size, id))

                }
            }
            return mLocalImageList
        }


        //this method is actually fetching the json string
        fun queryingImages() {
            class queryingImages : AsyncTask<Void, Void, MutableList<Images>>() {
                //this method will be called before execution
                //you can display a progress bar or something
                //so that user can understand that he should wait
                //as network operation may take some time
                override fun onPreExecute() {
                    super.onPreExecute()
                }

                //this method will be called after execution
                //so here we are displaying a toast with the json string
                override fun onPostExecute(result: MutableList<Images>) {
                    mFragment?.imagesList?.addAll(result)
                    mFragment?.closeDialog(result)
                }

                //in this method we are fetching the json string
                override fun doInBackground(vararg voids: Void): MutableList<Images>? {

                    val mLocalImageList = mutableListOf<Images>()

                    query?.use { cursor ->
                        // Cache column indices.
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val nameColumn =
                            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                        val dateTaken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                        while (cursor.moveToNext()) {
                            // Get values of columns for a given video.
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)
                            val takenDate = cursor.getInt(dateTaken)
                            val size = cursor.getInt(sizeColumn)

                            val contentUri: Uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                            // Stores column values and the contentUri in a local object
                            // that represents the media file.
                            mLocalImageList.add(Images(contentUri.toString(), name, takenDate, size, id))

                        }
                    }

                    return mLocalImageList
                }
            }
            queryingImages().execute()
        }


    }





    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoadingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String, handler: Handler) =
            LoadingFragment(handler).apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
