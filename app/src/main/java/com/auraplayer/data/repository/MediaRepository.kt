package com.auraplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    val coverArtManager = CoverArtManager(context)

    suspend fun loadAudioFiles(): List<MediaModel> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<MediaModel>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol) ?: "Unknown Title"
                    val rawArtist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val data = cursor.getString(dataCol) ?: ""
                    val size = cursor.getLong(sizeCol)

                    // Clean artist & title if MediaStore indexed file as "Artist - Title" with unknown artist
                    var finalArtist = if (rawArtist.equals("<unknown>", ignoreCase = true) || rawArtist.equals("Unknown Artist", ignoreCase = true) || rawArtist.isBlank()) {
                        "Artista desconocido"
                    } else rawArtist

                    var finalTitle = rawTitle
                    if ((finalArtist == "Artista desconocido" || finalArtist.startsWith("Unknown")) && rawTitle.contains(" - ")) {
                        finalArtist = rawTitle.substringBefore(" - ").trim()
                        finalTitle = rawTitle.substringAfter(" - ").trim()
                    }

                    // Also check if filename has "Artist - Title" (common for downloaded songs)
                    if (data.isNotBlank()) {
                        val file = File(data)
                        val fileNameNoExt = file.nameWithoutExtension
                        if ((finalArtist == "Artista desconocido" || finalArtist.isBlank()) && fileNameNoExt.contains(" - ")) {
                            finalArtist = fileNameNoExt.substringBefore(" - ").replace("_", " ").trim()
                            if (finalTitle.isBlank() || finalTitle == rawTitle || finalTitle.contains("_")) {
                                finalTitle = fileNameNoExt.substringAfter(" - ").replace("_", " ").trim()
                            }
                        }
                    }

                    val album = if (rawAlbum.equals("<unknown>", ignoreCase = true) || rawAlbum.equals("Unknown Album", ignoreCase = true) || rawAlbum.isBlank()) {
                        "Álbum desconocido"
                    } else rawAlbum

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    
                    // Check local saved persistent cover art first (by ID, parsed artist/title, raw title, path, etc.)
                    val localCoverUri = coverArtManager.getLocalCoverUri(id, finalArtist, finalTitle, data)
                        ?: coverArtManager.getLocalCoverUri(id, rawArtist, rawTitle, data)

                    val folderName = try {
                        File(data).parentFile?.name ?: "Música"
                    } catch (e: Exception) {
                        "Música"
                    }

                    val mediaModel = MediaModel(
                        id = id,
                        title = finalTitle,
                        artist = finalArtist,
                        album = album,
                        duration = duration,
                        uri = contentUri,
                        artworkUri = localCoverUri,
                        isVideo = false,
                        folderName = folderName,
                        path = data,
                        size = size
                    )

                    audioList.add(mediaModel)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        audioList
    }

    suspend fun loadVideoFiles(): List<MediaModel> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<MediaModel>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val vaultManager = VaultManager(context)

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Video"
                    val duration = cursor.getLong(durationCol)
                    val data = cursor.getString(dataCol) ?: ""
                    val size = cursor.getLong(sizeCol)

                    // 1. Skip if empty, inside vault, or marked as hidden
                    if (data.isBlank() || data.contains(".secure_vault") || vaultManager.isPathHidden(data)) {
                        continue
                    }

                    // 2. Skip if physical file does not exist on disk, and purge stale MediaStore entry
                    val file = File(data)
                    if (!file.exists() || file.length() == 0L) {
                        try {
                            val staleUri = ContentUris.withAppendedId(collection, id)
                            context.contentResolver.delete(staleUri, null, null)
                        } catch (_: Exception) {}
                        continue
                    }

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    val folderName = try {
                        file.parentFile?.name ?: "Videos"
                    } catch (e: Exception) {
                        "Videos"
                    }

                    videoList.add(
                        MediaModel(
                            id = id,
                            title = title,
                            artist = folderName,
                            album = "Videos",
                            duration = duration,
                            uri = contentUri,
                            artworkUri = contentUri,
                            isVideo = true,
                            folderName = folderName,
                            path = data,
                            size = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList
    }

    val favoritesManager = FavoritesManager(context)

    suspend fun deleteAudioFile(song: MediaModel): Boolean = withContext(Dispatchers.IO) {
        var fileDeleted = false
        var resolverDeleted = false

        // 1. Try physical file deletion
        try {
            if (song.path.isNotEmpty()) {
                val file = File(song.path)
                if (file.exists()) {
                    fileDeleted = file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Try MediaStore content resolver deletion
        try {
            val rows = context.contentResolver.delete(song.uri, null, null)
            if (rows > 0) resolverDeleted = true
        } catch (e: Exception) {
            // Ignored if handled via MediaStore.createDeleteRequest
        }

        // 3. Scan file path to synchronize MediaStore
        try {
            if (song.path.isNotEmpty()) {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(song.path),
                    null,
                    null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Clean local artwork cache
        cleanupSongCache(song)

        fileDeleted || resolverDeleted
    }

    fun cleanupSongCache(song: MediaModel) {
        try {
            val coverById = File(context.filesDir, "covers/${song.id}.jpg")
            if (coverById.exists()) coverById.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

