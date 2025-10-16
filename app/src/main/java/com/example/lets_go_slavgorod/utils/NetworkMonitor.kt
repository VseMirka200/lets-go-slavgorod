package com.example.lets_go_slavgorod.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * Монитор сетевого подключения
 * 
 * Отслеживает состояние интернет-соединения в реальном времени
 * и предоставляет реактивный Flow для наблюдения за изменениями.
 * 
 * Особенности:
 * - Поддержка Android 6.0+
 * - Реактивное отслеживание через Flow
 * - Автоматическая подписка/отписка от событий
 * - Определение типа сети (WiFi, Cellular)
 * 
 * Использование:
 * ```kotlin
 * NetworkMonitor.observeConnectivity(context)
 *     .collect { isConnected ->
 *         if (isConnected) {
 *             // Есть интернет
 *         } else {
 *             // Нет интернета
 *         }
 *     }
 * ```
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
object NetworkMonitor {

    /**
     * Наблюдает за состоянием сетевого подключения
     * 
     * Возвращает Flow, который эмитит true при наличии соединения
     * и false при его отсутствии. Автоматически обновляется при
     * изменении состояния сети.
     * 
     * @param context контекст приложения
     * @return Flow<Boolean> с состоянием подключения
     */
    fun observeConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Отправляем текущее состояние сразу
        trySend(isConnected(context))
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.d("Network available: $network")
                trySend(true)
            }

            override fun onLost(network: Network) {
                Timber.d("Network lost: $network")
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                Timber.d("Network capabilities changed: hasInternet=$hasInternet")
                trySend(hasInternet)
            }
        }

        // Регистрируем callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        }

        // Отписываемся при закрытии Flow
        awaitClose {
            Timber.d("Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged() // Избегаем дублирующихся событий

    /**
     * Проверяет наличие интернет-соединения
     * 
     * Синхронная проверка текущего состояния сети.
     * Для реактивного наблюдения используйте observeConnectivity().
     * Использует современные API для Android 6.0+.
     * 
     * @param context контекст приложения
     * @return true если есть соединение, false иначе
     */
    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            // Fallback для Android < 6.0
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnected == true
        }
    }

    /**
     * Определяет тип текущего подключения
     * 
     * Использует современные API для Android 6.0+.
     * 
     * @param context контекст приложения
     * @return тип подключения (WiFi, Cellular, Ethernet, Unknown, None)
     */
    fun getConnectionType(context: Context): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.NONE
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                else -> ConnectionType.UNKNOWN
            }
        } else {
            // Fallback для Android < 6.0
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> ConnectionType.WIFI
                ConnectivityManager.TYPE_MOBILE -> ConnectionType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> ConnectionType.ETHERNET
                else -> {
                    @Suppress("DEPRECATION")
                    if (networkInfo?.isConnected == true) ConnectionType.UNKNOWN else ConnectionType.NONE
                }
            }
        }
    }

    /**
     * Типы сетевых подключений
     */
    enum class ConnectionType {
        /** WiFi подключение */
        WIFI,
        
        /** Мобильная сеть */
        CELLULAR,
        
        /** Ethernet подключение */
        ETHERNET,
        
        /** Неизвестный тип подключения */
        UNKNOWN,
        
        /** Нет подключения */
        NONE
    }
}

