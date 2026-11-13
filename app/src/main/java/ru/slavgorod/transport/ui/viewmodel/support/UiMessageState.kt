package ru.slavgorod.transport.ui.viewmodel.support

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.slavgorod.transport.ui.model.UiMessage
import ru.slavgorod.transport.ui.model.uiMessage

/** Хранит одно текущее сообщение интерфейса, заменяемое при следующей публикации. */
class UiMessageState {

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    /** Поток текущего сообщения; `null` означает отсутствие сообщения. */
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    /** Удаляет текущее сообщение. */
    fun clear() {
        _uiMessage.value = null
    }

    /** Заменяет текущее сообщение строковым ресурсом и его аргументами. */
    fun publish(
        @StringRes messageResId: Int,
        messageArgs: List<String> = emptyList()
    ) {
        _uiMessage.value = uiMessage(
            textResId = messageResId,
            textArgs = messageArgs
        )
    }
}
