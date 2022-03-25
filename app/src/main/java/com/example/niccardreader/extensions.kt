package com.example.niccardreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

fun Context.T(msg: String) {
    GlobalScope.launch {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@T, msg, Toast.LENGTH_LONG).show()
        }
    }

}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun Context.copyUriToFile(uri: Uri, fileNameFromUri: String, mediaType: String): File? {
    val f = File(this.externalCacheDir, fileNameFromUri)
    try {
        val `in`: InputStream? = this.contentResolver.openInputStream(uri)
        val out: OutputStream = FileOutputStream(f)
        val buf = ByteArray(Constants.BYTE_SIZE)
        var len: Int
        if (`in` != null) {
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
        }
        out.close()
        `in`?.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    if (mediaType == Constants.MEDIA_TYPE_IMAGE) {
        f.compress()
    }
    return f

}

fun File.compress(): File? {
    try {

        // BitmapFactory options to downsize the image
        val o: BitmapFactory.Options = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        o.inSampleSize = 6
        // factor of downsizing the image

        var inputStream = FileInputStream(this)
        //Bitmap selectedBitmap = null;
        BitmapFactory.decodeStream(inputStream, null, o)
        inputStream.close()

        // The new size we want to scale to
        val REQUIRED_SIZE = 30

        // Find the correct scale value. It should be the power of 2.
        var scale = 1
        while (o.outWidth / scale / 2 >= REQUIRED_SIZE &&
            o.outHeight / scale / 2 >= REQUIRED_SIZE
        ) {
            scale *= 2
        }

        val o2: BitmapFactory.Options = BitmapFactory.Options()
        o2.inSampleSize = scale
        inputStream = FileInputStream(this)

        val selectedBitmap: Bitmap = BitmapFactory.decodeStream(inputStream, null, o2)!!
        inputStream.close()

        // here i override the original image file
        this.createNewFile()
        val outputStream = FileOutputStream(this)

        if (this.path.endsWith("png")) {
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 1, outputStream)
        } else {
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        }

        return this
    } catch (e: Exception) {
        return null
    }

}

fun Context.getFileNameFromUri(uri: Uri): String {
    var name = ""
    this.contentResolver
        .query(uri, null, null, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                )
                val size: String = cursor.getString(cursor.getColumnIndex(OpenableColumns.SIZE))
            }
        }
    return name
}

