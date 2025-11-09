package com.carryzonemap.app.domain.model

/**
 * Represents the reason why firearms carry is restricted at a location.
 * Only applicable for pins with status NO_GUN.
 */
enum class RestrictionTag(
    val displayName: String,
    val description: String,
) {
    /**
     * Federal government building, post office, military base, VA facility, courthouse, or tribal land
     */
    FEDERAL_PROPERTY(
        displayName = "Federal Government Property",
        description = "Federal building, post office, military base, VA facility, courthouse, or tribal land",
    ),

    /**
     * Airport secure area past TSA security checkpoint
     */
    AIRPORT_SECURE(
        displayName = "Airport Secure Area",
        description = "Past TSA security checkpoint",
    ),

    /**
     * State/local government building, courthouse, or polling place
     */
    STATE_LOCAL_GOVT(
        displayName = "State/Local Government Property",
        description = "State/local government building, courthouse, or polling place",
    ),

    /**
     * Elementary, middle, or high school campus
     */
    SCHOOL_K12(
        displayName = "School (K-12)",
        description = "Elementary, middle, or high school campus",
    ),

    /**
     * College or university campus
     */
    COLLEGE_UNIVERSITY(
        displayName = "College/University",
        description = "College or university campus",
    ),

    /**
     * Bar, restaurant, or venue with alcohol restrictions
     */
    BAR_ALCOHOL(
        displayName = "Bar/Alcohol Establishment",
        description = "Bar, restaurant, or venue with alcohol restrictions",
    ),

    /**
     * Hospital, medical clinic, or childcare facility
     */
    HEALTHCARE(
        displayName = "Healthcare Facility",
        description = "Hospital, medical clinic, or childcare facility",
    ),

    /**
     * Church, mosque, temple, or religious facility
     */
    PLACE_OF_WORSHIP(
        displayName = "Place of Worship",
        description = "Church, mosque, temple, or religious facility",
    ),

    /**
     * Sports stadium, arena, concert hall, or amusement park
     */
    SPORTS_ENTERTAINMENT(
        displayName = "Sports/Entertainment Venue",
        description = "Sports stadium, arena, concert hall, or amusement park",
    ),

    /**
     * Private business, workplace, or property restricting carry
     */
    PRIVATE_PROPERTY(
        displayName = "Private Property",
        description = "Private business, workplace, or property restricting carry",
    ),
    ;

    companion object {
        /**
         * Converts a string name to RestrictionTag
         * Returns null if the string doesn't match any tag
         */
        fun fromString(name: String?): RestrictionTag? {
            if (name == null) return null
            return entries.find { it.name == name }
        }
    }
}
