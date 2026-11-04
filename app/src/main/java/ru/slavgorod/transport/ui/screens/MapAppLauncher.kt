package ru.slavgorod.transport.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import ru.slavgorod.transport.data.repository.RouteStopsCatalogStore
import ru.slavgorod.transport.data.local.PreferencesKeys
import ru.slavgorod.transport.data.local.dataStore
import java.util.Locale

/** An explicit map provider; coordinates must be verified before navigation is offered. */
enum class PreferredMapApp(val label: String) {
    SYSTEM("Системное приложение"),
    YANDEX("Яндекс Карты"),
    TWO_GIS("2ГИС")
}

fun Context.preferredMapApp(): Flow<PreferredMapApp> = dataStore.data.map { preferences ->
    runCatching {
        PreferredMapApp.valueOf(preferences[PreferencesKeys.preferredMapApp] ?: "SYSTEM")
    }.getOrDefault(PreferredMapApp.SYSTEM)
}

suspend fun Context.setPreferredMapApp(app: PreferredMapApp) {
    dataStore.edit { it[PreferencesKeys.preferredMapApp] = app.name }
}

private data class StopCoordinate(val latitude: Double, val longitude: Double) {
    fun pair() = String.format(Locale.US, "%.7f,%.7f", latitude, longitude)
}

/** Coordinates are read from the same editable catalog as stop names. Only verified points are opened. */
private object StopCoordinateCatalog {
    fun get(context: Context, routeId: String, stopName: String): StopCoordinate? {
        val data = runCatching {
            val file = RouteStopsCatalogStore.cachedFile(context)
            val json = if (file.isFile) file.readText(Charsets.UTF_8)
                else context.assets.open(RouteStopsCatalogStore.ASSET).bufferedReader().use { it.readText() }
            JSONObject(json)
        }.getOrNull() ?: return null
        val stops = data.optJSONArray(routeId) ?: return null
        for (index in 0 until stops.length()) {
            val stop = stops.optJSONObject(index) ?: continue
            if (stop.optString("name") != stopName || !stop.optBoolean("verified", false)) continue
            if (stop.isNull("latitude") || stop.isNull("longitude")) return null
            val latitude = stop.optDouble("latitude", Double.NaN)
            val longitude = stop.optDouble("longitude", Double.NaN)
            if (latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                return StopCoordinate(latitude, longitude)
            }
        }
        return null
    }
}

fun openStopMap(context: Context, routeId: String, stopName: String, preferred: PreferredMapApp) {
    val coordinates = StopCoordinateCatalog.get(context, routeId, stopName)
    if (coordinates == null) {
        Toast.makeText(context, "Координаты остановки пока не указаны", Toast.LENGTH_LONG).show()
        return
    }
    openMapCoordinates(context, coordinates, preferred)
}

fun openRouteMap(context: Context, routeId: String, stopNames: List<String>, preferred: PreferredMapApp) {
    val points = stopNames.mapNotNull { StopCoordinateCatalog.get(context, routeId, it) }
    if (points.size != stopNames.size || points.size < 2) {
        Toast.makeText(context, "Для маршрута ещё нет полного набора координат", Toast.LENGTH_LONG).show()
        return
    }
    val first = points.first()
    val last = points.last()
    // This opens endpoints by GPS coordinates, not an invented bus route or a search query.
    val routeUri = when (preferred) {
        PreferredMapApp.YANDEX -> Uri.parse("https://yandex.ru/maps/?rtext=${first.pair()}~${last.pair()}&rtt=mt")
        PreferredMapApp.TWO_GIS -> Uri.parse("https://2gis.ru/directions/points/${first.longitude},${first.latitude};${last.longitude},${last.latitude}")
        PreferredMapApp.SYSTEM -> Uri.parse("geo:${first.pair()}")
    }
    val providerPackage = when (preferred) {
        PreferredMapApp.YANDEX -> "ru.yandex.yandexmaps"
        PreferredMapApp.TWO_GIS -> "ru.dublgis.dgismobile"
        PreferredMapApp.SYSTEM -> null
    }
    launchWithFallback(context, routeUri, providerPackage)
}

private fun openMapCoordinates(context: Context, point: StopCoordinate, preferred: PreferredMapApp) {
    val uri = when (preferred) {
        PreferredMapApp.SYSTEM -> Uri.parse("geo:${point.pair()}?z=17")
        PreferredMapApp.YANDEX -> Uri.parse("https://yandex.ru/maps/?ll=${point.longitude},${point.latitude}&z=17&pt=${point.longitude},${point.latitude},pm2rdm")
        PreferredMapApp.TWO_GIS -> Uri.parse("https://2gis.ru/geo/${point.longitude},${point.latitude}")
    }
    val packageName = when (preferred) {
        PreferredMapApp.SYSTEM -> null
        PreferredMapApp.YANDEX -> "ru.yandex.yandexmaps"
        PreferredMapApp.TWO_GIS -> "ru.dublgis.dgismobile"
    }
    launchWithFallback(context, uri, packageName)
}

private fun launchWithFallback(context: Context, uri: Uri, packageName: String?) {
    fun launch(targetPackage: String?): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (targetPackage != null) setPackage(targetPackage)
        })
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
    if (!(packageName != null && launch(packageName)) && !launch(null)) {
        Toast.makeText(context, "Не удалось открыть карты", Toast.LENGTH_SHORT).show()
    }
}
