# Keep LuaJ classes (uses reflection internally for stdlib loaders).
-keep class org.luaj.** { *; }
-dontwarn org.luaj.**

# Keep kotlinx serialization companions.
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
