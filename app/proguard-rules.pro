# Add project specific ProGuard rules here.

# -----------------------------------------------
# Room Database - Jangan hapus/rename entity & DAO
# -----------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }

# -----------------------------------------------
# Kotlin
# -----------------------------------------------
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# -----------------------------------------------
# Coroutines & Flow
# -----------------------------------------------
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# -----------------------------------------------
# App entities - lindungi semua data class di package lokal
# -----------------------------------------------
-keep class com.mekarsari.kasir.data.local.entity.** { *; }
-keep class com.mekarsari.kasir.data.local.dao.** { *; }
