package ru.slavgorod.transport.core

import android.content.Context
import androidx.annotation.StringRes

object AppText {

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(@StringRes resId: Int, vararg args: Any): String {
        val context = appContext
        return if (context != null) {
            runCatching { context.formatString(resId, args.toList()) }
                .getOrElse { fallback(resId) }
        } else {
            fallback(resId)
        }
    }

    private fun fallback(@StringRes resId: Int): String = "string:$resId"
}

internal fun Context.formatString(
    @StringRes resId: Int,
    args: List<*>
): String {
    return when (args.size) {
        0 -> getString(resId)
        1 -> getString(resId, args[0])
        2 -> getString(resId, args[0], args[1])
        else -> getString(resId, *args.toTypedArray())
    }
}
