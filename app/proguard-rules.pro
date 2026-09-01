# Disable aggressive code mutation/inlining optimizations that break reflection
-dontoptimize

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
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
    *;
}
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * {
    <init>(...);
    *;
}
-keep class com.quranicwords.app.MainViewModel {
    <init>(...);
    *;
}
-keep class com.quranicwords.app.feature.**.*ViewModel {
    <init>(...);
    *;
}

# Hilt, Dagger, Workers, and Startup
-keep class dagger.hilt.** { *; }
-keep class com.quranicwords.app.**_Factory { *; }
-keep class com.quranicwords.app.**_MembersInjector { *; }
-keep class com.quranicwords.app.**_HiltModules** { *; }
-keep class com.quranicwords.app.core.di.** { *; }
-keep class * extends androidx.hilt.work.HiltWorkerFactory { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.work.** { *; }
-keep class androidx.startup.** { *; }

# DataStore and Coroutines
-keep class androidx.datastore.** { *; }
-dontwarn kotlinx.coroutines.**
