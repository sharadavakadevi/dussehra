# Proguard rules for Dussehra Photo Organizer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.dussehraphotoorganizer.model.** { *; }
