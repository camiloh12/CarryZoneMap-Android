package com.carryzonemap.app.ui.state

import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinStatus

/**
 * Represents the state of the pin dialog.
 */
sealed class PinDialogState {
    /**
     * Dialog is hidden.
     */
    data object Hidden : PinDialogState()

    /**
     * Dialog is shown for creating a new pin.
     *
     * @property name The name of the POI this pin belongs to
     * @property location The location where the pin will be created
     * @property selectedStatus The currently selected status (default: ALLOWED)
     */
    data class Creating(
        val name: String,
        val location: Location,
        val selectedStatus: PinStatus = PinStatus.ALLOWED,
    ) : PinDialogState()

    /**
     * Dialog is shown for editing an existing pin.
     *
     * @property pin The pin being edited
     * @property selectedStatus The currently selected status
     */
    data class Editing(
        val pin: Pin,
        val selectedStatus: PinStatus = pin.status,
    ) : PinDialogState()
}
