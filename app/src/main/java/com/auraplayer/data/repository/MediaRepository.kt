package com.auraplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.auraplayer.data.model.MediaModel
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    companion object {
        private const val TAG = "MediaRepository"
    }

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
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val parseCursor: (android.database.Cursor) -> Unit = { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                if (id == 0L) continue

                val rawTitle = if (titleCol >= 0) cursor.getString(titleCol) ?: "Unknown Title" else "Unknown Title"
                val rawArtist = if (artistCol >= 0) cursor.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist"
                val rawAlbum = if (albumCol >= 0) cursor.getString(albumCol) ?: "Unknown Album" else "Unknown Album"
                val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                val data = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                val rawDateAdded = if (dateAddedCol >= 0) {
                    try { cursor.getLong(dateAddedCol) } catch (_: Exception) { 0L }
                } else 0L
                val dateAdded = if (rawDateAdded > 0L) rawDateAdded else {
                    try {
                        if (data.isNotBlank()) File(data).lastModified() / 1000L else 0L
                    } catch (_: Exception) { 0L }
                }

                // Filter out tiny sound effects or UI clicks (< 2s) if duration is known
                if (duration in 1..2000L) continue

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
                    try {
                        val file = File(data)
                        val fileNameNoExt = file.nameWithoutExtension
                        if ((finalArtist == "Artista desconocido" || finalArtist.isBlank()) && fileNameNoExt.contains(" - ")) {
                            finalArtist = fileNameNoExt.substringBefore(" - ").replace("_", " ").trim()
                            if (finalTitle.isBlank() || finalTitle == rawTitle || finalTitle.contains("_")) {
                                finalTitle = fileNameNoExt.substringAfter(" - ").replace("_", " ").trim()
                            }
                        }
                    } catch (e: Exception) { AppLog.d(TAG, "No se pudo interpretar artista/título desde el nombre del archivo", e) }
                }

                val album = if (rawAlbum.equals("<unknown>", ignoreCase = true) || rawAlbum.equals("Unknown Album", ignoreCase = true) || rawAlbum.isBlank()) {
                    "Álbum desconocido"
                } else rawAlbum

                val contentUri = ContentUris.withAppendedId(collection, id)
                
                // Check local saved persistent cover art first (by ID, parsed artist/title, raw title, path, etc.)
                val localCoverUri = coverArtManager.getLocalCoverUri(id, finalArtist, finalTitle, data)
                    ?: coverArtManager.getLocalCoverUri(id, rawArtist, rawTitle, data)

                val folderName = try {
                    if (data.isNotBlank()) File(data).parentFile?.name ?: "Música" else "Música"
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
                    size = size,
                    dateAdded = dateAdded
                )

                audioList.add(mediaModel)
            }
        }

        // Strategy 1: Standard broad query for Android MediaStore
        val primarySelection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= 2000 OR ${MediaStore.Audio.Media.DURATION} IS NULL)"
        try {
            context.contentResolver.query(collection, projection, primarySelection, null, sortOrder)?.use { cursor ->
                parseCursor(cursor)
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Fallo al consultar MediaStore para cargar canciones", e)
        }

        // Strategy 2 (Fallback): If query returned 0 items (OEM ROM restriction or provider issue), query without selection
        if (audioList.isEmpty()) {
            try {
                context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                    parseCursor(cursor)
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Fallo en la consulta de audio sin filtro (fallback)", e)
            }
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
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                    if (id == 0L) continue

                    val title = if (titleCol >= 0) cursor.getString(titleCol) ?: "Video" else "Video"
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val data = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                    // 1. Skip if empty, inside vault, or marked as hidden
                    if (data.isBlank() || data.contains(".secure_vault") || data.contains(".dave_vault") || vaultManager.isPathHidden(data)) {
                        continue
                    }

                    // 2. Skip if physical file does not exist on disk, and purge stale MediaStore entry
                    val file = File(data)
                    if (!file.exists() || file.length() == 0L) {
                        try {
                            val staleUri = ContentUris.withAppendedId(collection, id)
                            context.contentResolver.delete(staleUri, null, null)
                        } catch (e: Exception) { AppLog.d(TAG, "No se pudo eliminar la entrada obsoleta de video en MediaStore", e) }
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
            AppLog.e(TAG, "Fallo al consultar MediaStore para cargar videos", e)
        }

        videoList
    }

    suspend fun loadPhotoFiles(): List<MediaModel> = withContext(Dispatchers.IO) {
        val photoList = mutableListOf<MediaModel>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val vaultManager = VaultManager(context)

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                    if (id == 0L) continue

                    val title = if (titleCol >= 0) cursor.getString(titleCol) ?: "Foto" else "Foto"
                    val data = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                    if (data.isBlank() || data.contains(".secure_vault") || data.contains(".dave_vault") || vaultManager.isPathHidden(data)) {
                        continue
                    }

                    val file = File(data)
                    if (!file.exists() || file.length() == 0L) {
                        try {
                            val staleUri = ContentUris.withAppendedId(collection, id)
                            context.contentResolver.delete(staleUri, null, null)
                        } catch (e: Exception) { AppLog.d(TAG, "No se pudo eliminar la entrada obsoleta de foto en MediaStore", e) }
                        continue
                    }

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val folderName = try {
                        file.parentFile?.name ?: "Fotos"
                    } catch (_: Exception) {
                        "Fotos"
                    }

                    photoList.add(
                        MediaModel(
                            id = id,
                            title = title,
                            artist = folderName,
                            album = "Fotos",
                            duration = 0L,
                            uri = contentUri,
                            artworkUri = contentUri,
                            isVideo = false,
                            folderName = folderName,
                            path = data,
                            size = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Fallo al consultar MediaStore para cargar fotos", e)
        }

        photoList
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
            AppLog.w(TAG, "Fallo al eliminar el archivo físico del audio", e)
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
            AppLog.d(TAG, "No se pudo reescanear la ruta en MediaStore tras borrar", e)
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
            AppLog.d(TAG, "No se pudo limpiar la caché de carátula de la canción", e)
        }
    }

    suspend fun rescanDownloadDirectories() = withContext(Dispatchers.IO) {
        try {
            val pathsToScan = mutableListOf<String>()
            val extStorage = android.os.Environment.getExternalStorageDirectory()
            val candidateDirs = listOf(
                File(extStorage, "Download"),
                File(extStorage, "Download/Snaptube"),
                File(extStorage, "snaptube/download"),
                File(extStorage, "Music"),
                File(extStorage, "Music/Snaptube"),
                File(extStorage, "WhatsApp/Media/WhatsApp Audio"),
                File(extStorage, "Telegram/Telegram Audio")
            )
            for (dir in candidateDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && (file.extension.equals("mp3", true) ||
                                    file.extension.equals("m4a", true) ||
                                    file.extension.equals("flac", true) ||
                                    file.extension.equals("wav", true) ||
                                    file.extension.equals("aac", true) ||
                                    file.extension.equals("opus", true) ||
                                    file.extension.equals("ogg", true))) {
                            pathsToScan.add(file.absolutePath)
                        }
                    }
                }
            }
            if (pathsToScan.isNotEmpty()) {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    pathsToScan.toTypedArray(),
                    null,
                    null
                )
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "No se pudo reescanear las carpetas de descargas", e)
        }
    }
}

