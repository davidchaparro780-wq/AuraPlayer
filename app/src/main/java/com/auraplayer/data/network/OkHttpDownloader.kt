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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : Downloader() {

    companion object {
        val instance: OkHttpDownloader by lazy { OkHttpDownloader() }
    }

    override fun execute(request: Request): Response {
        val builder = OkRequest.Builder().url(request.url())
        
        request.headers()?.forEach { (key, values) ->
            values.forEach { v ->
                builder.addHeader(key, v)
            }
        }

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
