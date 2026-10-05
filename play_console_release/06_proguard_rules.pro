# ==============================================================================
# ProGuard & R8 Rules for Universal TV Remote Control (Flutter)
# ==============================================================================

# 1. Flutter Engine & Platform Channels
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }
-dontwarn io.flutter.embedding.**

# 2. Consumer IR Hardware & System Services
-keep class android.hardware.ConsumerIrManager { *; }
-keepclassmembers class * extends android.hardware.ConsumerIrManager {
    public *;
}

# 3. Flutter Blue Plus (BLE Hardware Access)
-keep class com.boskokg.flutter_blue_plus.** { *; }
-keepclassmembers class com.boskokg.flutter_blue_plus.** { *; }
-dontwarn com.boskokg.flutter_blue_plus.**

# 4. IR Sensor Plugin / Method Channels
-keep class com.arnon.** { *; }
-keep class com.remote.ir_sensor.** { *; }

# 5. Firebase & Google Play Services
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# 6. JSON Data Models & Deserializers
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.omnitech.universal.tv.remote.models.** { *; }

# 7. Network / WebSocket / SSDP
-keep class java.net.MulticastSocket { *; }
-keep class java.net.DatagramPacket { *; }
