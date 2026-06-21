-keep class com.connectchat.data.api.model.** { *; }
-keep class com.connectchat.data.local.entity.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
