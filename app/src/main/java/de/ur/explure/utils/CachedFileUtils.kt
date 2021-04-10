package de.ur.explure.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CachedFileUtils {

    private const val PROVIDER_AUTHORITY = "com.explure.fileprovider"

    fun getNewImageUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        val file = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            context.externalCacheDir
        )
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
    }

    fun getNewVideoUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        val file = File.createTempFile(
            "MP4_${timeStamp}_",
            ".mp4",
            context.externalCacheDir
        )
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
    }

    fun getNewAudioFile(context: Context): File {
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        return File.createTempFile(
            "M4A_${timeStamp}_",
            ".m4a",
            context.externalCacheDir
        )
    }
}
