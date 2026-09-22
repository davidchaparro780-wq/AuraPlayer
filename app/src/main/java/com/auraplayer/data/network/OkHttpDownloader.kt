package com.auraplayer.data.network

import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class OkHttpDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) : Downloader() {

    companion object {
        val instance: OkHttpDownloader by lazy { OkHttpDownloader() }
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }

    override fun execute(request: Request): Response {
        val builder = OkRequest.Builder().url(request.url())
        
        var hasUserAgent = false
        request.headers()?.forEach { (key, values) ->
            if (key.equals("User-Agent", ignoreCase = true)) hasUserAgent = true
            values.forEach { v ->
                builder.addHeader(key, v)
            }
        }

        if (!hasUserAgent) {
            builder.header("User-Agent", USER_AGENT)
        }
        builder.header("Accept-Language", "es-ES,es;q=0.9,en-US;q=0.8,en;q=0.7")
        builder.header("Cookie", "SOCS=CAESEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg")

        if (request.httpMethod().equals("POST", ignoreCase = true)) {
            val bodyBytes = request.dataToSend() ?: ByteArray(0)
            builder.post(bodyBytes.toRequestBody())
        } else if (request.httpMethod().equals("HEAD", ignoreCase = true)) {
            builder.head()
        } else {
            builder.get()
        }

        val okResponse = client.newCall(builder.build()).execute()
        val bodyString = okResponse.body?.string() ?: ""
        val responseHeaders = okResponse.headers.toMultimap()

        return Response(
            okResponse.code,
            okResponse.message,
            responseHeaders,
            bodyString,
            okResponse.request.url.toString()
        )
    }
}
