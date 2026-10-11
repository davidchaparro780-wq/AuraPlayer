package com.auraplayer.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.auraplayer.util.AppLog
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
    val fileSizeMb: Double,
    val versionCode: Long = 0L
)

class UpdateManager(private val context: Context) {

    private val rawVersionJson = "https://raw.githubusercontent.com/davidchaparro780-wq/D4ve/main/version.json"
    private val rawVersionJsonFallback = "https://raw.githubusercontent.com/davidchaparro780-wq/AuraPlayer/main/version.json"
    private val repoReleasesApi = "https://api.github.com/repos/davidchaparro780-wq/D4ve/releases/latest"
    private val tag = "UpdateManager"

    val currentVersionCode: Long
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            86L
        }

    val currentVersionName: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "4.0.0"
        } catch (_: Exception) {
            "4.0.0"
        }

    /**
     * Comprueba si hay una nueva versión disponible.
     * Consulta version.json (en D4ve o AuraPlayer) y como fallback la API de GitHub Releases.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val currentVersion = currentVersionName.removePrefix("v").trim()

        // 1. Consulta directa a version.json
        val jsonUrls = listOf(rawVersionJson, rawVersionJsonFallback)
        for (jsonUrl in jsonUrls) {
            try {
                val url = URL(jsonUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "DaVE-App/$currentVersionName")
                }
                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(jsonStr)
                    val remoteVersion = root.optString("versionName", "").removePrefix("v").trim()
                    val remoteVersionCode = root.optLong("versionCode", 0L)
                    val changelog = root.optString("changelog", "Nueva versión de DaVE disponible con mejoras y optimizaciones.")
                    val downloadUrl = root.optString("downloadUrl", "")
                    val fileSizeMb = root.optDouble("fileSizeMb", 24.5)

                    AppLog.d(tag, "version.json: Remota $remoteVersion (cód. $remoteVersionCode) vs Actual $currentVersion (cód. $currentVersionCode)")

                    val isNewer = (remoteVersionCode > 0 && remoteVersionCode > currentVersionCode) || isVersionNewer(remoteVersion, currentVersion)
                    if (isNewer && downloadUrl.isNotBlank()) {
                        val info = UpdateInfo(
                            versionName = "v$remoteVersion",
                            changelog = changelog,
                            downloadUrl = downloadUrl,
                            fileSizeMb = fileSizeMb,
                            versionCode = remoteVersionCode
                        )
                        com.auraplayer.notification.UpdateNotificationHelper.showUpdateNotification(context, info)
                        return@withContext info
                    }
                }
            } catch (e: Exception) {
                AppLog.w(tag, "Fallo al comprobar $jsonUrl: ${e.message}")
            }
        }

        // 2. Fallback a GitHub Releases API
        try {
            val url = URL(repoReleasesApi)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DaVE-App/$currentVersionName")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val rawTagName = root.optString("tag_name", "")
                val remoteVersion = rawTagName.removePrefix("v").trim()
                val body = root.optString("body", "Mejoras de rendimiento y nuevas funciones.")

                if (isVersionNewer(remoteVersion, currentVersion)) {
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
                        val info = UpdateInfo(
                            versionName = rawTagName,
                            changelog = body,
                            downloadUrl = apkUrl,
                            fileSizeMb = String.format("%.1f", apkSize / (1024.0 * 1024.0)).toDoubleOrNull() ?: 24.5
                        )
                        com.auraplayer.notification.UpdateNotificationHelper.showUpdateNotification(context, info)
                        return@withContext info
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.e(tag, "Error al comprobar en GitHub Releases: ${e.message}", e)
        }

        null
    }

    /**
     * Retorna la ruta al archivo APK descargado para la versión dada.
     */
    fun getDownloadedApkFile(updateInfo: UpdateInfo): File {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        return File(downloadDir, "DaVE-update-${updateInfo.versionName}.apk")
    }

    /**
     * Valida si el archivo APK descargado es íntegro, ejecutable y más nuevo que la app actual.
     */
    fun isApkValidAndNewer(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() < 10_000_000L) return false
        return try {
            val pInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (pInfo != null) {
                val apkVerCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toLong()
                }
                apkVerCode > currentVersionCode
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Descarga atómicamente el APK (usando un .tmp temporal) y lanza la instalación al finalizar.
     */
    suspend fun downloadAndInstall(
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val targetFile = getDownloadedApkFile(updateInfo)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

        try {
            // Si ya existe un APK válido y más nuevo que el actual, instalar directamente
            if (isApkValidAndNewer(targetFile)) {
                targetFile.setReadable(true, false)
                withContext(Dispatchers.Main) {
                    onProgress(100)
                    installApk(targetFile)
                    onSuccess()
                }
                return@withContext
            } else if (targetFile.exists()) {
                // El archivo existente es viejo o corrupto: eliminar para re-descargar
                targetFile.delete()
            }

            if (tempFile.exists()) {
                tempFile.delete()
            }

            var currentUrl = updateInfo.downloadUrl
            var conn: HttpURLConnection
            var redirects = 0

            // Seguir redirecciones de CDN (GitHub Releases -> AWS S3)
            while (true) {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 25000
                    readTimeout = 45000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) DaVE-Updater")
                    setRequestProperty("Accept-Encoding", "identity") // Evita compresión para Content-Length exacto
                }
                val code = conn.responseCode
                if (code in 301..308 && redirects < 8) {
                    val loc = conn.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        conn.disconnect()
                        currentUrl = loc
                        redirects++
                        continue
                    }
                }
                break
            }

            if (conn.responseCode !in 200..299) {
                withContext(Dispatchers.Main) {
                    onError("El servidor respondió con error ${conn.responseCode}. Intenta descargar desde el navegador.")
                }
                return@withContext
            }

            val totalLength = conn.contentLength.let {
                if (it > 0) it.toLong() else (updateInfo.fileSizeMb * 1024 * 1024).toLong()
            }

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        if (totalLength > 0L) {
                            val progress = ((totalDownloaded * 100) / totalLength).toInt().coerceIn(1, 99)
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Validar que el archivo descargado sea un APK Android íntegro
            tempFile.setReadable(true, false)
            val parsedInfo = context.packageManager.getPackageArchiveInfo(tempFile.absolutePath, 0)
            if (parsedInfo == null || tempFile.length() < 5_000_000L) {
                tempFile.delete()
                withContext(Dispatchers.Main) {
                    onError("La descarga quedó incompleta o corrupta. Por favor intenta descargar directamente desde el navegador.")
                }
                return@withContext
            }

            // Renombrar atómicamente a targetFile
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)
            targetFile.setReadable(true, false)

            // Abrir instalador de Android en hilo principal
            withContext(Dispatchers.Main) {
                onProgress(100)
                installApk(targetFile)
                onSuccess()
            }

        } catch (e: Exception) {
            AppLog.e(tag, "Error al descargar la actualización: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            withContext(Dispatchers.Main) {
                onError("Error de conexión al descargar: ${e.localizedMessage}. Usa el botón 'Navegador' para descargarla directamente.")
            }
        }
    }

    /**
     * Lanza el instalador oficial de Android mediante FileProvider.
     */
    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists() || apkFile.length() < 5_000_000L) {
                Toast.makeText(context, "El archivo de actualización no es válido", Toast.LENGTH_SHORT).show()
                return
            }

            apkFile.setReadable(true, false)

            // Verificación de permiso en Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Por favor activa 'Permitir desde esta fuente' para instalar la actualización de DaVE",
                        Toast.LENGTH_LONG
                    ).show()
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return
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

            // Conceder permisos explícitos al instalador de paquetes
            val resInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    installIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(
                    installIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            }

            for (resolveInfo in resInfoList) {
                val pkgName = resolveInfo.activityInfo.packageName
                try {
                    context.grantUriPermission(pkgName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    AppLog.d(tag, "No se pudo otorgar permiso de URI a $pkgName", e)
                }
            }

            context.startActivity(installIntent)

        } catch (e: Exception) {
            AppLog.e(tag, "Error al abrir el instalador: ${e.message}", e)
            Toast.makeText(
                context,
                "No se pudo abrir el instalador del sistema: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()

            // Fallback directo: abrir navegador si falla el instalador nativo
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/davidchaparro780-wq/D4ve/releases/latest"))
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val cleanRemote = remote.removePrefix("v").trim()
        val cleanCurrent = current.removePrefix("v").trim()
        val remoteParts = cleanRemote.split(".").mapNotNull { it.replace(Regex("[^0-9]"), "").toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.replace(Regex("[^0-9]"), "").toIntOrNull() }

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
