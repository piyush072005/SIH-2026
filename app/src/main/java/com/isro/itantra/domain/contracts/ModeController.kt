package com.isro.itantra.domain.contracts

import com.isro.itantra.domain.contracts.models.OperationalMode
import kotlinx.coroutines.flow.Flow

/**
 * Shared contract for coordinating system operational modes and alert overrides.
 *
 * Implemented by Backend scope (`mode/`).
 * Consumed by UI scope (`ui/`).
 */
interface ModeController {
    /**
     * Emits the currently active operational mode (PushToTalk, Duplex, or Alert).
     */
    val currentMode: Flow<OperationalMode>

    /**
     * Emits the previous operational mode before an emergency alert override was triggered.
     * Null if no alert override is currently active.
     */
    val preAlertMode: Flow<OperationalMode?>

    /**
     * Switches the primary operating mode.
     *
     * @param mode The desired [OperationalMode] (PushToTalk or Duplex).
     */
    fun setMode(mode: OperationalMode)

    /**
     * Triggers an emergency alert override, transitioning the system immediately into [OperationalMode.Alert].
     *
     * @param alertMessage Optional textual alert payload broadcasted to peers.
     */
    fun triggerAlert(alertMessage: String? = null)

    /**
     * Clears an ongoing emergency alert override and restores the previous operating mode.
     */
    fun clearAlert()
}
