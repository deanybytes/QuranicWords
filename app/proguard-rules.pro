# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt

# Keep all serialization companion & serializer classes (including $serializer & $$serializer)
-keepclassmembers class com.quranicwords.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.quranicwords.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.quranicwords.app.**$serializer {
    *;
}
-keep class com.quranicwords.app.**$$serializer {
    *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep domain models, entities, and asset files
-keep class com.quranicwords.app.core.domain.model.** { *; }
-keep class com.quranicwords.app.core.data.local.entity.** { *; }
-keep class com.quranicwords.app.core.data.assets.** { *; }
-keep class com.quranicwords.app.feature.widget.** { *; }

# Keep Room DAOs and Databases
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep ViewModels and Application
-keep class com.quranicwords.app.QwApplication { *; }
-keep class com.quranicwords.app.MainActivity { *; }
