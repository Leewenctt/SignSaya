package com.mcc.signsaya.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.IOException

sealed class AuthResult {
    object Success : AuthResult()
    sealed class Error(open val message: String) : AuthResult() {
        data class Network(override val message: String) : Error(message)
        data class InvalidInput(
            override val message: String,
            val cause: AuthErrorCause = AuthErrorCause.UNKNOWN
        ) : Error(message)
        data class Conflict(override val message: String) : Error(message)
        data class TooManyRequests(override val message: String) : Error(message)
        data class Unexpected(override val message: String) : Error(message)
    }
}

sealed class VerificationResult {
    object Verified : VerificationResult()
    object NotYetVerified : VerificationResult()
    object NetworkError : VerificationResult()
    object SessionExpired : VerificationResult()
    data class Unexpected(val message: String) : VerificationResult()
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private suspend fun <T> withRetry(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 4000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (_: FirebaseNetworkException) {
                // Network error, retry
            } catch (_: IOException) {
                // Network error, retry
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // Last attempt
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Error.InvalidInput(
                message = "Email and password cannot be empty.",
                cause = AuthErrorCause.UNKNOWN
            )
        }

        return try {
            withRetry {
                auth.createUserWithEmailAndPassword(email, password).await()
            }

            
            AuthResult.Success
        } catch (_: FirebaseAuthWeakPasswordException) {
            AuthResult.Error.InvalidInput(
                message = "Invalid password.",
                cause = AuthErrorCause.PASSWORD
            )
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error.InvalidInput(
                message = "Invalid email address.",
                cause = AuthErrorCause.EMAIL
            )
        } catch (_: FirebaseAuthUserCollisionException) {
            AuthResult.Error.Conflict(
                "Email address is already in use."
            )
        } catch (_: FirebaseNetworkException) {
            AuthResult.Error.Network(
                "Network error. No internet connection."
            )
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> AuthResult.Error.TooManyRequests(
                    "Too many attempts. Please try again later."
                )
                else -> toUnexpectedError()
            }
        } catch (_: Exception) {
            AuthResult.Error.Unexpected(
                "Something went wrong. Please try again later."
            )
        }
    }

    suspend fun loginWithEmail(email: String, password: String): AuthResult {
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Error.InvalidInput(
                message = "Email and password cannot be empty.",
                cause = AuthErrorCause.UNKNOWN
            )
        }

        return try {
            withRetry {
                auth.signInWithEmailAndPassword(email, password).await()
            }
            AuthResult.Success
        } catch (_: FirebaseAuthInvalidUserException) {
            AuthResult.Error.InvalidInput(
                message = "Invalid email address.",
                cause = AuthErrorCause.EMAIL
            )
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error.InvalidInput(
                message = "Incorrect email or password.",
                cause = AuthErrorCause.UNKNOWN
            )
        } catch (_: FirebaseNetworkException) {
            AuthResult.Error.Network(
                "Network error. No internet connection."
            )
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> AuthResult.Error.TooManyRequests(
                    "Too many attempts. Please try again later."
                )
                else -> toUnexpectedError()
            }
        } catch (_: Exception) {
            AuthResult.Error.Unexpected(
                "Something went wrong. Please try again later."
            )
        }
    }

    suspend fun loginWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            withRetry {
                auth.signInWithCredential(credential).await()
            }
            AuthResult.Success
        } catch (_: FirebaseAuthUserCollisionException) {
            AuthResult.Error.Conflict("An account already exists with the same email address but different sign-in credentials.")
        } catch (_: FirebaseNetworkException) {
            AuthResult.Error.Network("Network error. No internet connection.")
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> AuthResult.Error.TooManyRequests("Too many attempts. Please try again later.")
                else -> toUnexpectedError()
            }
        } catch (_: Exception) {
            AuthResult.Error.Unexpected("Google sign-in failed. Please try again later.")
        }
    }

    suspend fun resendVerificationEmail(): AuthResult {
        val currentUser = auth.currentUser ?: return AuthResult.Error.Unexpected(
            "Session not found. Please login or sign up."
        )

        return try {
            withRetry {
                currentUser.sendEmailVerification().await()
            }
            AuthResult.Success
        } catch (_: FirebaseNetworkException) {
            AuthResult.Error.Network(
                "Network error. No internet connection."
            )
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> AuthResult.Error.TooManyRequests(
                    "Too many attempts. Please try again later."
                )
                else -> toUnexpectedError()
            }
        } catch (_: Exception) {
            AuthResult.Error.Unexpected(
                "Something went wrong. Please try again later."
            )
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        if (email.isBlank()) {
            return AuthResult.Error.InvalidInput("Email cannot be empty.")
        }
        return try {
            withRetry {
                auth.sendPasswordResetEmail(email).await()
            }
            AuthResult.Success
        } catch (_: FirebaseAuthInvalidUserException) {
            // Email enumeration protection: return success even if user not found
            AuthResult.Success
        } catch (_: FirebaseNetworkException) {
            AuthResult.Error.Network("Network error. No internet connection.")
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> AuthResult.Error.TooManyRequests(
                    "Too many attempts. Please try again later."
                )
                else -> toUnexpectedError()
            }
        } catch (_: Exception) {
            AuthResult.Error.Unexpected("Something went wrong. Please try again later.")
        }
    }

    suspend fun checkEmailVerified(): VerificationResult {
        val user = auth.currentUser
            ?: return VerificationResult.SessionExpired

        return try {
            withRetry {
                user.reload().await()
                if (user.isEmailVerified) {
                    VerificationResult.Verified
                } else {
                    VerificationResult.NotYetVerified
                }
            }
        } catch (_: FirebaseNetworkException) {
            VerificationResult.NetworkError
        } catch (_: FirebaseAuthInvalidUserException) {
            VerificationResult.SessionExpired
        } catch (_: Exception) {
            VerificationResult.Unexpected(
                "Something went wrong. Please try again later."
            )
        }
    }

    fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val currentUser get() = auth.currentUser

    private fun toUnexpectedError(): AuthResult.Error.Unexpected =
        AuthResult.Error.Unexpected(
            "Something went wrong. Please try again later."
        )
}
