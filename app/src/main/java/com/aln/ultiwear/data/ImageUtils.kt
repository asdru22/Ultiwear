package com.aln.ultiwear.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val tag = "ImageUtils"

suspend fun compressAndUpload(uri: Uri, path: String): String? =
    withContext(Dispatchers.IO) {
        try {
            val context = Firebase.app.applicationContext

            // load bitmap
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open URI")
            val bitmap = rotateBitmap(BitmapFactory.decodeStream(inputStream))

            // compress to WEBP
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, baos)
            val bytes = baos.toByteArray()

            // upload to firebase
            val ref = Firebase.storage.reference.child(path)
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e(tag, "compressAndUpload failed: ${e.message}")
            null
        }
    }


private fun rotateBitmap(bitmap: Bitmap, degrees: Float = 90.0f): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width,
        bitmap.height, matrix, true
    )
}

fun createImageUri(context: Context): Uri {
    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DISPLAY_NAME, "temp_${System.currentTimeMillis()}.jpg")
    }
    return contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
    )!!
}
