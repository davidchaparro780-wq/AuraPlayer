package com.auraplayer.data.repository

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class VaultItem(
    val id: String,
    val file: File,
    val name: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val dateAdded: Long
)

class VaultManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("dave_vault_prefs", Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences("aura_vault_prefs", Context.MODE_PRIVATE)
    private val pinKey = "vault_pin_hash"
    private val hiddenPathsKey = "hidden_media_paths"

    // Primary internal vault directory
    private val vaultDir: File by lazy {
        File(context.filesDir, ".secure_vault").apply {
            if (!exists()) mkdirs()
            ensureNoMedia(this)
        }
    }

    private val videoVaultDir: File by lazy {
        File(vaultDir, "videos").apply {
            if (!exists()) mkdirs()
            ensureNoMedia(this)
        }
    }

    private val photoVaultDir: File by lazy {
        File(vaultDir, "photos").apply {
            if (!exists()) mkdirs()
            ensureNoMedia(this)
        }
    }

    // Persistent external vault directory (Survives app updates, data clears & reinstalls)
    private val persistentVaultDir: File by lazy {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        File(docsDir, ".dave_vault").apply {
            try {
                if (!exists()) mkdirs()
                ensureNoMedia(this)
            } catch (_: Exception) {}
        }
    }

    private val persistentVideoVaultDir: File by lazy {
        File(persistentVaultDir, "videos").apply {
            try {
                if (!exists()) mkdirs()
                ensureNoMedia(this)
            } catch (_: Exception) {}
        }
    }

    private val persistentPhotoVaultDir: File by lazy {
        File(persistentVaultDir, "photos").apply {
            try {
                if (!exists()) mkdirs()
                ensureNoMedia(this)
            } catch (_: Exception) {}
        }
    }

    // App external vault directory
    private val externalAppVaultDir: File by lazy {
        File(context.getExternalFilesDir(null), ".secure_vault").apply {
            try {
                if (!exists()) mkdirs()
                ensureNoMedia(this)
            } catch (_: Exception) {}
        }
    }

    init {
        migrateLegacyPrefs()
        ensureNoMedia(vaultDir)
        try { ensureNoMedia(persistentVaultDir) } catch (_: Exception) {}
    }

    private fun migrateLegacyPrefs() {
        try {
            if (!prefs.contains(pinKey) && legacyPrefs.contains(pinKey)) {
                val legacyPin = legacyPrefs.getString(pinKey, null)
                if (!legacyPin.isNullOrBlank()) {
                    prefs.edit().putString(pinKey, legacyPin).apply()
                }
            }
            if (!prefs.contains(hiddenPathsKey) && legacyPrefs.contains(hiddenPathsKey)) {
                val legacyPaths = legacyPrefs.getStringSet(hiddenPathsKey, null)
                if (legacyPaths != null) {
                    prefs.edit().putStringSet(hiddenPathsKey, legacyPaths).apply()
                }
            }
        } catch (_: Exception) {}
    }

    private fun ensureNoMedia(dir: File) {
        try {
            if (!dir.exists()) dir.mkdirs()
            val noMedia = File(dir, ".nomedia")
            if (!noMedia.exists()) noMedia.createNewFile()
        } catch (_: Exception) {}
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun openAllFilesAccessSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        }
    }

    fun isPinSet(): Boolean {
        return prefs.contains(pinKey) && !prefs.getString(pinKey, null).isNullOrBlank()
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(pinKey, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(pinKey, null) ?: return false
        return stored == hashPin(pin)
    }

    fun resetPin() {
        prefs.edit().remove(pinKey).apply()
    }

    fun markPathAsHidden(path: String?) {
        if (path.isNullOrBlank()) return
        val current = prefs.getStringSet(hiddenPathsKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(path)
        val name = try { File(path).name } catch (_: Exception) { "" }
        if (name.isNotBlank()) current.add(name)
        prefs.edit().putStringSet(hiddenPathsKey, current).apply()
    }

    fun unmarkPathAsHidden(path: String?) {
        if (path.isNullOrBlank()) return
        val current = prefs.getStringSet(hiddenPathsKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        val name = try { File(path).name } catch (_: Exception) { "" }
        current.removeAll { it == path || (name.isNotBlank() && (it == name || it.endsWith(name))) }
        prefs.edit().putStringSet(hiddenPathsKey, current).apply()
    }

    fun isPathHidden(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        if (path.contains(".secure_vault")) return true
        val set = prefs.getStringSet(hiddenPathsKey, emptySet()) ?: emptySet()
        if (set.contains(path)) return true
        val name = try { File(path).name } catch (_: Exception) { "" }
        if (name.isNotBlank() && set.contains(name)) return true
        return if (name.isNotBlank()) set.any { it.endsWith(name) } else false
    }

    fun resolveRealPathFromUri(uri: Uri?): String? {
        if (uri == null) return null
        try {
            if (uri.scheme == "file") return uri.path
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (idx != -1) {
                        val path = cursor.getString(idx)
                        if (!path.isNullOrBlank()) return path
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun getVaultItems(): List<VaultItem> = withContext(Dispatchers.IO) {
        val itemsMap = mutableMapOf<String, VaultItem>()

        val candidateDirs = listOf(
            videoVaultDir to true,
            photoVaultDir to false,
            persistentVideoVaultDir to true,
            persistentPhotoVaultDir to false,
            File(externalAppVaultDir, "videos") to true,
            File(externalAppVaultDir, "photos") to false,
            File(Environment.getExternalStorageDirectory(), ".dave_vault/videos") to true,
            File(Environment.getExternalStorageDirectory(), ".dave_vault/photos") to false,
            File(context.filesDir, "vault/videos") to true,
            File(context.filesDir, "vault/photos") to false
        )

        for ((dir, isVideo) in candidateDirs) {
            try {
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.isFile && it.name != ".nomedia" && it.length() > 0 }?.forEach { f ->
                        if (!itemsMap.containsKey(f.name)) {
                            itemsMap[f.name] = VaultItem(
                                id = f.name,
                                file = f,
                                name = f.nameWithoutExtension,
                                isVideo = isVideo,
                                sizeBytes = f.length(),
                                dateAdded = f.lastModified()
                            )
                            // Auto-mirror: If missing from internal, copy to internal. If missing from persistent, copy to persistent.
                            val internalTarget = File(if (isVideo) videoVaultDir else photoVaultDir, f.name)
                            if (!internalTarget.exists() && f.absolutePath != internalTarget.absolutePath) {
                                try {
                                    FileInputStream(f).use { input ->
                                        FileOutputStream(internalTarget).use { output -> input.copyTo(output) }
                                    }
                                } catch (_: Exception) {}
                            }
                            val persistentTarget = File(if (isVideo) persistentVideoVaultDir else persistentPhotoVaultDir, f.name)
                            if (!persistentTarget.exists() && f.absolutePath != persistentTarget.absolutePath) {
                                try {
                                    FileInputStream(f).use { input ->
                                        FileOutputStream(persistentTarget).use { output -> input.copyTo(output) }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Also check if any known hidden path still exists on disk
        val hiddenPaths = prefs.getStringSet(hiddenPathsKey, emptySet()) ?: emptySet()
        for (hp in hiddenPaths) {
            try {
                val f = File(hp)
                if (f.exists() && f.isFile && f.length() > 0 && !itemsMap.containsKey(f.name)) {
                    val isVid = hp.endsWith(".mp4", true) || hp.endsWith(".mkv", true) || hp.endsWith(".webm", true)
                    itemsMap[f.name] = VaultItem(
                        id = f.name,
                        file = f,
                        name = f.nameWithoutExtension,
                        isVideo = isVid,
                        sizeBytes = f.length(),
                        dateAdded = f.lastModified()
                    )
                }
            } catch (_: Exception) {}
        }

        itemsMap.values.sortedByDescending { it.dateAdded }
    }

    suspend fun copyMediaToVault(
        sourcePath: String? = null,
        sourceUri: Uri? = null,
        isVideo: Boolean,
        customName: String? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            val targetDir = if (isVideo) videoVaultDir else photoVaultDir
            val persistentDir = if (isVideo) persistentVideoVaultDir else persistentPhotoVaultDir

            val ext = if (sourcePath != null && sourcePath.contains(".")) {
                "." + sourcePath.substringAfterLast(".")
            } else if (isVideo) ".mp4" else ".jpg"

            val baseName = customName ?: "hidden_${System.currentTimeMillis()}"
            val targetFile = File(targetDir, "${baseName}${ext}")
            val persistentFile = File(persistentDir, "${baseName}${ext}")

            var realSourcePath = sourcePath
            if (realSourcePath.isNullOrBlank() && sourceUri != null) {
                realSourcePath = resolveRealPathFromUri(sourceUri)
            }

            if (realSourcePath != null && File(realSourcePath).exists()) {
                FileInputStream(File(realSourcePath)).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else if (sourceUri != null) {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                return@withContext null
            }

            // Dual persistence: create mirrored replica in external Documents/.dave_vault
            if (targetFile.exists() && targetFile.length() > 0) {
                try {
                    FileInputStream(targetFile).use { input ->
                        FileOutputStream(persistentFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (_: Exception) {}

                markPathAsHidden(realSourcePath ?: sourcePath)
                markPathAsHidden(targetFile.name)
                targetFile
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun cleanVaultFile(file: File?) {
        try {
            file?.delete()
        } catch (_: Exception) {}
    }

    fun resolveCanonicalMediaStoreUri(
        ctx: Context,
        uri: Uri?,
        filePath: String?,
        isVideo: Boolean
    ): Uri? {
        val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        // 1. Direct external media URI
        if (uri != null) {
            val uriStr = uri.toString()
            if (uriStr.startsWith("content://media/external/video/media/") ||
                uriStr.startsWith("content://media/external/images/media/")) {
                return uri
            }
            // 2. DocumentsContract URI (e.g., com.android.providers.media.documents/document/video:1234)
            try {
                if (DocumentsContract.isDocumentUri(ctx, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val idPart = if (docId.contains(":")) docId.split(":")[1] else docId
                    val idLong = idPart.toLongOrNull()
                    if (idLong != null) {
                        return ContentUris.withAppendedId(baseUri, idLong)
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Query MediaStore by file path
        if (!filePath.isNullOrBlank()) {
            try {
                ctx.contentResolver.query(
                    baseUri,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DATA} = ?",
                    arrayOf(filePath),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        return ContentUris.withAppendedId(baseUri, id)
                    }
                }
            } catch (_: Exception) {}
        }

        return uri
    }

    fun deleteOriginalMedia(
        ctx: Context,
        filePath: String?,
        uri: Uri?,
        isVideo: Boolean
    ): Boolean {
        var physicallyDeleted = false

        // 1. If we have the absolute path, attempt physical file deletion
        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                try {
                    physicallyDeleted = file.delete()
                } catch (_: Exception) {}
            } else {
                physicallyDeleted = true
            }
        }

        // 2. Resolve canonical URI and attempt MediaStore deletion
        val canonicalUri = resolveCanonicalMediaStoreUri(ctx, uri, filePath, isVideo)
        if (canonicalUri != null) {
            try {
                val count = ctx.contentResolver.delete(canonicalUri, null, null)
                if (count > 0) physicallyDeleted = true
            } catch (_: Exception) {}
        }

        // 3. Direct path delete in MediaStore
        if (!filePath.isNullOrBlank()) {
            try {
                val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ctx.contentResolver.delete(baseUri, "${MediaStore.MediaColumns.DATA} = ?", arrayOf(filePath))
            } catch (_: Exception) {}

            // 4. Force MediaScanner to update gallery
            try {
                MediaScannerConnection.scanFile(ctx, arrayOf(filePath), null, null)
            } catch (_: Exception) {}
        }

        // Check if file is actually gone
        if (!filePath.isNullOrBlank()) {
            return !File(filePath).exists()
        }

        return physicallyDeleted
    }

    fun getDeleteRequestPendingIntent(
        ctx: Context,
        uri: Uri?,
        filePath: String?,
        isVideo: Boolean
    ): PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val canonicalUri = resolveCanonicalMediaStoreUri(ctx, uri, filePath, isVideo)
            if (canonicalUri != null) {
                return try {
                    MediaStore.createDeleteRequest(ctx.contentResolver, listOf(canonicalUri))
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
        return null
    }

    suspend fun hideMediaFromUri(uri: Uri, isVideo: Boolean, customName: String? = null): Boolean = withContext(Dispatchers.IO) {
        val vaultFile = copyMediaToVault(sourceUri = uri, isVideo = isVideo, customName = customName)
            ?: return@withContext false

        val deleted = deleteOriginalMedia(context, filePath = null, uri = uri, isVideo = isVideo)
        if (!deleted && !hasAllFilesAccess()) {
            // Direct delete didn't work without permissions; return false so UI prompts delete intent
            return@withContext false
        }
        deleted
    }

    suspend fun hideMediaFile(sourcePath: String, isVideo: Boolean, sourceUri: Uri? = null): Boolean = withContext(Dispatchers.IO) {
        val vaultFile = copyMediaToVault(sourcePath = sourcePath, sourceUri = sourceUri, isVideo = isVideo)
            ?: return@withContext false

        val deleted = deleteOriginalMedia(context, filePath = sourcePath, uri = sourceUri, isVideo = isVideo)
        deleted
    }

    suspend fun restoreMedia(item: VaultItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val publicDir = if (item.isVideo) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            }
            if (!publicDir.exists()) publicDir.mkdirs()

            val destFile = File(publicDir, item.file.name)
            FileInputStream(item.file).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (destFile.exists()) {
                item.file.delete()
                val persistentCopy = if (item.isVideo) File(persistentVideoVaultDir, item.file.name) else File(persistentPhotoVaultDir, item.file.name)
                try { persistentCopy.delete() } catch (_: Exception) {}
                val externalCopy = File(File(externalAppVaultDir, if (item.isVideo) "videos" else "photos"), item.file.name)
                try { externalCopy.delete() } catch (_: Exception) {}

                unmarkPathAsHidden(destFile.absolutePath)
                unmarkPathAsHidden(item.file.name)
                // Force MediaScanner to index restored file so it shows in phone Gallery
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deletePermanently(item: VaultItem): Boolean = withContext(Dispatchers.IO) {
        try {
            unmarkPathAsHidden(item.file.name)
            item.file.delete()
            val persistentCopy = if (item.isVideo) File(persistentVideoVaultDir, item.file.name) else File(persistentPhotoVaultDir, item.file.name)
            try { persistentCopy.delete() } catch (_: Exception) {}
            val externalCopy = File(File(externalAppVaultDir, if (item.isVideo) "videos" else "photos"), item.file.name)
            try { externalCopy.delete() } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
