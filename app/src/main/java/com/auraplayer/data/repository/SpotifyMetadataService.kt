package com.auraplayer.data.repository

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * SpotifyMetadataService — Universal HD Metadata & Artwork Engine.
 *
 * 1. Primary: Spotify Web API (Client Credentials OAuth) when configured
 * 2. Automatic Fallback: Deezer Public API (1000x1000 px cover_xl, zero API key required)
 * 3. Secondary Fallback: iTunes Search API (1000x1000 px HD artwork, zero API key required)
 *
 * Provides gorgeous, high-resolution album artwork and canonical album/artist metadata
 * without requiring user configuration or failing silently.
 */
class SpotifyMetadataService(
    private val clientId: String = "TU_CLIENT_ID_AQUI",
    private val clientSecret: String = "TU_CLIENT_SECRET_AQUI"
) {

    private val tag = "SpotifyMeta"

    // Cached Spotify token + expiry timestamp
    @Volatile private var accessToken: String? = null
    @Volatile private var tokenExpiresAt: Long = 0L

    data class SpotifyMeta(
        val coverUrl: String,      // 640x640 to 1000x1000 HD artwork
        val albumName: String,
        val artistName: String,
        val spotifyTrackId: String
    )

    /**
     * Main entry point. First attempts Spotify, then falls back to Deezer HD and iTunes HD.
     */
    fun fetchMeta(title: String, artist: String): SpotifyMeta? {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank()) return null

        // 1. Try Spotify if credentials are configured
        if (clientId != "TU_CLIENT_ID_AQUI" && clientSecret != "TU_CLIENT_SECRET_AQUI") {
            try {
                val token = getOrRefreshToken()
                if (token != null) {
                    val meta = searchSpotify(token, cleanTitle, cleanArtist)
                    if (meta != null && meta.coverUrl.isNotBlank()) {
                        return meta
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Spotify fetch error: ${e.message}")
            }
        }

        // 2. High-Res Fallback: Deezer Public API (1000x1000 HD cover_xl, no API key required)
        try {
            val deezerMeta = searchDeezer(cleanTitle, cleanArtist)
            if (deezerMeta != null && deezerMeta.coverUrl.isNotBlank()) {
                return deezerMeta
            }
        } catch (e: Exception) {
            Log.e(tag, "Deezer HD fallback error: ${e.message}")
        }

        // 3. High-Res Fallback: iTunes Search API (1000x1000 HD cover, no API key required)
        try {
            val itunesMeta = searchItunes(cleanTitle, cleanArtist)
            if (itunesMeta != null && itunesMeta.coverUrl.isNotBlank()) {
                return itunesMeta
            }
        } catch (e: Exception) {
            Log.e(tag, "iTunes HD fallback error: ${e.message}")
        }

        return null
    }

    /** Client Credentials token request for Spotify — auto-refreshes when expired. */
    private fun getOrRefreshToken(): String? {
        val now = System.currentTimeMillis()
        if (accessToken != null && now < tokenExpiresAt) return accessToken

        return try {
            val credentials = "$clientId:$clientSecret"
            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            val body = "grant_type=client_credentials"

            val conn = (URL("https://accounts.spotify.com/api/token").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Authorization", "Basic $encoded")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val token = json.getString("access_token")
                val expiresIn = json.optInt("expires_in", 3600)
                accessToken = token
                tokenExpiresAt = now + (expiresIn - 60) * 1000L
                token
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun searchSpotify(token: String, title: String, artist: String): SpotifyMeta? {
        return try {
            val query = if (artist.isNotBlank() && artist != "Artista" && artist != "Artista Libre") {
                "track:$title artist:$artist"
            } else {
                title
            }
            val encoded = URLEncoder.encode(query, "UTF-8")
            val urlStr = "https://api.spotify.com/v1/search?q=$encoded&type=track&limit=1&market=US"

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("User-Agent", "AuraPlayer/1.8.1 (Android)")
            }

            if (conn.responseCode == 200) {
                val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val items = root.optJSONObject("tracks")?.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val track = items.getJSONObject(0)
                    val trackId = track.optString("id", "")
                    val album = track.optJSONObject("album")
                    val albumName = album?.optString("name", "") ?: ""
                    val artistName = track.optJSONArray("artists")?.optJSONObject(0)?.optString("name", artist) ?: artist
                    val images = album?.optJSONArray("images")
                    val coverUrl = images?.optJSONObject(0)?.optString("url", "") ?: ""

                    if (coverUrl.isNotBlank()) {
                        SpotifyMeta(
                            coverUrl = coverUrl,
                            albumName = albumName,
                            artistName = artistName,
                            spotifyTrackId = trackId
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deezer Public Search API (zero API keys needed, returns up to 1000x1000 cover_xl).
     */
    private fun searchDeezer(title: String, artist: String): SpotifyMeta? {
        return try {
            val query = if (artist.isNotBlank() && artist != "Artista" && artist != "Artista Libre") {
                "$artist $title"
            } else {
                title
            }
            val encoded = URLEncoder.encode(query, "UTF-8")
            val urlStr = "https://api.deezer.com/search?q=$encoded&limit=1"

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 AuraPlayer/1.8.1")
            }

            if (conn.responseCode == 200) {
                val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val data = root.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val item = data.getJSONObject(0)
                    val trackTitle = item.optString("title", title)
                    val artistObj = item.optJSONObject("artist")
                    val artistName = artistObj?.optString("name", artist) ?: artist
                    val albumObj = item.optJSONObject("album")
                    val albumName = albumObj?.optString("title", "") ?: ""
                    val coverUrl = albumObj?.optString("cover_xl", "")?.ifBlank {
                        albumObj.optString("cover_big", "")
                    } ?: ""

                    if (coverUrl.isNotBlank()) {
                        SpotifyMeta(
                            coverUrl = coverUrl,
                            albumName = albumName,
                            artistName = artistName,
                            spotifyTrackId = "deezer_${item.optLong("id")}"
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * iTunes Search API (zero API keys needed, replaces 100x100 with 1000x1000 HD).
     */
    private fun searchItunes(title: String, artist: String): SpotifyMeta? {
        return try {
            val query = if (artist.isNotBlank() && artist != "Artista" && artist != "Artista Libre") {
                "$artist $title"
            } else {
                title
            }
            val encoded = URLEncoder.encode(query, "UTF-8")
            val urlStr = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=1"

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 AuraPlayer/1.8.1")
            }

            if (conn.responseCode == 200) {
                val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val results = root.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val item = results.getJSONObject(0)
                    val artistName = item.optString("artistName", artist)
                    val albumName = item.optString("collectionName", "")
                    var cover = item.optString("artworkUrl100", "")
                    if (cover.isNotBlank()) {
                        cover = cover.replace("100x100bb.jpg", "1000x1000bb.jpg")
                            .replace("100x100bb", "1000x1000bb")
                        SpotifyMeta(
                            coverUrl = cover,
                            albumName = albumName,
                            artistName = artistName,
                            spotifyTrackId = "itunes_${item.optLong("trackId")}"
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
