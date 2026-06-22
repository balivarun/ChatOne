# API models - Gson needs field names preserved
-keep class com.connectchat.data.api.model.** { *; }
-keepclassmembers class com.connectchat.data.api.model.** { *; }

# Room entities (correct package - no .entity sub-package)
-keep class com.connectchat.data.local.** { *; }

# Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Coil
-dontwarn coil.**

# Keep BuildConfig
-keep class com.connectchat.BuildConfig { *; }
