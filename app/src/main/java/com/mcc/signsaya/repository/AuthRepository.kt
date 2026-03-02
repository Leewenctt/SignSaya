package com.mcc.signsaya.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

// ---------------------------------------------------------------------------
// Result contract
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Repository
// ---------------------------------------------------------------------------

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // --- Sign up ---
    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        require(email.isNotBlank()) { "Email must not be blank" }
        require(password.isNotBlank()) { "Password must not be blank" }

        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.sendEmailVerification()?.await()
            AuthResult.Success
        } catch (_: FirebaseAuthWeakPasswordException) {
            AuthResult.Error.InvalidInput(
                message = "Your password isn't strong enough. Use at least 8 characters, " +
                        "including one uppercase letter and one number.",
                cause = AuthErrorCause.PASSWORD
            )
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error.InvalidInput(
                message = "That doesn't look like a valid email address. " +
                        "Please double-check and try again.",
                cause = AuthErrorCause.EMAIL
            )
        } catch (_: FirebaseAuthUserCollisionException) {
            AuthResult.Error.Conflict(
                "An account with this email already exists. " +
                        "Try logging in instead, or use a different email."
            )
        } catch (_: FirebaseNetworkException) {
            AuthResult.Error.Network(
                "We couldn't connect to the server. " +
                        "Check your internet connection and try again."
            )
        } catch (e: FirebaseAuthException) {
            e.toUnexpectedError()
        } catch (_: Exception) {
            AuthResult.Error.Unexpected(
                "Something went wrong on our end. Please try again in a moment."
            )
        }
    }

    // --- Email verification ---
    suspend fun resendVerificationEmail(): AuthResult {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
                ?: return AuthResult.Error.Unexpected(
                    "No signed-in account found. Please sign up again."
                )
            AuthResult.Success
        } catch (_: FirebaseNetworkException) {
            AuthResult.Error.Network(
                "We couldn't send the email — check your internet connection and try again."
            )
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_TOO_MANY_REQUESTS" -> AuthResult.Error.TooManyRequests(
                    "You've requested too many verification emails. " +
                            "Please wait a few minutes before trying again."
                )
                else -> e.toUnexpectedError()
            }
        } catch (_: Exception) {
            AuthResult.Error.Unexpected(
                "We couldn't send the verification email. Please try again in a moment."
            )
        }
    }

    suspend fun checkEmailVerified(): VerificationResult {
        val user = auth.currentUser
            ?: return VerificationResult.SessionExpired

        return try {
            user.reload().await()
            if (user.isEmailVerified) {
                VerificationResult.Verified
            } else {
                VerificationResult.NotYetVerified
            }
        } catch (_: FirebaseNetworkException) {
            VerificationResult.NetworkError
        } catch (_: FirebaseAuthInvalidUserException) {
            VerificationResult.SessionExpired
        } catch (_: Exception) {
            VerificationResult.Unexpected(
                "We couldn't check your verification status. Please try again."
            )
        }
    }

    // --- Session ---

    fun signOut() {
        auth.signOut()
    }

    val currentUser get() = auth.currentUser

}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun FirebaseAuthException.toUnexpectedError(): AuthResult.Error.Unexpected =
    AuthResult.Error.Unexpected(
        "Something went wrong (code: $errorCode). Please try again or contact support."
    )