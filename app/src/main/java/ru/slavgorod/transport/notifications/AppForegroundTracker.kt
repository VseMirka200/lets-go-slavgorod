package ru.slavgorod.transport.notifications

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppForegroundTracker {

    private val _isAppForeground = MutableStateFlow(false)
    val isAppForeground: StateFlow<Boolean> = _isAppForeground.asStateFlow()

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

    fun setForegroundForTesting(isForeground: Boolean) {
        updateForegroundState(isForeground)
    }

    private fun updateForegroundState(isForeground: Boolean) {
        _isAppForeground.value = isForeground
    }
}
