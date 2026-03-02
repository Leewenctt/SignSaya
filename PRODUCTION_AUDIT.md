# SignSaya Production Readiness Audit

**Date**: March 2, 2026  
**Scope**: Full codebase audit for production readiness  
**Status**: NOT PRODUCTION READY

---

## Critical Production Blockers

### Security

| Severity | Issue | File | Details |
|----------|-------|------|---------|
| **CRITICAL** | API keys committed to git | `app/google-services.json` | `AIzaSyDs8LgmmlYB9WB5AeR7TdWgIODbtmr6lhs` and OAuth client IDs are exposed in version control. Anyone with repo access can abuse your Firebase quota. |
| **CRITICAL** | SHA-1 certificate hash exposed | `app/google-services.json:21` | `edfa71d0e67bcdb84f5d4c253b819df2c88e1fc4` - combine with stolen keystore for app impersonation |
| **HIGH** | No encrypted storage | Missing | Firebase Auth tokens stored in plain SharedPreferences (default). Use `EncryptedSharedPreferences` |
| **HIGH** | No ProGuard rules | `app/proguard-rules.pro` | Only default comments. Add `-keep` rules for Firebase Auth classes to prevent obfuscation crashes |
| **HIGH** | Obfuscation disabled | `app/build.gradle.kts:27` | `isMinifyEnabled = false` - source code is fully exposed in APK |
| **MEDIUM** | No certificate pinning | Network layer | App accepts any Firebase certificate. MITM attacks possible on compromised networks |
| **MEDIUM** | Basic email regex | `SignUpViewModel.kt:198` | `^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$` - doesn't catch all invalid emails. Use `Patterns.EMAIL_ADDRESS` |
| **MEDIUM** | `android:allowBackup` not explicitly set | `AndroidManifest.xml` | If true (default), user data backed up to Google cloud unencrypted |

### Stability & Reliability

| Severity | Issue | File | Details |
|----------|-------|------|---------|
| **CRITICAL** | Infinite polling | `EmailVerificationViewModel.kt:177` | `while(true)` loop with 5-second delays never terminates except on success/session expiry. Drains battery, wastes bandwidth. |
| **CRITICAL** | No process death handling | ViewModels | No `SavedStateHandle` usage. Form data lost on process death (low memory killer, permission changes) |
| **HIGH** | No network state monitoring | `EmailVerificationViewModel` | Polling continues offline, showing errors. Should pause/resume based on connectivity |
| **HIGH** | No app lifecycle awareness | `EmailVerificationViewModel.kt` | Polling runs in background when app minimized. Wastes resources. |
| **HIGH** | Coroutine leak risk | `EmailVerificationViewModel.kt:55-56` | Jobs stored in variables but `onCleared()` cancels them - good. But `screenEntered` flag prevents restart after config change. |
| **MEDIUM** | Multiple FirebaseAuth instances | `SignUpViewModel.kt:41`, `EmailVerificationViewModel.kt:48` | Each ViewModel creates new `AuthRepository()` which calls `FirebaseAuth.getInstance()`. Singleton so harmless but architecturally wrong. |
| **MEDIUM** | No debouncing on input | `SignUpViewModel.kt:51-79` | Every keystroke triggers state update + recomposition. Use `Flow.debounce(300)` |
| **MEDIUM** | No rate limiting on buttons | `SignUpScreen.kt` | Rapid taps can trigger multiple Firebase calls. Add throttle (1-second guard) |

### Missing Core Features

| Severity | Feature | Impact |
|----------|---------|--------|
| **CRITICAL** | Login implementation | `LoginScreen.kt` has 2 lines (package only). Returning users completely blocked. |
| **CRITICAL** | Logout functionality | `ProfileScreen.kt` is empty stub. Users can't sign out. |
| **CRITICAL** | Google Sign-In | Button visible but `TODO` comment. Users expect it to work. |
| **HIGH** | Forgot password | No UI, no ViewModel method, no repository function. |
| **HIGH** | Change email during verification | User stuck if they typo their email. No escape hatch. |
| **HIGH** | Auth state persistence | No `DataStore` or even plain persistence. User re-authenticates every cold start. |
| **MEDIUM** | "Check spam folder" hint | Users miss verification emails, blame app. |
| **MEDIUM** | Guest data migration | Guest users lose data when they finally sign up. |

---

## Design Flaws

### Architecture Issues

```kotlin
// FLAW: ViewModels create their own dependencies
class SignUpViewModel(
    private val repository: AuthRepository = AuthRepository()  // Hardcoded instantiation
) : ViewModel()
```

**Problems**:
1. Can't mock repository for unit tests
2. Multiple repository instances (FirebaseAuth is singleton, but still wrong)
3. No way to inject `SavedStateHandle` for process death survival
4. No way to inject `CoroutineDispatcher` for testing

**Fix**: Add Hilt
```kotlin
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel()
```

### State Management Issues

| Issue | Location | Problem |
|-------|----------|---------|
| Navigation as nullable state | `SignUpUiState.navigateToVerification: String?` | Pattern is correct but easy to forget `onNavigationHandled()`. Consider `Channel` or `SharedFlow` for one-time events. |
| Mixed concerns in UiState | `VerificationUiState` | Network error boolean + banner error string. Should be sealed class `ErrorState`. |
| Magic numbers | `EmailVerificationViewModel.kt:20-21` | `RESEND_COOLDOWN = 60`, `POLL_INTERVAL_MS = 5_000L`. Extract to constants object or use `BuildConfig` for remote config. |
| `bannerError` is String | `SignUpUiState.kt:27` | Not type-safe. Use sealed class `BannerMessage` with `Error` and `Info` subtypes. |

### Repository Design Flaws

```kotlin
// FLAW: Repository knows about UI error messages
AuthResult.Error.InvalidInput(
    message = "Your password isn't strong enough. Use at least 8 characters...",  // UI text in data layer!
    cause = AuthErrorCause.PASSWORD
)
```

**Problem**: Repository shouldn't format user-facing strings. Return error codes, let UI layer localize.

**Fix**:
```kotlin
sealed class ValidationError {
    object WeakPassword : ValidationError()
    object InvalidEmailFormat : ValidationError()
    // UI layer maps to R.string.weak_password
}
```

### Navigation Design Flaws

```kotlin
// FLAW: Screen sealed class mixes navigation with UI resources
sealed class Screen(val route: String, val title: String, val icon: Int)  // icon is @DrawableRes

// Problem: Auth screens have empty title and 0 icon - awkward
object Welcome : Screen("welcome", "", 0)
object Login : Screen("login", "", 0)  // Unused but defined
```

**Fix**: Separate navigation graph from UI model
```kotlin
// Navigation
sealed class Route(val path: String) {
    object Home : Route("home")
    object SignUp : Route("signup")
}

// UI Model (different file)
data class BottomNavItem(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val route: String
)
```

---

## Redundant & Useless Code

### Completely Unused

| File/Code | Lines | Evidence |
|-----------|-------|----------|
| `LoginScreen.kt` | 2 | Only `package com.mcc.signsaya.screens.auth`. No composable function. |
| `Screen.Login` | 1 | Defined in sealed class but `LoginScreen` empty, navigation callback goes nowhere functional. |
| `AuthErrorCause.UNKNOWN` | 1 enum value | Only used as fallback for Google errors when `EMAIL`/`PASSWORD` don't fit. Never displayed to specific field. |
| `Screen.EmailVerification.title` | " | Empty string, never used in scaffold (auth screens hide bottom bar). |
| `Screen.EmailVerification.icon` | 0 | Never used (0 is invalid resource ID). |

### Effectively Unused / Dead Code

| Code | Location | Why It's Useless |
|------|----------|------------------|
| Google Sign-In button click | `SignUpScreen.kt:260` | `onClick = { /* TODO */ }`. Button renders but does nothing. Worse than removing it. |
| `isGoogleSigningIn` state | `SignUpUiState.kt:30`, `SignUpScreen.kt` | Tracked but never triggered since Google Sign-In not implemented. |
| `submitGoogleSignIn()` method | `SignUpViewModel.kt:147-179` | Called nowhere. 32 lines of dead code. |
| Email parameter in EmailVerification | `SignSayaApp.kt:92` | `arguments?.getString("email") ?: ""` - falls back to empty. No validation email was actually passed. |
| `enableEdgeToEdge()` | `MainActivity.kt:12` | Called but no `WindowInsets` handling in screens. Content drawn under status bar without padding. |

### Partially Used / Wasteful

| Code | Location | Issue |
|------|----------|-------|
| `SignUpTextField` composable | `SignUpScreen.kt:320-372` | Good abstraction, but only used in this file. Could be app-wide component but isn't. |
| `GhostButton` scale animation | `Buttons.kt:69-79` | 12 lines for 0.98x scale + 0.6 alpha on press. Over-engineered for simple text button. |
| `Color.darker()` extension | `Buttons.kt:155-160` | Used only for shadow effect. Could use `MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)` |
| Inter font family (4 weights) | `Type.kt:11-16` | Only `Normal`, `SemiBold`, `Bold` seem used in practice. `Medium` may be unused. |
| Full Typography definition | `Type.kt:18-103` | 86 lines defining all 11 text styles. App likely uses 5-6 in practice. |
| `NoRippleInteractionSource` | `BottomBar.kt:65` | Used only once. Standard `MutableInteractionSource()` with `indication = null` would suffice. |

---

## Beneficial Alterations

### High Impact

#### 1. Replace Polling with Push

**Current**: Poll every 5 seconds forever
```kotlin
while (true) {
    delay(POLL_INTERVAL_MS)
    handleVerificationResult(repository.checkEmailVerified())
}
```

**Better**: Firebase Dynamic Link or Cloud Function trigger
- User clicks email link → opens app with deep link → instant verification
- No battery drain, no server load, better UX

#### 2. Add Dependency Injection

**Without Hilt**:
- Can't unit test ViewModels (hardcoded repositories)
- Can't inject dispatchers for testing
- Can't use `SavedStateHandle` properly
- Manual dependency graph management

**With Hilt**:
- Testable architecture
- Process death handling via `SavedStateHandle`
- Scoped dependencies (Activity, ViewModel, Singleton)
- Generated code at compile time (no reflection)

#### 3. Add DataStore for Auth Persistence

**Current**: No persistence
```kotlin
// Every cold start:
val user = FirebaseAuth.getInstance().currentUser  // Null until Firebase re-authenticates
```

**Better**: Store minimal auth state
```kotlin
// dataStore
val lastEmail: String?  // For "Welcome back" UX
val authProvider: String?  // "google" | "email" | null
val biometricEnabled: Boolean
```

#### 4. Implement Modern Google Sign-In

**Current**: Using deprecated Google Sign-In SDK pattern (implied by TODO)

**Better**: Credential Manager (Android 14+ recommended)
```kotlin
val credentialManager = CredentialManager.create(context)
val googleIdOption = GetGoogleIdOption.Builder()
    .setServerClientId(WEB_CLIENT_ID)
    .setNonce(generateNonce())  // Security!
    .build()
```

### Medium Impact

#### 5. Add Input Debouncing

```kotlin
// In ViewModel
private val emailQuery = MutableStateFlow("")

init {
    emailQuery
        .debounce(300)
        .distinctUntilChanged()
        .map { validateEmail(it) }
        .onEach { error -> _state.update { it.copy(emailError = error) } }
        .launchIn(viewModelScope)
}
```

#### 6. Extract All Strings

**Current**: 50+ hardcoded strings in screens
**Better**: Centralized `strings.xml` for localization

#### 7. Add Exponential Backoff for Polling

```kotlin
private var pollInterval = 5_000L
private var consecutiveErrors = 0

private fun startPolling() {
    pollJob = viewModelScope.launch {
        while (true) {
            delay(pollInterval)
            // ... check
            if (result is Error) {
                consecutiveErrors++
                pollInterval = min(pollInterval * 2, 30_000L)  // Cap at 30s
            } else {
                consecutiveErrors = 0
                pollInterval = 5_000L
            }
        }
    }
}
```

#### 8. Add Biometric Authentication

```kotlin
val biometricPrompt = BiometricPrompt(
    activity,
    executor,
    object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: AuthenticationResult) {
            // Auto-login with stored credentials
        }
    }
)
```

### Low Impact / Polish

#### 9. Replace `while(true)` with `Flow`

```kotlin
fun verificationStatus(): Flow<VerificationResult> = flow {
    while (currentCoroutineContext().isActive) {
        emit(repository.checkEmailVerified())
        delay(POLL_INTERVAL_MS)
    }
}.retry { e ->
    e is NetworkError
}
```

#### 10. Add Rate Limiting UI

```kotlin
private var lastSubmitTime = 0L

fun submitSignUp() {
    if (System.currentTimeMillis() - lastSubmitTime < 2000) return
    lastSubmitTime = System.currentTimeMillis()
    // ... proceed
}
```

---

## Optimizations

### Build / Binary Size

| Current | Optimized | Savings |
|-----------|-----------|---------|
| `isMinifyEnabled = false` | `isMinifyEnabled = true` | 20-60% APK size reduction |
| `shrinkResources` not set | `shrinkResources = true` | Removes unused drawables/layouts |
| No ProGuard rules | Proper `-keep` rules | Smaller DEX, faster load times |
| Debug Firebase logging enabled | `FirebaseAuth.getInstance().setLogLevel(ERROR)` in release | Reduced log overhead |

### Runtime Performance

| Area | Current | Optimized |
|------|---------|-----------|
| Recomposition | Every keystroke updates entire form | Use `derivedStateOf` for validation, skip unchanged fields |
| Polling | 5s fixed, forever | Adaptive interval: 5s → 10s → 30s → pause when backgrounded |
| Image loading | `painterResource(R.drawable.ic_logo)` - loads every recomposition | Use `remember` or Coil for caching |
| Font loading | 4 TTF files (1.3MB total) | Use downloadable fonts or variable font |
| Animation | `animateFloatAsState` on every GhostButton press | Use `Modifier.pointerInput` with less allocation |

### Network

| Current | Optimized |
|-----------|-----------|
| Polls even offline | Check `ConnectivityManager` before API calls |
| No request caching | Cache user profile in Room/DataStore |
| Firebase Auth every cold start | Verify local token first, refresh only if needed |

---

## Anything That Stops Production Readiness

### Immediate Blockers (Fix Before Any Release)

1. **Remove `google-services.json` from git**
   ```bash
   git rm --cached app/google-services.json
   echo "app/google-services.json" >> .gitignore
   git commit -m "Remove API keys from repository"
   # Rotate credentials in Firebase Console immediately
   ```

2. **Enable ProGuard**
   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = true
           isShrinkResources = true
           proguardFiles(...)
       }
   }
   ```

3. **Add ProGuard rules**
   ```proguard
   -keep class com.google.firebase.auth.** { *; }
   -keepclassmembers class com.mcc.signsaya.viewmodel.** { *; }
   ```

4. **Fix infinite polling**
   - Add app lifecycle observer
   - Add network state observer
   - Add exponential backoff
   - Cap total polling duration (e.g., 10 minutes max)

5. **Implement LoginScreen or remove it**
   - Empty file crashes on navigation if called
   - Remove from `Screen` sealed class if not implemented

6. **Add logout to ProfileScreen**
   - Minimum: `IconButton` with `AuthRepository.signOut()`
   - Navigate to Welcome screen after logout

### Beta Blockers (Fix Before Public Beta)

7. **Add dependency injection (Hilt)**
8. **Add DataStore for persistence**
9. **Complete Google Sign-In**
10. **Add forgot password flow**
11. **Add process death handling (`SavedStateHandle`)**
12. **Add comprehensive unit tests**
13. **Extract all strings for localization**
14. **Add accessibility labels**

### Production Blockers (Fix Before Store Release)

15. **Add encrypted storage for tokens**
16. **Add certificate pinning**
17. **Implement deep link email verification**
18. **Add analytics/monitoring (Crashlytics)**
19. **Add rate limiting for API abuse prevention**
20. **Security audit with static analysis (MobSF, etc.)**

---

## Recommended Priority Order

### Week 1: Security & Stability
- Remove API keys from git
- Enable ProGuard
- Fix infinite polling (add lifecycle awareness)
- Remove empty LoginScreen or implement it

### Week 2: Core Features
- Implement LoginScreen + ViewModel
- Add logout functionality
- Add DataStore for persistence
- Add Hilt for DI

### Week 3: Polish & Testing
- Complete Google Sign-In
- Add forgot password
- Extract strings
- Unit tests
- Accessibility pass

### Week 4: Production Hardening
- Encrypted storage
- Certificate pinning
- Deep links
- Analytics
- Final security review

---

## Files Requiring Immediate Attention

| File | Action Required |
|------|-----------------|
| `app/google-services.json` | **DELETE FROM GIT IMMEDIATELY** |
| `app/build.gradle.kts` | Enable `isMinifyEnabled`, `isShrinkResources` |
| `app/proguard-rules.pro` | Add Firebase Auth keep rules |
| `app/src/main/java/.../LoginScreen.kt` | Implement or delete |
| `app/src/main/java/.../EmailVerificationViewModel.kt` | Fix infinite polling |
| `app/src/main/java/.../ProfileScreen.kt` | Add logout button |
| `app/src/main/java/.../SignUpScreen.kt:260` | Remove TODO or implement Google Sign-In |

---

**Verdict**: The app has good architectural bones but lacks critical production requirements. Fix the CRITICAL and HIGH items before any release. The committed API keys are an immediate security incident requiring credential rotation.
