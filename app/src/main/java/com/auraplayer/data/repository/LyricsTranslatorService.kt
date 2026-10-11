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
    // Caché en memoria acotada (LRU) para evitar fugas de memoria con miles de letras
    private val translationCache = object : LinkedHashMap<String, String>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 1000
        }
    }

    suspend fun translateLineToSpanish(originalText: String): String = withContext(Dispatchers.IO) {
        val clean = originalText.trim()
        if (clean.isBlank()) return@withContext ""

        synchronized(translationCache) {
            translationCache[clean]?.let { return@withContext it }
        }

        // Si es muy corto / no alfabético, retornar
        if (clean.length < 2) return@withContext clean

        var conn: HttpURLConnection? = null
        try {
            // Google Translate Web Public API Endpoint
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=es&dt=t&q=$encoded"
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
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
                        synchronized(translationCache) {
                            translationCache[clean] = translated
                        }
                        return@withContext translated
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.d(TAG, "Traducción online no disponible: ${e.message}")
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Throwable) {}
        }

        // Fallback: retornar original
        return@withContext clean
    }

    suspend fun batchTranslate(lines: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        val needed = mutableListOf<String>()

        synchronized(translationCache) {
            for (line in lines) {
                val clean = line.trim()
                if (clean.isBlank()) continue
                val cached = translationCache[clean]
                if (cached != null) {
                    result[clean] = cached
                } else {
                    needed.add(clean)
                }
            }
        }

        if (needed.isEmpty()) return@withContext result

        // Agrupar en bloques de 20 líneas unidas con salto de línea para traducir todo en 1-2 peticiones
        val chunks = needed.chunked(20)
        for (chunk in chunks) {
            val joinedText = chunk.joinToString("\n")
            var conn: HttpURLConnection? = null
            try {
                val encoded = URLEncoder.encode(joinedText, "UTF-8")
                val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=es&dt=t&q=$encoded"
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
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
                        val translatedAll = sb.toString().split("\n")
                        for (i in chunk.indices) {
                            val orig = chunk[i]
                            val trans = translatedAll.getOrNull(i)?.trim()
                            val finalTrans = if (!trans.isNullOrBlank()) trans else orig
                            result[orig] = finalTrans
                            synchronized(translationCache) {
                                translationCache[orig] = finalTrans
                            }
                        }
                    }
                } else {
                    for (orig in chunk) {
                        result[orig] = orig
                    }
                }
            } catch (e: Exception) {
                AppLog.d(TAG, "Error en lote de traducción: ${e.message}")
                for (orig in chunk) {
                    result[orig] = orig
                }
            } finally {
                try {
                    conn?.disconnect()
                } catch (_: Throwable) {}
            }
        }

        result
    }
}
