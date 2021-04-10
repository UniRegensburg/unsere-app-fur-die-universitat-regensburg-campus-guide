package de.ur.explure.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*

object CachedFileUtils {

    fun getImageUri(context: Context): Uri {
        val newFile = File(context.externalCacheDir,  "explure_image.jpg")
        newFile.createNewFile()
        return Uri.fromFile(newFile)
    }
}