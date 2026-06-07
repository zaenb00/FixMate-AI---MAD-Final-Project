# Keep data model classes intact so Gson/Firestore can (de)serialize them by name.
-keep class com.fixmateai.data.model.** { *; }
-keep class com.fixmateai.data.remote.** { *; }

# Gson specifics.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Retrofit / OkHttp.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions

# Glide.
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

# Firebase models reflection.
-keepclassmembers class * {
  @com.google.firebase.firestore.PropertyName <methods>;
}
