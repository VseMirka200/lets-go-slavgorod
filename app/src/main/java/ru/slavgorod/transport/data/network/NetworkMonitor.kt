package ru.slavgorod.transport.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Наблюдает за состоянием системной сети без выполнения пробного HTTP-запроса. */
object NetworkMonitor {

    /**
     * Проверяет признак доступа в интернет у активной сети Android.
     * Этот признак не гарантирует, что конкретный сервер расписания доступен:
     * запрос может завершиться ошибкой, например из-за авторизации или DNS.
     *
     * @param context Контекст для получения системной службы соединений.
     * @return `false`, если служба, активная сеть или нужный признак отсутствуют.
     */
    fun isConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Передаёт текущее состояние и последующие изменения до отмены подписки.
     * Каждая подписка регистрирует собственный системный обработчик, который
     * снимается при завершении сбора потока.
     */
    fun observeConnectionState(context: Context): Flow<Boolean> = callbackFlow {
        val appContext = context.applicationContext
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (connectivityManager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        trySend(isConnected(appContext))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                trySend(isConnected(appContext))
            }

            override fun onLost(network: android.net.Network) {
                trySend(isConnected(appContext))
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(isConnected(appContext))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        // Регистрация принадлежит подписке и освобождается при её завершении.
        awaitClose {
            runCatching {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }
}
