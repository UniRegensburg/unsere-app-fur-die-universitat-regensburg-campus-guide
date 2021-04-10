package de.ur.explure.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CachedFileUtils {

    private const val PROVIDER_AUTHORITY = "com.explure.fileprovider"

    fun getImageUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
        val file = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            context.externalCacheDir
        )
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
    }
}
