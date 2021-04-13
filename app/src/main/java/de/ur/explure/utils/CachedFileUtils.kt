package de.ur.explure.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

@Suppress("TooGenericExceptionCaught")
object CachedFileUtils {

    private const val PROVIDER_AUTHORITY = "com.explure.fileprovider"

    const val AUDIO_FILE_SUFFIX = ".3gp"
    const val VIDEO_FILE_SUFFIX = ".mp4"
    const val IMAGE_FILE_SUFFIX = ".jpg"

    fun getNewImageUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        val file = File.createTempFile(
            "JPEG_${timeStamp}_",
            IMAGE_FILE_SUFFIX,
            context.externalCacheDir
        )
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
    }

    fun getNewVideoUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        val file = File.createTempFile(
            "MP4_${timeStamp}_",
            VIDEO_FILE_SUFFIX,
            context.externalCacheDir
        )
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
    }

    fun getNewAudioFile(context: Context): File {
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        return File.createTempFile(
            "3GP_${timeStamp}_",
            AUDIO_FILE_SUFFIX,
            context.externalCacheDir
        )
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
    }

    fun getUriFromBitmap(context: Context, bitmap: Bitmap): Uri? {
        try {
            val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                IMAGE_FILE_SUFFIX,
                context.externalCacheDir
            )
            val fileOutputStream = file.outputStream()
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            fileOutputStream.write(byteArray)
            fileOutputStream.flush()
            fileOutputStream.close()
            byteArrayOutputStream.close()
            return getUriForFile(context, file)
        } catch (e: Exception) {
            Timber.d("Failed to convert Bitmap to Uri with $e")
            return null
        }
    }
}
