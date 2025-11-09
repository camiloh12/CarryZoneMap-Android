# ProGuard rules for CCW Map Release Build

# ===== MapLibre =====
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# ===== Kotlinx Serialization =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.carryzonemap.app.**$$serializer { *; }
-keepclassmembers class com.carryzonemap.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.carryzonemap.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===== Ktor Client =====
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**

# ===== Supabase =====
-keep class io.github.jan.supabase.** { *; }
-keep class * extends io.github.jan.supabase.SupabaseClient { *; }
-keepclassmembers class * {
    @io.github.jan.supabase.annotations.SupabaseInternal *;
}

# ===== Room Database =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ===== Hilt/Dagger =====
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.android.** { *; }
-keep class dagger.hilt.internal.** { *; }

# ===== Jetpack Compose =====
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.compose.**

# ===== Kotlin Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ===== Timber Logging =====
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.** { *; }

# ===== Data Classes =====
# Keep domain models
-keep class com.carryzonemap.app.domain.model.** { *; }
# Keep DTOs
-keep class com.carryzonemap.app.data.remote.dto.** { *; }
# Keep entities
-keep class com.carryzonemap.app.data.local.entity.** { *; }

# ===== General Android =====
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keep class * extends java.lang.Exception

# ===== Gson (if used) =====
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer