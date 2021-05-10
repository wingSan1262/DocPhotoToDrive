package com.example.simplegallery

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.net.Uri
import android.util.Size
import android.widget.*


class GridAdapter (mListImagesModel: MutableList<LoadingFragment.Images>,
                   context : Context) : BaseAdapter(){
    var listImagesModel : MutableList<LoadingFragment.Images>? = null
    var mContext : Context? = null

    init {
        listImagesModel = mListImagesModel
        mContext = context
    }


    override fun getItem(position: Int): Any {
        return 0;
    }

    override fun getItemId(position: Int): Long {
        return 0;
    }

    override fun getCount(): Int {
        return listImagesModel!!.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view : View = View.inflate(mContext, R.layout.grid_item_layout, null)

        val imageView = view.findViewById<ImageView>(R.id.image_view)

        var bitmap : Bitmap? = null

        bitmap = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            mContext?.contentResolver?.loadThumbnail(Uri.parse(listImagesModel?.get(position)!!.uri), Size(96,96), null)
        } else {
            listImagesModel?.get(position)?.id?.let {
                MediaStore.Images.Thumbnails.getThumbnail(mContext?.contentResolver,
                    it,
                    MediaStore.Images.Thumbnails.MINI_KIND, null)
            }
        }

        //TODO calculate

        imageView.layoutParams.height = 200
        imageView.layoutParams.width = 200
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageBitmap(bitmap)
        imageView.setPadding(4, 4, 4, 4)
        return view
    }

}