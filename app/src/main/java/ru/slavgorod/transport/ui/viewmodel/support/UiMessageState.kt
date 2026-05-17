package ru.slavgorod.transport.ui.viewmodel.support

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.slavgorod.transport.ui.model.UiMessage
import ru.slavgorod.transport.ui.model.uiMessage

class UiMessageState {

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    fun clear() {
        _uiMessage.value = null
    }

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
