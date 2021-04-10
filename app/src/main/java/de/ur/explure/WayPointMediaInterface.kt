package de.ur.explure

import de.ur.explure.model.view.WayPointAudioItem
import de.ur.explure.model.view.WayPointImageItem
import de.ur.explure.model.view.WayPointMediaItem
import de.ur.explure.model.view.WayPointVideoItem

interface WayPointMediaInterface {

    fun showImageMedia(mediaItem: WayPointImageItem)

    fun showVideoMedia(mediaItem: WayPointVideoItem)

    fun playAudioMedia(mediaItem: WayPointAudioItem)

    fun removeMediaItem(item: WayPointMediaItem)
}
