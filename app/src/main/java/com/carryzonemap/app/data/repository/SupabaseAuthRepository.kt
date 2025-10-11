package com.carryzonemap.app.data.repository

import android.util.Log
import com.carryzonemap.app.domain.model.User
import com.carryzonemap.app.domain.repository.AuthRepository
import com.carryzonemap.app.domain.repository.AuthState
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

        private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
        override val authState: Flow<AuthState> = _authState.asStateFlow()

        override val currentUserId: String?
            get() = auth.currentUserOrNull()?.id

        init {
            // Initialize auth state based on current session
            updateAuthState()
        }

        override suspend fun signUpWithEmail(
            email: String,
            password: String,
        ): Result<User> {
            return try {
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
                    _authState.value = AuthState.Authenticated(user)
                    Log.d(TAG, "Sign up successful: ${user.email}")
                    Result.success(user)
                } else {
                    val error = Exception("Sign up succeeded but user is null")
                    Log.e(TAG, "Sign up error", error)
                    Result.failure(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign up failed", e)
                Result.failure(e)
            }
        }

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<User> {
            return try {
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
                    _authState.value = AuthState.Authenticated(user)
                    Log.d(TAG, "Sign in successful: ${user.email}")
                    Result.success(user)
                } else {
                    val error = Exception("Sign in succeeded but user is null")
                    Log.e(TAG, "Sign in error", error)
                    Result.failure(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed", e)
                Result.failure(e)
            }
        }

        override suspend fun signOut(): Result<Unit> {
            return try {
                auth.signOut()
                _authState.value = AuthState.Unauthenticated
                Log.d(TAG, "Sign out successful")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed", e)
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

        /**
         * Updates the auth state based on current session.
         */
        private fun updateAuthState() {
            val currentUser = auth.currentUserOrNull()
            _authState.value =
                if (currentUser != null) {
                    AuthState.Authenticated(
                        User(
                            id = currentUser.id,
                            email = currentUser.email,
                        ),
                    )
                } else {
                    AuthState.Unauthenticated
                }
        }
    }
