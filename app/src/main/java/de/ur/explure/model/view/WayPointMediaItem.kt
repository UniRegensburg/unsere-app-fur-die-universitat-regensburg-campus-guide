package de.ur.explure.model.view

import android.net.Uri

open class WayPointMediaItem()

data class WayPointImageItem(val uri: Uri?) : WayPointMediaItem()

data class WayPointVideoItem(val uri: Uri?) : WayPointMediaItem()

data class WayPointAudioItem(val uri: Uri?) : WayPointMediaItem()
