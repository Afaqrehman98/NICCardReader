package com.example.niccardreader

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.example.niccardreader.databinding.FragmentReaderBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.mlkit.nl.languageid.LanguageIdentification
import java.io.ByteArrayOutputStream

class ReaderFragment : Fragment(), ActivityResultCallback<Uri?> {
    private lateinit var getImageUri: ActivityResultLauncher<String>
    private lateinit var mBinding: FragmentReaderBinding
    private lateinit var functions: FirebaseFunctions
    private val languageIdentifier = LanguageIdentification.getClient()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentReaderBinding.inflate(layoutInflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getImageUri = registerForActivityResult(GalleryContract(), this)
        setListeners()
    }

    private fun setListeners() {
        mBinding.btnNic.setOnClickListener {
            openGallery()
        }
    }

    override fun onActivityResult(result: Uri?) {
        if (result != null) {
            onResultFromActivity(result)
        } else {
            requireContext().T("No Image selected")
        }
    }

    private fun onResultFromActivity(result: Uri?) {
        mBinding.btnNic.gone()
        mBinding.ivSelectedImage.show()
        var bitmap: Bitmap =
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, result)
        bitmap = scaleBitmapDown(bitmap, 640)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
//        val imageFile = result?.let { uri ->
//            requireContext().copyUriToFile(
//                uri,
//                requireContext().getFileNameFromUri(uri),
//                Constants.MEDIA_TYPE_IMAGE
//            )
//        }
//        val imageUri = BitmapFactory.decodeFile(imageFile?.absolutePath)
//        mBinding.ivSelectedImage.setImageBitmap(imageUri)
//        runTextRecognition(result)
    }

    private fun annotateImage(requestJson: String): Task<JsonElement> {
        return functions
            .getHttpsCallable("annotateImage")
            .call(requestJson)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data
                JsonParser.parseString(Gson().toJson(result))
            }
    }

    private fun openGallery() {
        getImageUri.launch("")

    }

//    private fun runTextRecognition(uri: Uri?) {
//
//        val stream = uri?.let { requireContext().contentResolver.openInputStream(it) }
//        val bitmap = BitmapFactory.decodeStream(stream)
//        val image = InputImage.fromBitmap(bitmap, 0)
//        functions = Firebase.functions
//        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//        recognizer.process(image)
//            .addOnSuccessListener { texts ->
//                processTextRecognitionResult(texts)
//            }
//            .addOnFailureListener { e -> // Task failed with an exception
//                e.printStackTrace()
//            }
//    }

//    private fun processTextRecognitionResult(texts: Text?) {
//        val blocks = texts?.textBlocks
//        if (blocks?.size == 0) {
//            requireContext().T("No text found")
//            return
//        }
////        mGraphicOverlay.clear()
//        for (i in blocks?.indices!!) {
//            val lines = blocks[i]?.lines
//            if (lines != null) {
//                for (j in lines.indices) {
//                    val elements = lines[j].elements
//                    Log.e("MainActivity", "The data at element$elements")
//                    for (k in elements.indices) {
//                        checkLanguage(elements[k].text)
//                        Log.e(ReaderFragment::class.java.simpleName, "${elements[k].text}")
//                        //                    val textGraphic: Graphic = TextGraphic(mGraphicOverlay)
//                        //                    mGraphicOverlay.add(textGraphic)
//                    }
//                }
//            }
//        }
//    }

//    private fun checkLanguage(text: String?) {
//        if (text != null) {
//            languageIdentifier.identifyLanguage(text)
//                .addOnSuccessListener { languageCode ->
//                    when (languageCode) {
//                        "ur" -> {
//                            Log.e(ReaderFragment::class.java.simpleName, "The language is urdu")
//                        }
//                        "en" -> {
//                            Log.e(ReaderFragment::class.java.simpleName, "The language is english")
//                        }
//                        else -> {
//                            Log.e(
//                                ReaderFragment::class.java.simpleName,
//                                "Could not verify language"
//                            )
//                        }
//                    }
//
//                }
//                .addOnFailureListener {
//                    it.printStackTrace()
//                }
//        }
//    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        when {
            originalHeight > originalWidth -> {
                resizedHeight = maxDimension
                resizedWidth =
                    (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
            }
            originalWidth > originalHeight -> {
                resizedWidth = maxDimension
                resizedHeight =
                    (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
            }
            originalHeight == originalWidth -> {
                resizedHeight = maxDimension
                resizedWidth = maxDimension
            }
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }
}