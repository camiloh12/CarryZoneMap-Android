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
                _uiState.update { it.copy(isLoading = true, error = null) }

                authRepository.signInWithEmail(email, password)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Sign in failed: ${error.message}",
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
                _uiState.update { it.copy(isLoading = true, error = null) }

                authRepository.signUpWithEmail(email, password)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Sign up failed: ${error.message}",
                            )
                        }
                    }
            }
        }

        /**
         * Clears any error messages.
         */
        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        private fun validateInput(
            email: String,
            password: String,
        ): Boolean {
            if (email.isBlank()) {
                _uiState.update { it.copy(error = "Email is required") }
                return false
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _uiState.update { it.copy(error = "Invalid email format") }
                return false
            }

            if (password.isBlank()) {
                _uiState.update { it.copy(error = "Password is required") }
                return false
            }

            if (password.length < 6) {
                _uiState.update { it.copy(error = "Password must be at least 6 characters") }
                return false
            }

            return true
        }
    }

/**
 * UI state for authentication screens.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)
