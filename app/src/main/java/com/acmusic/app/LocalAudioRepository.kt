package com.acmusic.app

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

object LocalAudioRepository {
    fun load(context: Context): List<Track> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val result = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val rawTitle = cursor.getString(titleIndex)
                val fileName = cursor.getString(nameIndex)
                val title = rawTitle?.takeIf { it.isNotBlank() } ?: fileName ?: "Canción"
                val artist = cursor.getString(artistIndex)?.takeIf { it.isNotBlank() }
                    ?: "Artista desconocido"
                val uri = ContentUris.withAppendedId(collection, id)
                val albumId = cursor.getLong(albumIndex)
                val artwork = if(albumId > 0) "content://media/external/audio/albumart/" + albumId else null
                result += Track(title, artist, uri.toString(), true, artwork)
            }
        }
        return result
    }
}
