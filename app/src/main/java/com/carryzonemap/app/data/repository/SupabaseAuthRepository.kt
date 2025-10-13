package com.carryzonemap.app.data.repository

import android.util.Log
import com.carryzonemap.app.domain.model.User
import com.carryzonemap.app.domain.repository.AuthRepository
import com.carryzonemap.app.domain.repository.AuthState
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase implementation of AuthRepository.
 *
 * Manages user authentication using Supabase Auth with email/password.
 *
 * @property auth Supabase Auth client
 */
@Singleton
class SupabaseAuthRepository
    @Inject
    constructor(
        private val auth: Auth,
    ) : AuthRepository {
        companion object {
            private const val TAG = "SupabaseAuthRepository"
        }

        // Scope for managing coroutines in this repository
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
        override val authState: Flow<AuthState> = _authState.asStateFlow()

        override val currentUserId: String?
            get() = auth.currentUserOrNull()?.id

        init {
            Log.d(TAG, "Initializing SupabaseAuthRepository")
            // Observe session status changes for automatic state updates
            scope.launch {
                auth.sessionStatus.collect { status ->
                    Log.d(TAG, "Session status changed: $status")
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val currentUser = auth.currentUserOrNull()
                            if (currentUser != null) {
                                val user =
                                    User(
                                        id = currentUser.id,
                                        email = currentUser.email,
                                    )
                                _authState.value = AuthState.Authenticated(user)
                                Log.d(TAG, "User authenticated from session: ${user.email}")
                            }
                        }
                        is SessionStatus.Initializing -> {
                            Log.d(TAG, "Initializing session...")
                            _authState.value = AuthState.Loading
                        }
                        is SessionStatus.RefreshFailure -> {
                            Log.w(TAG, "Session refresh failed, keeping current state")
                            // Don't change state on refresh failure - keep user logged in
                            // Session will retry on next network availability
                        }
                        is SessionStatus.NotAuthenticated -> {
                            Log.d(TAG, "User not authenticated")
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                }
            }
        }

        override suspend fun signUpWithEmail(
            email: String,
            password: String,
        ): Result<User> {
            return try {
                Log.d(TAG, "Attempting sign up with email: $email")
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val currentUser = auth.currentUserOrNull()
                if (currentUser != null) {
                    val user =
                        User(
                            id = currentUser.id,
                            email = currentUser.email,
                        )
                    // Note: authState will be updated by session status collector
                    Log.d(TAG, "Sign up successful: ${user.email}, session will be persisted")
                    Result.success(user)
                } else {
                    val error = Exception("Sign up succeeded but user is null")
                    Log.e(TAG, "Sign up error", error)
                    Result.failure(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign up failed: ${e.message}", e)
                Result.failure(e)
            }
        }

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<User> {
            return try {
                Log.d(TAG, "Attempting sign in with email: $email")
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val currentUser = auth.currentUserOrNull()
                if (currentUser != null) {
                    val user =
                        User(
                            id = currentUser.id,
                            email = currentUser.email,
                        )
                    // Note: authState will be updated by session status collector
                    Log.d(TAG, "Sign in successful: ${user.email}, session will be persisted")
                    Result.success(user)
                } else {
                    val error = Exception("Sign in succeeded but user is null")
                    Log.e(TAG, "Sign in error", error)
                    Result.failure(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed: ${e.message}", e)
                Result.failure(e)
            }
        }

        override suspend fun signOut(): Result<Unit> {
            return try {
                Log.d(TAG, "Signing out and clearing session...")
                auth.signOut()
                // Note: authState will be updated by session status collector
                Log.d(TAG, "Sign out successful, session cleared")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed: ${e.message}", e)
                Result.failure(e)
            }
        }

        override suspend fun getCurrentUser(): User? {
            val currentUser = auth.currentUserOrNull()
            return currentUser?.let {
                User(
                    id = it.id,
                    email = it.email,
                )
            }
        }

    }
