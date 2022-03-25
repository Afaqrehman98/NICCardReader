package com.example.niccardreader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

class GalleryContract : ActivityResultContract<String, Uri?>() {

    override fun createIntent(context: Context, input: String?): Intent {
        return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = when {
        resultCode != Activity.RESULT_OK -> null // Return null, if action is cancelled
        else -> intent?.data      // Return the data
    }
}