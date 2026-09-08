package com.isro.itantra.mode.machine

import com.google.common.truth.Truth.assertThat
import com.isro.itantra.domain.contracts.models.OperationalMode
import org.junit.Test

class ModeReducerTest {
    @Test
    fun setModeDuplexFromPtt() {
        val next = ModeReducer.reduce(ModeMachineState(), ModeEvent.SetMode(OperationalMode.Duplex))
        assertThat(next.mode).isEqualTo(OperationalMode.Duplex)
        assertThat(next.preAlertMode).isNull()
    }

    @Test
    fun alertOverridesAndRestores() {
        var state = ModeReducer.reduce(ModeMachineState(), ModeEvent.SetMode(OperationalMode.Duplex))
        state = ModeReducer.reduce(state, ModeEvent.RaiseAlert("SOS"))
        assertThat(state.mode).isEqualTo(OperationalMode.Alert)
        assertThat(state.preAlertMode).isEqualTo(OperationalMode.Duplex)
        assertThat(state.alertText).isEqualTo("SOS")
        state = ModeReducer.reduce(state, ModeEvent.SetMode(OperationalMode.PushToTalk))
        assertThat(state.mode).isEqualTo(OperationalMode.Alert)
        state = ModeReducer.reduce(state, ModeEvent.ClearAlert)
        assertThat(state.mode).isEqualTo(OperationalMode.Duplex)
        assertThat(state.preAlertMode).isNull()
    }

    @Test
    fun stackedAlertKeepsOriginalPrevious() {
        var state = ModeReducer.reduce(ModeMachineState(), ModeEvent.SetMode(OperationalMode.Duplex))
        state = ModeReducer.reduce(state, ModeEvent.RaiseAlert("first"))
        state = ModeReducer.reduce(state, ModeEvent.RaiseAlert("second"))
        state = ModeReducer.reduce(state, ModeEvent.ClearAlert)
        assertThat(state.mode).isEqualTo(OperationalMode.Duplex)
    }
}
