package de.ur.explure.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

@Suppress("ReturnCount")
fun Uri.getRealSize(context: Context): Long? {
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver
            .query(
                this,
                arrayOf(MediaStore.Audio.Media.SIZE),
                null,
                null,
                null
            ) ?: return null
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        cursor.moveToFirst()
        val result = cursor.getString(columnIndex)
        result.toLong()
    } finally {
        cursor?.close()
    }
}
