# R8 runs with full optimization (no -dontoptimize). Every library used here - kotlinx.serialization,
# Room, Hilt/Dagger, WorkManager, DataStore, AndroidX Startup, Compose - ships its own consumer
# rules, so nothing library-wide is kept here; broad "-keep class x.** { *; }" rules stop R8 from
# shrinking and optimizing and make Play Console flag the release. Only what the app itself
# reaches by name stays below.

# kotlinx.serialization: the plugin generates the serializers and the library's own rules keep
# them. These narrow app-package rules are a safety net for the polymorphic ExerciseContent tree
# and the backup models decoded from JSON (assets, backups, stored content blobs).
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, InnerClasses, Signature
-keepclassmembers @kotlinx.serialization.Serializable class com.quranicwords.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.quranicwords.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.quranicwords.app.**
-keep,allowoptimization class <1>$$serializer { *; }

# WorkManager stores a worker's class name in its database and re-creates it by that name after
# an app update, so worker names must survive obfuscation (constructors are kept by WorkManager).
-keepnames class * extends androidx.work.ListenableWorker

-dontwarn kotlinx.coroutines.**
