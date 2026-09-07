package com.example.aiclipmaker.data.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object MediaStoreHelper {

    private const val RELATIVE_DIR = "Movies/AI Clip Maker"

    suspend fun saveVideoToGallery(
        context: Context,
        sourceFile: File,
        clipTitle: String
    ): Uri? = withContext(Dispatchers.IO) {
        val sanitizedTitle = clipTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.mp4"

        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, RELATIVE_DIR)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val destinationUri = resolver.insert(collection, contentValues) ?: return@withContext null

        try {
            resolver.openOutputStream(destinationUri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(destinationUri, contentValues, null, null)
            }

            destinationUri
        } catch (e: Exception) {
            e.printStackTrace()
            // Clean up partial insert on error
            try {
                resolver.delete(destinationUri, null, null)
            } catch (ignored: Exception) {}
            null
        }
    }
}
