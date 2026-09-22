package com.auraplayer.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.auraplayer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val fileSizeMb: Double
)

class UpdateManager(private val context: Context) {

    private val repoReleasesApi = "https://api.github.com/repos/davidchaparro780-wq/AuraPlayer/releases/latest"
    private val tag = "UpdateManager"

    /**
     * Checks GitHub Releases for a newer version of DaVE.
     * Returns UpdateInfo if an update is available, or null if already on the latest version.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(repoReleasesApi)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DaVE-App/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (conn.responseCode != 200) {
                Log.d(tag, "GitHub API returned code ${conn.responseCode}")
                return@withContext null
            }

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)

            val rawTagName = root.optString("tag_name", "")
            val remoteVersion = rawTagName.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
            val body = root.optString("body", "Mejoras de rendimiento y nuevas funciones.")

            Log.d(tag, "Remote version: $remoteVersion vs Current: $currentVersion")

            if (isVersionNewer(remoteVersion, currentVersion)) {
                // Find APK asset
                val assets = root.optJSONArray("assets")
                var apkUrl = ""
                var apkSize = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "")
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (apkUrl.isNotBlank()) {
                    return@withContext UpdateInfo(
                        versionName = rawTagName,
                        changelog = body,
                        downloadUrl = apkUrl,
                        fileSizeMb = String.format("%.1f", apkSize / (1024.0 * 1024.0)).toDoubleOrNull() ?: 20.0
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking for update: ${e.message}")
        }
        null
    }

    /**
     * Downloads the APK file showing progress percentage (0-100), then triggers installation.
     */
    suspend fun downloadAndInstall(
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            var currentUrl = updateInfo.downloadUrl
            var conn: HttpURLConnection
            var redirects = 0

            // Follow redirects for GitHub asset CDN
            while (true) {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "DaVE-Updater/1.0")
                }
                val code = conn.responseCode
                if (code in 301..308 && redirects < 5) {
                    val loc = conn.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        currentUrl = loc
                        redirects++
                        continue
                    }
                }
                break
            }

            if (conn.responseCode !in 200..299) {
                withContext(Dispatchers.Main) {
                    onError("El servidor respondió con código ${conn.responseCode}")
                }
                return@withContext
            }

            val totalLength = conn.contentLength
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val targetFile = File(downloadDir, "DaVE-update-${updateInfo.versionName}.apk")

            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        if (totalLength > 0) {
                            val progress = ((totalDownloaded * 100) / totalLength).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Launch package installer on Main thread
            withContext(Dispatchers.Main) {
                installApk(targetFile)
            }

        } catch (e: Exception) {
            Log.e(tag, "Download failed: ${e.message}")
            withContext(Dispatchers.Main) {
                onError("Error al descargar actualización: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Triggers the Android package installer via FileProvider.
     */
    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(tag, "APK file does not exist at ${apkFile.absolutePath}")
                return
            }

            // Android 8.0+ unknown sources permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(tag, "Error triggering install: ${e.message}")
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
