package com.auraplayer.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class CoverArtManager(private val context: Context) {

    private val coversDir = File(context.filesDir, "covers").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Returns a local saved cover Uri if available, otherwise null.
     */
    fun getLocalCoverUri(id: Long, artist: String, title: String): Uri? {
        if (id > 0) {
            val fileById = File(coversDir, "$id.jpg")
            if (fileById.exists() && fileById.length() > 0) {
                return Uri.fromFile(fileById)
            }
        }

        val cleanKey = sanitize("${artist}_${title}")
        val fileByName = File(coversDir, "$cleanKey.jpg")
        if (fileByName.exists() && fileByName.length() > 0) {
            return Uri.fromFile(fileByName)
        }

        val cleanTitleKey = sanitize(title)
        val fileByTitle = File(coversDir, "$cleanTitleKey.jpg")
        if (fileByTitle.exists() && fileByTitle.length() > 0) {
            return Uri.fromFile(fileByTitle)
        }

        // Fuzzy match: check if any file in covers folder matches artist or title
        try {
            val files = coversDir.listFiles { _, name -> name.endsWith(".jpg", ignoreCase = true) }
            if (files != null) {
                val cleanWords = cleanSearchTerm(title).lowercase().split(" ").filter { it.length > 3 }
                for (f in files) {
                    val fName = f.nameWithoutExtension.lowercase()
                    if (cleanWords.isNotEmpty() && cleanWords.all { fName.contains(it) }) {
                        return Uri.fromFile(f)
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Auto-fetches and permanently saves cover art for any audio file.
     * 1. Checks embedded tags first (saves extracted bytes locally for fast access).
     * 2. If missing, queries iTunes / Apple Music online search API for high-res 600x600 artwork.
     * 3. Downloads and saves to persistent storage.
     */
    suspend fun autoFetchAndSaveCover(media: MediaModel): Uri? = withContext(Dispatchers.IO) {
        val existing = getLocalCoverUri(media.id, media.artist, media.title)
        if (existing != null) return@withContext existing

        // 1. Try extracting embedded picture first
        val embeddedSaved = extractAndSaveEmbeddedArt(media)
        if (embeddedSaved != null) return@withContext embeddedSaved

        // 2. Try fetching from online iTunes Search API
        val onlineSaved = fetchFromOnlineApi(media.title, media.artist, media.id)
        if (onlineSaved != null) return@withContext onlineSaved

        null
    }

    private fun extractAndSaveEmbeddedArt(media: MediaModel): Uri? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, media.uri)
            val picture = retriever.embeddedPicture
            retriever.release()

            if (picture != null && picture.isNotEmpty()) {
                val targetFile = File(coversDir, "${media.id}.jpg")
                FileOutputStream(targetFile).use { it.write(picture) }
                val cleanKey = sanitize("${media.artist}_${media.title}")
                val namedFile = File(coversDir, "$cleanKey.jpg")
                FileOutputStream(namedFile).use { it.write(picture) }
                Uri.fromFile(targetFile)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromOnlineApi(title: String, artist: String, id: Long): Uri? {
        return try {
            val cleanTitle = cleanSearchTerm(title)
            val cleanArtist = if (artist.contains("unknown", ignoreCase = true) || artist.contains("desconocido", ignoreCase = true)) "" else cleanSearchTerm(artist)
            val query = "$cleanTitle $cleanArtist".trim()
            if (query.length < 2) return null

            val urlString = "https://itunes.apple.com/search?term=${URLEncoder.encode(query, "UTF-8")}&entity=song&limit=1"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "AuraPlayer/1.0")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    var artUrl = first.optString("artworkUrl100")
                    if (artUrl.isNotEmpty()) {
                        // Upgrade to 600x600 high quality artwork
                        artUrl = artUrl.replace("100x100bb.jpg", "600x600bb.jpg")
                        val targetFile = File(coversDir, "$id.jpg")
                        downloadImageToFile(artUrl, targetFile)
                        if (targetFile.exists() && targetFile.length() > 0) {
                            val cleanKey = sanitize("${artist}_${title}")
                            val namedFile = File(coversDir, "$cleanKey.jpg")
                            if (!namedFile.exists()) {
                                targetFile.copyTo(namedFile, overwrite = true)
                            }
                            return Uri.fromFile(targetFile)
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadImageToFile(imageUrl: String, targetFile: File) {
        try {
            val conn = URL(imageUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            if (conn.responseCode == 200) {
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanSearchTerm(text: String): String {
        return text.replace(Regex("(?i)\\.(mp3|m4a|wav|flac|opus|ogg)"), "")
            .replace(Regex("(?i)\\(.*?(remix|feat|official|video|audio|lyric|letra|mp3).*?\\)"), "")
            .replace(Regex("(?i)\\[.*?\\]"), "")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
    }

    private fun sanitize(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}
