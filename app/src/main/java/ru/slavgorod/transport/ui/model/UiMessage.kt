package ru.slavgorod.transport.ui.model

import androidx.annotation.StringRes
import java.util.concurrent.atomic.AtomicLong

data class UiMessage(
    val id: Long,
    val text: String? = null,
    @param:StringRes val textResId: Int? = null,
    val textArgs: List<String> = emptyList()
)

private val uiMessageIds = AtomicLong(0L)

fun uiMessage(text: String): UiMessage {
    return UiMessage(
        id = nextUiMessageId(),
        text = text
    )
}

fun uiMessage(
    @StringRes textResId: Int,
    textArgs: List<String> = emptyList()
): UiMessage {
    return UiMessage(
        id = nextUiMessageId(),
        textResId = textResId,
        textArgs = textArgs
    )
}

private fun nextUiMessageId(): Long = uiMessageIds.incrementAndGet()
