package com.carryzonemap.app.domain.model

/**
 * Domain model representing an authenticated user.
 *
 * @property id Unique user identifier (UUID from auth provider)
 * @property email User's email address
 */
data class User(
    val id: String,
    val email: String?,
)
