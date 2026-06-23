package com.musicplayer.localmusicplayer.data.local.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scanAudioFiles(): List<SongEntity> {
        val songs = mutableListOf<SongEntity>()
        val contentResolver: ContentResolver = context.contentResolver

        // Batch-query the genre map (mediaStoreId -> genre name) once.
        // MediaStore.Audio.Genres is a separate table joined by audio id; the main
        // audio MediaStore table does not expose a genre column.
        val genreMap = buildGenreMap(contentResolver)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 15000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val mediaStoreId = cursor.getLong(idColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId
                    )

                    songs.add(
                        SongEntity(
                            mediaStoreId = mediaStoreId,
                            title = cursor.getString(titleColumn)
                                ?: MediaStore.UNKNOWN_STRING,
                            artist = cursor.getString(artistColumn)
                                ?: MediaStore.UNKNOWN_STRING,
                            album = cursor.getString(albumColumn)
                                ?: MediaStore.UNKNOWN_STRING,
                            albumId = albumId,
                            duration = cursor.getLong(durationColumn),
                            filePath = cursor.getString(dataColumn),
                            contentUri = contentUri.toString(),
                            albumArtUri = albumArtUri,
                            dateAdded = cursor.getLong(dateAddedColumn),
                            year = cursor.getInt(yearColumn).takeIf { it > 0 },
                            trackNumber = cursor.getInt(trackColumn).takeIf { it > 0 }?.let { raw ->
                                if (raw >= 1000) raw % 1000 else raw
                            },
                            discNumber = cursor.getInt(trackColumn).takeIf { it >= 1000 }?.let { raw ->
                                raw / 1000
                            },
                            genre = genreMap[mediaStoreId],
                            mimeType = cursor.getString(mimeTypeColumn) ?: "audio/*",
                            size = cursor.getLong(sizeColumn)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaStoreDataSource", "Failed to scan audio files", e)
        }

        return songs
    }

    /**
     * Build a map of audio MediaStore id -> genre name by iterating the
     * MediaStore.Audio.Genres table and its members. Done once per scan instead
     * of per-song to keep it fast.
     */
    private fun buildGenreMap(contentResolver: ContentResolver): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        try {
            contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null, null, null
            )?.use { genreCursor ->
                val idCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genreCursor.moveToNext()) {
                    val genreId = genreCursor.getLong(idCol)
                    val genreName = genreCursor.getString(nameCol) ?: continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null, null, null
                    )?.use { memberCursor ->
                        val audioIdCol = memberCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (memberCursor.moveToNext()) {
                            map[memberCursor.getLong(audioIdCol)] = genreName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaStoreDataSource", "Failed to build genre map", e)
        }
        return map
    }
}
