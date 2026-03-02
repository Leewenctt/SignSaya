# Firebase Auth - specific classes needed for proper functionality
-keep class com.google.firebase.auth.FirebaseAuth { *; }
-keep class com.google.firebase.auth.FirebaseUser { *; }
-keep class com.google.firebase.auth.AuthResult { *; }
-keep class com.google.firebase.auth.FirebaseAuthException { *; }
-keep class com.google.firebase.auth.FirebaseAuthWeakPasswordException { *; }
-keep class com.google.firebase.auth.FirebaseAuthInvalidCredentialsException { *; }
-keep class com.google.firebase.auth.FirebaseAuthUserCollisionException { *; }
-keep class com.google.firebase.auth.FirebaseAuthInvalidUserException { *; }
-keep class com.google.firebase.FirebaseNetworkException { *; }

# Keep serialized data classes
-keepclassmembers class com.mcc.signsaya.repository.** {
    *;
}
-keepclassmembers class com.mcc.signsaya.viewmodel.** {
    *;
}

# Keep kotlinx.coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}