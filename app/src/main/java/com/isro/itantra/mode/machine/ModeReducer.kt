package com.isro.itantra.mode.machine

import com.isro.itantra.domain.contracts.models.OperationalMode

sealed class ModeEvent {
    data class SetMode(val mode: OperationalMode) : ModeEvent()
    data class RaiseAlert(val text: String?) : ModeEvent()
    data object ClearAlert : ModeEvent()
}

data class ModeMachineState(
    val mode: OperationalMode = OperationalMode.PushToTalk,
    val preAlertMode: OperationalMode? = null,
    val alertText: String? = null,
)

object ModeReducer {
    fun reduce(state: ModeMachineState, event: ModeEvent): ModeMachineState {
        return when (event) {
            is ModeEvent.RaiseAlert -> {
                val previous = when (state.mode) {
                    OperationalMode.Alert -> state.preAlertMode ?: OperationalMode.PushToTalk
                    else -> state.mode
                }
                state.copy(
                    mode = OperationalMode.Alert,
                    preAlertMode = previous,
                    alertText = event.text,
                )
            }
            ModeEvent.ClearAlert -> {
                val restored = state.preAlertMode ?: OperationalMode.PushToTalk
                state.copy(mode = restored, preAlertMode = null, alertText = null)
            }
            is ModeEvent.SetMode -> {
                if (state.mode == OperationalMode.Alert) state
                else if (event.mode == OperationalMode.Alert) {
                    reduce(state, ModeEvent.RaiseAlert(null))
                } else {
                    state.copy(mode = event.mode, preAlertMode = null, alertText = null)
                }
            }
        }
    }
}
