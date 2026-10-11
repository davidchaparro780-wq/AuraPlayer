package com.auraplayer.data.repository

import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Servicio de traducción en tiempo real para letras musicales.
 * Traduce líneas de letras (inglés, coreano, japonés, etc.) al español usando APIs libres de traducción,
 * con almacenamiento en caché en memoria y fallback instantáneo sin dependencias pesadas.
 */
object LyricsTranslatorService {

    private const val TAG = "LyricsTranslatorService"
    private val translationCache = mutableMapOf<String, String>()

    suspend fun translateLineToSpanish(originalText: String): String = withContext(Dispatchers.IO) {
        val clean = originalText.trim()
        if (clean.isBlank()) return@withContext ""
        
        // Retornar de la caché si ya se tradujo
        translationCache[clean]?.let { return@withContext it }

        // Si ya parece español o es muy corto / no alfabético, retornar
        if (clean.length < 2) return@withContext clean

        try {
            // Google Translate Web Public API Endpoint
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=es&dt=t&q=$encoded"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONArray(response)
                val sentences = json.optJSONArray(0)
                if (sentences != null && sentences.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until sentences.length()) {
                        val part = sentences.optJSONArray(i)
                        if (part != null && part.length() > 0) {
                            sb.append(part.optString(0))
                        }
                    }
                    val translated = sb.toString().trim()
                    if (translated.isNotBlank()) {
                        translationCache[clean] = translated
                        return@withContext translated
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.d(TAG, "Traducción online no disponible: ${e.message}")
        }

        // Fallback: retornar original
        return@withContext clean
    }

    suspend fun batchTranslate(lines: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        for (line in lines) {
            if (line.isNotBlank()) {
                result[line] = translateLineToSpanish(line)
            }
        }
        result
    }
}
