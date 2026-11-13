package ru.slavgorod.transport.notifications

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Отслеживает состояние процесса приложения по его жизненному циклу. */
class AppForegroundTracker {

    private val _isAppForeground = MutableStateFlow(false)
    /** До установки наблюдателя значение равно `false`. */
    val isAppForeground: StateFlow<Boolean> = _isAppForeground.asStateFlow()

    /** Регистрирует наблюдатель и сразу синхронизирует состояние с жизненным циклом. */
    fun install() {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    updateForegroundState(true)
                }

                override fun onStop(owner: LifecycleOwner) {
                    updateForegroundState(false)
                }
            }
        )

        updateForegroundState(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
    }

    /** Подменяет состояние для тестового сценария до следующего события жизненного цикла. */
    fun setForegroundForTesting(isForeground: Boolean) {
        updateForegroundState(isForeground)
    }

    private fun updateForegroundState(isForeground: Boolean) {
        _isAppForeground.value = isForeground
    }
}
