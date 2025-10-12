package com.carryzonemap.app.domain.repository

import com.carryzonemap.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 *
 * Provides methods for user authentication and session management.
 */
interface AuthRepository {
    /**
     * Flow that emits the current authentication state.
     */
    val authState: Flow<AuthState>

    /**
     * Current authenticated user ID, or null if not authenticated.
     */
    val currentUserId: String?

    /**
     * Signs up a new user with email and password.
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing the User or an error
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
    ): Result<User>

    /**
     * Signs in an existing user with email and password.
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing the User or an error
     */
    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<User>

    /**
     * Signs out the current user.
     *
     * @return Result indicating success or error
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Gets the currently authenticated user.
     *
     * @return Current user or null if not authenticated
     */
    suspend fun getCurrentUser(): User?
}

/**
 * Represents the authentication state of the application.
 */
sealed class AuthState {
    /**
     * Authentication state is being loaded/checked.
     */
    data object Loading : AuthState()

    /**
     * User is authenticated.
     */
    data class Authenticated(val user: User) : AuthState()

    /**
     * User is not authenticated.
     */
    data object Unauthenticated : AuthState()
}
