package ru.slavgorod.transport.data.repository

import android.content.Context
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.slavgorod.transport.core.ScheduleConfig
import timber.log.Timber
import java.io.File

/**
 * Single route-stops catalog: remote JSON -> last valid downloaded file -> bundled assets.
 * The same persisted catalog is read by map buttons (no separate coordinates file).
 */
class RouteStopsCatalogStore(
    private val context: Context,
    private val client: OkHttpClient,
    private val url: String = ScheduleConfig.remoteStopsUrl
) {
    fun stopNames(): Map<String, List<String>> = parse(readCurrent()).orEmpty()

    private fun readCurrent(): String = runCatching {
        val cached = cachedFile(context)
        if (cached.isFile) cached.readText(Charsets.UTF_8)
        else context.assets.open(ASSET).bufferedReader().use { it.readText() }
    }.getOrDefault("{}")

    /** Returns true only when a complete, valid remote document replaced the cached catalog. */
    fun refresh(): Boolean {
        val responseText = try {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.w(exception, "Не удалось загрузить данные остановок")
            null
        } ?: return false

        if (parse(responseText) == null) {
            Timber.w("Удалённый каталог остановок не прошёл проверку, сохранена предыдущая версия")
            return false
        }
        if (responseText == readCurrent()) return false
        return try {
            val target = cachedFile(context)
            val temp = File(target.parentFile, "${ASSET}.tmp")
            temp.writeText(responseText, Charsets.UTF_8)
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            true
        } catch (exception: Exception) {
            Timber.w(exception, "Не удалось сохранить обновлённые остановки")
            false
        }
    }

    companion object {
        const val ASSET = "route-stops.json"
        fun cachedFile(context: Context): File = File(context.filesDir, ASSET)

        /** Strict enough to prevent a truncated/empty response from wiping known stops. */
        fun parse(json: String): Map<String, List<String>>? {
            return try {
            val root = JSONObject(json)
            if (root.length() == 0) return null
            val result = linkedMapOf<String, List<String>>()
            root.keys().forEach { routeId ->
                if (routeId.isBlank()) return null
                val entries = root.optJSONArray(routeId) ?: return null
                if (entries.length() == 0) return null
                val names = (0 until entries.length()).map { index ->
                    val stop = entries.optJSONObject(index) ?: return null
                    val name = stop.optString("name").trim()
                    if (name.isEmpty()) return null
                    val lat = stop.opt("latitude")
                    val lon = stop.opt("longitude")
                    if ((lat == JSONObject.NULL) != (lon == JSONObject.NULL)) return null
                    if (lat != null && lat != JSONObject.NULL) {
                        if (lat !is Number || lon !is Number) return null
                        val latitude = lat.toDouble()
                        val longitude = lon.toDouble()
                        if (!latitude.isFinite() || !longitude.isFinite() ||
                            latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
                    }
                    name
                }
                result[routeId] = names
            }
            result
            } catch (_: Exception) {
                null
            }
        }
    }
}
