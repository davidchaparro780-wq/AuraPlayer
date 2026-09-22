package com.auraplayer.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
        }
    }

    private val videoVaultDir: File by lazy {
        File(vaultDir, "videos").apply {
            if (!exists()) mkdirs()
        }
    }

    private val photoVaultDir: File by lazy {
        File(vaultDir, "photos").apply {
            if (!exists()) mkdirs()
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

        videoVaultDir.listFiles()?.filter { it.isFile }?.forEach { f ->
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

        photoVaultDir.listFiles()?.filter { it.isFile }?.forEach { f ->
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

    suspend fun hideMediaFromUri(uri: Uri, isVideo: Boolean, customName: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = if (isVideo) videoVaultDir else photoVaultDir
            val ext = if (isVideo) ".mp4" else ".jpg"
            val baseName = customName ?: "hidden_${System.currentTimeMillis()}"
            val targetFile = File(targetDir, "${baseName}${ext}")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Try to delete from MediaStore if user gave permission / accessible
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {}

            targetFile.exists() && targetFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun hideMediaFile(sourcePath: String, isVideo: Boolean, sourceUri: Uri? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val targetDir = if (isVideo) videoVaultDir else photoVaultDir
            val ext = sourceFile.extension.ifBlank { if (isVideo) "mp4" else "jpg" }
            val fileName = "${sourceFile.nameWithoutExtension}_${System.currentTimeMillis()}.$ext"
            val targetFile = File(targetDir, fileName)

            if (sourceFile.exists()) {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                sourceFile.delete()
            } else if (sourceUri != null) {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Delete from MediaStore so it vanishes from gallery
            if (sourceUri != null) {
                try {
                    context.contentResolver.delete(sourceUri, null, null)
                } catch (_: Exception) {}
            }

            if (sourcePath.isNotBlank()) {
                MediaScannerConnection.scanFile(context, arrayOf(sourcePath), null, null)
            }

            targetFile.exists() && targetFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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
