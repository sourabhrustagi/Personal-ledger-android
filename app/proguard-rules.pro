# ProGuard rules for Personal Ledger

# Keep domain models and DTOs for Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses,Signature
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static final ** INSTANCE;
}
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.** { *; }

# Retrofit & OkHttp
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Hilt & Dagger
-keepclassmembers class * {
    @javax.inject.* *;
    @dagger.* *;
}

# Timber (remove debug logging calls in release)
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
}
