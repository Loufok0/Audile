package com.mrsep.musicrecognizer.core.network

import android.util.Log
import com.mrsep.musicrecognizer.core.domain.track.model.Track
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface CustomServerService {
    suspend fun sendTrackData(track: Track, serverUrl: String): Result<Unit>
}

internal class CustomServerServiceImpl @Inject constructor() : CustomServerService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun sendTrackData(track: Track, serverUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (serverUrl.isEmpty()) {
                return@withContext Result.success(Unit)
            }
            try {
                val payload = TrackPayload(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    releaseDate = track.releaseDate?.toString(),
                    duration = track.duration?.inWholeSeconds,
                    recognizedAt = track.recognizedAt?.inWholeSeconds,
                    recognizedBy = track.recognizedBy.name,
                    recognitionDate = track.recognitionDate.toString(),
                    isrc = track.isrc,
                    artworkUrl = track.artworkUrl,
                    artworkThumbUrl = track.artworkThumbUrl,
                    trackLinks = track.trackLinks.mapKeys { it.key.name }
                )

                val jsonString = json.encodeToString(payload)
                val responseCode = sendPostRequest(serverUrl, jsonString)

                val safeUrl = serverUrl.substringBefore('?')
                if (responseCode in 200..299) {
                    Log.d(TAG, "Successfully sent track data to $safeUrl")
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "Server returned HTTP $responseCode for $safeUrl")
                    Result.failure(Exception("Server returned HTTP $responseCode"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception while sending track data", e)
                Result.failure(e)
            }
        }

    /** Blocking call, must be invoked from Dispatchers.IO. Throws on network errors. */
    private fun sendPostRequest(serverUrl: String, jsonPayload: String): Int {
        val bytes = jsonPayload.toByteArray(Charsets.UTF_8)
        val connection = URL(serverUrl).openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setFixedLengthStreamingMode(bytes.size)
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            connection.outputStream.use { output ->
                output.write(bytes)
                output.flush()
            }

            return connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "CustomServerService"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CustomServerModule {

    @Binds
    @Singleton
    abstract fun bindCustomServerService(impl: CustomServerServiceImpl): CustomServerService
}

@Serializable
internal data class TrackPayload(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("artist")
    val artist: String,
    @SerialName("album")
    val album: String?,
    @SerialName("releaseDate")
    val releaseDate: String?,
    @SerialName("duration")
    val duration: Long?,
    @SerialName("recognizedAt")
    val recognizedAt: Long?,
    @SerialName("recognizedBy")
    val recognizedBy: String,
    @SerialName("recognitionDate")
    val recognitionDate: String,
    @SerialName("isrc")
    val isrc: String?,
    @SerialName("artworkUrl")
    val artworkUrl: String?,
    @SerialName("artworkThumbUrl")
    val artworkThumbUrl: String?,
    @SerialName("trackLinks")
    val trackLinks: Map<String, String>,
)
