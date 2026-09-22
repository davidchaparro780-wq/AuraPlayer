package com.auraplayer.data.repository

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * SpotifyMetadataService — Client Credentials flow (no user login needed).
 *
 * Only uses the Spotify Web API to retrieve:
 *   - HD cover art (640x640 px)
 *   - Canonical album name
 *   - Official artist name
 *
 * No audio download, no DRM bypass. 100% within Spotify Developer Terms of Service.
 *
 * Setup:
 *   1. Go to https://developer.spotify.com/dashboard → Create App
 *   2. Copy your Client ID and Client Secret below.
 */
class SpotifyMetadataService(
    private val clientId: String = "TU_CLIENT_ID_AQUI",
    private val clientSecret: String = "TU_CLIENT_SECRET_AQUI"
) {

    private val tag = "SpotifyMeta"

    // Cached token + expiry timestamp
    @Volatile private var accessToken: String? = null
    @Volatile private var tokenExpiresAt: Long = 0L

    data class SpotifyMeta(
        val coverUrl: String,      // 640x640 JPEG from Spotify CDN
        val albumName: String,
        val artistName: String,
        val spotifyTrackId: String
    )

    /**
     * Main entry point. Returns null if credentials are not configured or network fails.
     */
    fun fetchMeta(title: String, artist: String): SpotifyMeta? {
        if (clientId == "TU_CLIENT_ID_AQUI" || clientSecret == "TU_CLIENT_SECRET_AQUI") {
            Log.w(tag, "Spotify credentials not configured. Skipping metadata enrichment.")
            return null
        }
        return try {
            val token = getOrRefreshToken() ?: return null
            searchTrack(token, title, artist)
        } catch (e: Exception) {
            Log.e(tag, "Spotify fetchMeta error: ${e.message}")
            null
        }
    }

    /** Client Credentials token request — auto-refreshes when expired. */
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
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Authorization", "Basic $encoded")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val token = json.getString("access_token")
                val expiresIn = json.optInt("expires_in", 3600)
                accessToken = token
                tokenExpiresAt = now + (expiresIn - 60) * 1000L  // refresh 60s early
                Log.d(tag, "Spotify token obtained, expires in ${expiresIn}s")
                token
            } else {
                Log.e(tag, "Spotify token error: HTTP ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Spotify token request failed: ${e.message}")
            null
        }
    }

    /** Searches Spotify for the best matching track and returns enriched metadata. */
    private fun searchTrack(token: String, title: String, artist: String): SpotifyMeta? {
        return try {
            // Build query: "track:TITLE artist:ARTIST" for best match
            val query = buildSearchQuery(title, artist)
            val encoded = URLEncoder.encode(query, "UTF-8")
            val urlStr = "https://api.spotify.com/v1/search?q=$encoded&type=track&limit=1&market=US"

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("User-Agent", "AuraPlayer/1.8.0 (Android)")
            }

            if (conn.responseCode == 200) {
                val root = JSONObject(conn.inputStream.bufferedReader().readText())
                val items = root.optJSONObject("tracks")?.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val track = items.getJSONObject(0)
                    val trackId = track.optString("id", "")
                    val album = track.optJSONObject("album")
                    val albumName = album?.optString("name", "") ?: ""
                    val artistName = track.optJSONArray("artists")
                        ?.optJSONObject(0)?.optString("name", artist) ?: artist

                    // Pick highest resolution image (first = largest, usually 640x640)
                    val images = album?.optJSONArray("images")
                    val coverUrl = images?.optJSONObject(0)?.optString("url", "") ?: ""

                    if (coverUrl.isNotBlank()) {
                        Log.d(tag, "Spotify enriched: $title → cover=${coverUrl.take(60)}")
                        SpotifyMeta(
                            coverUrl = coverUrl,
                            albumName = albumName,
                            artistName = artistName,
                            spotifyTrackId = trackId
                        )
                    } else null
                } else {
                    Log.d(tag, "Spotify: no results for '$query'")
                    null
                }
            } else if (conn.responseCode == 401) {
                // Token expired unexpectedly — invalidate for next call
                accessToken = null
                tokenExpiresAt = 0L
                Log.w(tag, "Spotify 401 — token invalidated for retry")
                null
            } else {
                Log.e(tag, "Spotify search HTTP ${conn.responseCode} for '$query'")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Spotify searchTrack error: ${e.message}")
            null
        }
    }

    /**
     * Builds the best search query for Spotify:
     *   - If we have both title and artist → "track:TITLE artist:ARTIST"
     *   - If only title → just the title string
     */
    private fun buildSearchQuery(title: String, artist: String): String {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        return if (cleanArtist.isNotBlank() && cleanArtist != "Artista" && cleanArtist != "Artista Libre") {
            "track:$cleanTitle artist:$cleanArtist"
        } else {
            cleanTitle
        }
    }
}
