# ProGuard rules for ProjectU

# Keep Kotlin Metadata
-keep class kotlin.Metadata { *; }

# Keep Koin
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.projectu.**$$serializer { *; }
-keepclassmembers class com.projectu.** {
    *** Companion;
}
-keepclasseswithmembers class com.projectu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# Keep Voyager
-keep class cafe.adriel.voyager.** { *; }

# Keep data classes
-keep class com.projectu.shared.domain.model.** { *; }
-keep class com.projectu.shared.data.remote.dto.** { *; }

# General
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
