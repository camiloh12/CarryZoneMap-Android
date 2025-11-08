package com.carryzonemap.app.ui.state

import com.carryzonemap.app.domain.model.Location
import com.carryzonemap.app.domain.model.Pin
import com.carryzonemap.app.domain.model.PinStatus
import com.carryzonemap.app.domain.model.RestrictionTag

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
     * @property selectedRestrictionTag The selected restriction tag (required if status is NO_GUN)
     * @property hasSecurityScreening Whether active security screening is present
     * @property hasPostedSignage Whether posted "no guns" signage is visible
     */
    data class Creating(
        val name: String,
        val location: Location,
        val selectedStatus: PinStatus = PinStatus.ALLOWED,
        val selectedRestrictionTag: RestrictionTag? = null,
        val hasSecurityScreening: Boolean = false,
        val hasPostedSignage: Boolean = false,
    ) : PinDialogState()

    /**
     * Dialog is shown for editing an existing pin.
     *
     * @property pin The pin being edited
     * @property selectedStatus The currently selected status
     * @property selectedRestrictionTag The selected restriction tag (required if status is NO_GUN)
     * @property hasSecurityScreening Whether active security screening is present
     * @property hasPostedSignage Whether posted "no guns" signage is visible
     */
    data class Editing(
        val pin: Pin,
        val selectedStatus: PinStatus = pin.status,
        val selectedRestrictionTag: RestrictionTag? = pin.restrictionTag,
        val hasSecurityScreening: Boolean = pin.hasSecurityScreening,
        val hasPostedSignage: Boolean = pin.hasPostedSignage,
    ) : PinDialogState()
}
