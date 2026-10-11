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
import android.provider.OpenableColumns
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

            // 1. Direct DATA column lookup
            try {
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (idx != -1) {
                            val path = cursor.getString(idx)
                            if (!path.isNullOrBlank() && File(path).exists()) return path
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. DocumentsContract parsing (e.g., com.android.providers.media.documents)
            try {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    if (docId.startsWith("raw:")) {
                        val rawPath = docId.removePrefix("raw:")
                        if (File(rawPath).exists()) return rawPath
                    }
                    val idPart = if (docId.contains(":")) docId.split(":")[1] else docId
                    val idLong = idPart.toLongOrNull()
                    if (idLong != null) {
                        val isVideo = docId.startsWith("video")
                        val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        context.contentResolver.query(
                            baseUri,
                            arrayOf(MediaStore.MediaColumns.DATA),
                            "${MediaStore.MediaColumns._ID} = ?",
                            arrayOf(idLong.toString()),
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                                if (idx != -1) {
                                    val path = cursor.getString(idx)
                                    if (!path.isNullOrBlank() && File(path).exists()) return path
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // 3. Fallback for PhotoPicker: query MediaStore by Display Name and Size
            try {
                var displayName: String? = null
                var fileSize: Long = -1L
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                    }
                }

                if (!displayName.isNullOrBlank()) {
                    for (baseUri in listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
                        val selection = if (fileSize > 0L) {
                            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?"
                        } else {
                            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                        }
                        val args = if (fileSize > 0L) arrayOf(displayName!!, fileSize.toString()) else arrayOf(displayName!!)
                        context.contentResolver.query(
                            baseUri,
                            arrayOf(MediaStore.MediaColumns.DATA),
                            selection,
                            args,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                                if (idx != -1) {
                                    val path = cursor.getString(idx)
                                    if (!path.isNullOrBlank() && File(path).exists()) return path
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        return null
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun resolveFileNameFromUri(uri: Uri?): String? {
        if (uri == null) return null
        try {
            if (uri.scheme == "file") {
                return File(uri.path ?: "").name
            }
            val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Exception) {}
        return uri.lastPathSegment?.substringAfterLast("/")
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
            File(Environment.getExternalStorageDirectory(), ".dave_vault/photos") to false
        )

        for ((dir, isVideo) in candidateDirs) {
            try {
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.isFile && it.name != ".nomedia" && it.length() > 0 }?.forEach { f ->
                        val dedupeKey = "${if (isVideo) "v_" else "p_"}${f.name.lowercase().trim()}"
                        if (!itemsMap.containsKey(dedupeKey)) {
                            itemsMap[dedupeKey] = VaultItem(
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

            // Determine original file name and extension
            var originalFileName: String? = null
            if (!sourcePath.isNullOrBlank()) {
                originalFileName = File(sourcePath).name
            } else if (sourceUri != null) {
                originalFileName = resolveFileNameFromUri(sourceUri)
            }

            val ext = if (originalFileName != null && originalFileName.contains(".")) {
                "." + originalFileName.substringAfterLast(".")
            } else if (sourcePath != null && sourcePath.contains(".")) {
                "." + sourcePath.substringAfterLast(".")
            } else if (isVideo) ".mp4" else ".jpg"

            val rawBaseName = customName
                ?: (originalFileName?.substringBeforeLast(".")?.takeIf { it.isNotBlank() })
                ?: "hidden_${System.currentTimeMillis()}"

            // Clean characters for safe filename
            val safeBaseName = rawBaseName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

            val targetFile = File(targetDir, "${safeBaseName}${ext}")
            val persistentFile = File(persistentDir, "${safeBaseName}${ext}")

            // If this exact file already exists in vault and has content (>0 bytes), avoid duplicate re-copy!
            if (targetFile.exists() && targetFile.length() > 0) {
                val realSourcePath = sourcePath ?: resolveRealPathFromUri(sourceUri)
                if (realSourcePath != null) markPathAsHidden(realSourcePath)
                markPathAsHidden(targetFile.name)
                return@withContext targetFile
            }

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

    private fun isCanonicalExternalMediaUri(uri: Uri?): Boolean {
        if (uri == null) return false
        val s = uri.toString()
        return s.startsWith("content://media/external/video/media/") ||
               s.startsWith("content://media/external/images/media/")
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

        // 3. Query MediaStore by physical file path
        val path = filePath ?: resolveRealPathFromUri(uri)
        if (!path.isNullOrBlank()) {
            try {
                ctx.contentResolver.query(
                    baseUri,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DATA} = ?",
                    arrayOf(path),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                        if (idCol >= 0) {
                            val id = cursor.getLong(idCol)
                            return ContentUris.withAppendedId(baseUri, id)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Query MediaStore by Display Name and Size (supports Android 13+ PhotoPicker URIs)
        if (uri != null) {
            try {
                var displayName: String? = null
                var fileSize: Long = -1L
                ctx.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                    }
                }

                if (!displayName.isNullOrBlank()) {
                    val selection = if (fileSize > 0L) {
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?"
                    } else {
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                    }
                    val args = if (fileSize > 0L) arrayOf(displayName!!, fileSize.toString()) else arrayOf(displayName!!)
                    ctx.contentResolver.query(
                        baseUri,
                        arrayOf(MediaStore.MediaColumns._ID),
                        selection,
                        args,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                            if (idCol >= 0) {
                                val id = cursor.getLong(idCol)
                                return ContentUris.withAppendedId(baseUri, id)
                            }
                        }
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
        val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val resolvedPath = filePath ?: resolveRealPathFromUri(uri)
        val canonicalUri = resolveCanonicalMediaStoreUri(ctx, uri, resolvedPath, isVideo)

        // 1. If we have the absolute path, attempt physical file deletion (works with MANAGE_EXTERNAL_STORAGE)
        if (!resolvedPath.isNullOrBlank()) {
            val file = File(resolvedPath)
            if (file.exists()) {
                try {
                    physicallyDeleted = file.delete()
                } catch (_: Exception) {}
            } else {
                physicallyDeleted = true
            }
        }

        // 2. Direct ContentResolver delete on canonical MediaStore URI
        if (canonicalUri != null && isCanonicalExternalMediaUri(canonicalUri)) {
            try {
                val count = ctx.contentResolver.delete(canonicalUri, null, null)
                if (count > 0) physicallyDeleted = true
            } catch (_: Exception) {}
        }

        // 3. Direct path delete in MediaStore
        if (!resolvedPath.isNullOrBlank()) {
            try {
                val count = ctx.contentResolver.delete(baseUri, "${MediaStore.MediaColumns.DATA} = ?", arrayOf(resolvedPath))
                if (count > 0) physicallyDeleted = true
            } catch (_: Exception) {}

            // 4. Force MediaScanner to update gallery
            try {
                MediaScannerConnection.scanFile(ctx, arrayOf(resolvedPath), null, null)
            } catch (_: Exception) {}
        }

        // Check if file is actually gone
        if (!resolvedPath.isNullOrBlank()) {
            if (!File(resolvedPath).exists()) {
                physicallyDeleted = true
            }
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
            if (canonicalUri != null && isCanonicalExternalMediaUri(canonicalUri)) {
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

    fun getMultipleDeleteRequestPendingIntent(
        ctx: Context,
        items: List<Pair<Uri?, String?>>,
        isVideo: Boolean
    ): PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val canonicalUris = items.mapNotNull { resolveCanonicalMediaStoreUri(ctx, it.first, it.second, isVideo) }
                .filter { isCanonicalExternalMediaUri(it) }
            if (canonicalUris.isNotEmpty()) {
                return try {
                    MediaStore.createDeleteRequest(ctx.contentResolver, canonicalUris)
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
