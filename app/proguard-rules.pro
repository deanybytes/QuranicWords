# kotlinx.serialization: keep generated serializer objects and Companion objects for this app's
# @Serializable model classes - most notably ExerciseContent's polymorphic sealed hierarchy,
# which is the one thing in this codebase genuinely dependent on reflection/annotations surviving
# minification (Room and Hilt/Dagger both bundle their own consumer ProGuard rules automatically).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class com.quranicwords.app.**$$serializer {
    *** INSTANCE;
}
-keepclassmembers class com.quranicwords.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.quranicwords.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
