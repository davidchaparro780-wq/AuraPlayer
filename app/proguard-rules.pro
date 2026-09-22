# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\...\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt

-keep class com.novaplayer.** { *; }
-keep class com.auraplayer.** { *; }

# NewPipeExtractor & Rhino JS Engine
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-dontwarn javax.annotation.**
-keep class org.schabi.newpipe.extractor.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

