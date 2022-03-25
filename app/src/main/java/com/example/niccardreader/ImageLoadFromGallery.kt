package com.example.niccardreader

import android.net.Uri
import androidx.activity.result.ActivityResultCallback

abstract class ImageLoadFromGallery : ActivityResultCallback<Uri?> {
    lateinit var mOnResult: OnResult
    override fun onActivityResult(result: Uri?) {

        if (result != null) {
            mOnResult.onResult(result)
        } else {
            mOnResult.noResult()
        }
    }
}

interface OnResult {
    fun onResult(result: Uri?)
    fun noResult()

}