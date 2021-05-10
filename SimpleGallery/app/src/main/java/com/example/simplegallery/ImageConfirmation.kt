package com.example.simplegallery

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.google.android.material.tabs.TabLayout


class ImageConfirmation : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_confirmation_activity)
        val viewPager = findViewById<ViewPager>(R.id.view_pager)
        val tabLayout= findViewById<TabLayout>(R.id.tab_layout)
        viewPager.adapter = DemoCollectionPagerAdapter (this.supportFragmentManager,
            intent.getSerializableExtra(ConstantsDefine().CHOSEN_IMAGES_MODEL_LIST) as ArrayList<LoadingFragment.Images>)
        tabLayout.setupWithViewPager(viewPager)
    }

    override fun onBackPressed() {
        setResult(332, intent)
        finish()
        super.onBackPressed()
    }

}

class DemoCollectionPagerAdapter(fm: FragmentManager, imageModelsList : ArrayList<LoadingFragment.Images>) : FragmentStatePagerAdapter(fm) {

    var mImageModelsList : ArrayList<LoadingFragment.Images>? = null
    init {
        mImageModelsList = imageModelsList
    }
    override fun getCount(): Int = (
            mImageModelsList!!.size)

    override fun getItem(i: Int): Fragment {
        val fragment = DemoObjectFragment()
        fragment.arguments = Bundle().apply {
            // Our object is just an integer :-P
            putInt(ConstantsDefine().FRAGMENT_POSITION, i)
            putSerializable(ConstantsDefine().CHOSEN_IMAGES_MODEL_LIST, mImageModelsList)
        }
        return fragment
    }
}

class DemoObjectFragment : Fragment() {

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
