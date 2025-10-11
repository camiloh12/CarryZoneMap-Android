package com.carryzonemap.app.domain.model

/**
 * Represents the carry zone status for a location pin.
 */
enum class PinStatus(val displayName: String, val colorCode: Int) {
    /**
     * Firearms are allowed at this location (Green)
     */
    ALLOWED("Allowed", 0),

    /**
     * Status is uncertain or unverified (Yellow)
     */
    UNCERTAIN("Uncertain", 1),

    /**
     * Firearms are not allowed at this location (Red)
     */
    NO_GUN("No Guns", 2),
    ;

    /**
     * Returns the next status in the cycle: ALLOWED -> UNCERTAIN -> NO_GUN -> ALLOWED
     */
    fun next(): PinStatus =
        when (this) {
            ALLOWED -> UNCERTAIN
            UNCERTAIN -> NO_GUN
            NO_GUN -> ALLOWED
        }

    companion object {
        /**
         * Converts a color code integer to PinStatus
         */
        fun fromColorCode(code: Int): PinStatus =
            when (code) {
                0 -> ALLOWED
                1 -> UNCERTAIN
                2 -> NO_GUN
                else -> ALLOWED // Default fallback
            }
    }
}
