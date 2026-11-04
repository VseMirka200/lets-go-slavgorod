package ru.slavgorod.transport.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.slavgorod.transport.data.local.PreferencesKeys
import ru.slavgorod.transport.data.local.dataStore

/** A preference for opening searches, not a claim of verified GPS stop locations. */
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

fun openStopMap(context: Context, stopName: String, routeName: String, preferred: PreferredMapApp) {
    openMapSearch(context, "остановка $stopName, $routeName, Алтайский край", preferred)
}

fun openRouteMap(context: Context, routeName: String, preferred: PreferredMapApp) {
    openMapSearch(context, "Автобус $routeName, Алтайский край", preferred)
}

private fun openMapSearch(context: Context, query: String, preferred: PreferredMapApp) {
    val encoded = Uri.encode(query)
    val geo = Uri.parse("geo:0,0?q=$encoded")
    val yandex = Uri.parse("https://yandex.ru/maps/?text=$encoded")
    val twoGis = Uri.parse("https://2gis.ru/search/$encoded")

    fun launch(uri: Uri, packageName: String? = null): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            if (packageName != null) setPackage(packageName)
        })
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    val opened = when (preferred) {
        PreferredMapApp.SYSTEM -> launch(geo) || launch(yandex)
        PreferredMapApp.YANDEX -> launch(yandex, "ru.yandex.yandexmaps") || launch(yandex)
        PreferredMapApp.TWO_GIS -> launch(twoGis, "ru.dublgis.dgismobile") || launch(twoGis)
    }
    if (!opened) Toast.makeText(context, "Не удалось открыть карты", Toast.LENGTH_SHORT).show()
}
