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
    private val pinKey = "vault_pin_hash"

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

    init {
        ensureNoMedia(vaultDir)
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

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun getVaultItems(): List<VaultItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<VaultItem>()

        videoVaultDir.listFiles()?.filter { it.isFile && it.name != ".nomedia" }?.forEach { f ->
            list.add(
                VaultItem(
                    id = f.name,
                    file = f,
                    name = f.nameWithoutExtension,
                    isVideo = true,
                    sizeBytes = f.length(),
                    dateAdded = f.lastModified()
                )
            )
        }

        photoVaultDir.listFiles()?.filter { it.isFile && it.name != ".nomedia" }?.forEach { f ->
            list.add(
                VaultItem(
                    id = f.name,
                    file = f,
                    name = f.nameWithoutExtension,
                    isVideo = false,
                    sizeBytes = f.length(),
                    dateAdded = f.lastModified()
                )
            )
        }

        list.sortedByDescending { it.dateAdded }
    }

    suspend fun copyMediaToVault(
        sourcePath: String? = null,
        sourceUri: Uri? = null,
        isVideo: Boolean,
        customName: String? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            val targetDir = if (isVideo) videoVaultDir else photoVaultDir
            val ext = if (sourcePath != null && sourcePath.contains(".")) {
                "." + sourcePath.substringAfterLast(".")
            } else if (isVideo) ".mp4" else ".jpg"

            val baseName = customName ?: "hidden_${System.currentTimeMillis()}"
            val targetFile = File(targetDir, "${baseName}${ext}")

            if (sourcePath != null && File(sourcePath).exists()) {
                FileInputStream(File(sourcePath)).use { input ->
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

            if (targetFile.exists() && targetFile.length() > 0) targetFile else null
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
            item.file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
