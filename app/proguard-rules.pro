# Keep kotlinx.serialization generated serializers for our @Serializable models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.etyhan.app.**$$serializer { *; }
-keepclassmembers class com.etyhan.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.etyhan.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
