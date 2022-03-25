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
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.*
import com.google.mlkit.nl.languageid.LanguageIdentification
import java.io.ByteArrayOutputStream

class ReaderFragment : Fragment(), ActivityResultCallback<Uri?> {
    private lateinit var getImageUri: ActivityResultLauncher<String>
    private lateinit var mBinding: FragmentReaderBinding
    private lateinit var functions: FirebaseFunctions

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
        functions = Firebase.functions
        var bitmap: Bitmap =
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, result)
        bitmap = scaleBitmapDown(bitmap, 640)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        // Create json request to cloud vision
        val request = JsonObject()
        // Add image to request
        val image = JsonObject()
        image.add("content", JsonPrimitive(base64encoded))
        request.add("image", image)
        //Add features to the request
        val feature = JsonObject()
        feature.add("type", JsonPrimitive("TEXT_DETECTION"))
        // Alternatively, for DOCUMENT_TEXT_DETECTION:
        // feature.add("type", JsonPrimitive("DOCUMENT_TEXT_DETECTION"))
        val features = JsonArray()
        features.add(feature)
        request.add("features", features)
        val imageContext = JsonObject()
        val languageHints = JsonArray()
        languageHints.add("en")
        imageContext.add("languageHints", languageHints)
        request.add("imageContext", imageContext)

        annotateImage(request.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    readData(task)
                } else {
                    requireContext().T("Unable to fetch")
                }

            }


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

    private fun readData(task: Task<JsonElement?>) {
        val annotation =
            task.result!!.asJsonArray[0].asJsonObject["fullTextAnnotation"].asJsonObject
        for (page in annotation["pages"].asJsonArray) {
            var pageText = ""
            for (block in page.asJsonObject["blocks"].asJsonArray) {
                var blockText = ""
                for (para in block.asJsonObject["paragraphs"].asJsonArray) {
                    var paraText = ""
                    for (word in para.asJsonObject["words"].asJsonArray) {
                        var wordText = ""
                        for (symbol in word.asJsonObject["symbols"].asJsonArray) {
                            wordText += symbol.asJsonObject["text"].asString
                            System.out.format(
                                "Symbol text: %s (confidence: %f)%n",
                                symbol.asJsonObject["text"].asString,
                                symbol.asJsonObject["confidence"].asFloat
                            )
                        }
                        System.out.format(
                            "Word text: %s (confidence: %f)%n%n", wordText,
                            word.asJsonObject["confidence"].asFloat
                        )
                        System.out.format(
                            "Word bounding box: %s%n",
                            word.asJsonObject["boundingBox"]
                        )
                        paraText = String.format("%s%s ", paraText, wordText)
                    }
                    System.out.format("%nParagraph: %n%s%n", paraText)
                    System.out.format(
                        "Paragraph bounding box: %s%n",
                        para.asJsonObject["boundingBox"]
                    )
                    System.out.format(
                        "Paragraph Confidence: %f%n",
                        para.asJsonObject["confidence"].asFloat
                    )
                    blockText += paraText
                }
                pageText += blockText
            }
        }
    }
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