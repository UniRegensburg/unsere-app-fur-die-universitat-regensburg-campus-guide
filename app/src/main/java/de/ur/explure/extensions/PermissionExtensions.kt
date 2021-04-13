package de.ur.explure.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun hasCameraPermission(context: Context) = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.CAMERA
) == PackageManager.PERMISSION_GRANTED

fun hasAudioPermission(context: Context) = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.RECORD_AUDIO
) == PackageManager.PERMISSION_GRANTED

fun hasExternalReadPermission(context: Context) = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.READ_EXTERNAL_STORAGE
) == PackageManager.PERMISSION_GRANTED
