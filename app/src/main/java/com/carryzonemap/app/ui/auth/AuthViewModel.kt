package com.carryzonemap.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carryzonemap.app.domain.repository.AuthRepository
import com.carryzonemap.app.domain.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens.
 *
 * Manages authentication state and user sign-in/sign-up operations.
 */
@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        val authState: StateFlow<AuthState> =
            authRepository.authState as? StateFlow<AuthState>
                ?: MutableStateFlow(AuthState.Loading)

        companion object {
            private const val MIN_PASSWORD_LENGTH = 6
        }

        /**
         * Signs in a user with email and password.
         */
        fun signIn(
            email: String,
            password: String,
        ) {
            if (!validateInput(email, password)) {
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

                authRepository.signInWithEmail(email, password)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                successMessage = null,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Sign in failed: ${error.message}",
                                successMessage = null,
                            )
                        }
                    }
            }
        }

        /**
         * Signs up a new user with email and password.
         */
        fun signUp(
            email: String,
            password: String,
        ) {
            if (!validateInput(email, password)) {
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

                authRepository.signUpWithEmail(email, password)
                    .onSuccess { user ->
                        // Check if email confirmation is required (empty ID indicates pending confirmation)
                        val message =
                            if (user.id.isEmpty()) {
                                "Account created! Please check your email to confirm your account before signing in."
                            } else {
                                null // Immediate login, no message needed
                            }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                successMessage = message,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Sign up failed: ${error.message}",
                                successMessage = null,
                            )
                        }
                    }
            }
        }

        /**
         * Clears any error or success messages.
         */
        fun clearError() {
            _uiState.update { it.copy(error = null, successMessage = null) }
        }

        private fun validateInput(
            email: String,
            password: String,
        ): Boolean {
            val errorMessage = getValidationError(email, password)

            return if (errorMessage != null) {
                _uiState.update { it.copy(error = errorMessage) }
                false
            } else {
                true
            }
        }

        private fun getValidationError(
            email: String,
            password: String,
        ): String? {
            return when {
                email.isBlank() -> "Email is required"
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
                password.isBlank() -> "Password is required"
                password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
                else -> null
            }
        }
    }

/**
 * UI state for authentication screens.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)
