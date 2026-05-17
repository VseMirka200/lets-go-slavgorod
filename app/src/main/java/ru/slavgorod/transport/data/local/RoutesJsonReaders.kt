package ru.slavgorod.transport.data.local

import com.google.gson.JsonElement
import com.google.gson.JsonObject

internal fun JsonObject.requiredText(key: String): String? {
    return optionalText(key)
}

internal fun JsonObject.optionalText(vararg keys: String): String? {
    for (key in keys) {
        val element = get(key) ?: continue
        if (element.isJsonNull) continue
        return element.asStringOrNull()
    }
    return null
}

internal fun JsonElement.asStringOrNull(): String? {
    return runCatching { asString.trim() }
        .getOrNull()
        ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
}

internal fun JsonElement.asJsonObjectOrNull(): JsonObject? {
    return runCatching { asJsonObject }.getOrNull()
}
