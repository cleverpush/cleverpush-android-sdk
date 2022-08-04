-keep class com.cleverpush.** { *; }
-dontwarn com.cleverpush.**

# Firebase
-keep class com.google.firebase.messaging.FirebaseMessaging { *; }
-keep class com.google.firebase.iid.FirebaseInstanceId { *; }
-keep class com.google.firebase.iid.FirebaseInstanceIdService { *; }

# Google Mobile Services
-keep class com.google.android.gms.common.api.GoogleApiClient { *; }

# Huawei
-keep class com.huawei.hms.api.HuaweiApiAvailability { *; }
-keep class com.huawei.hms.aaid.HmsInstanceId { *; }
-keep class com.huawei.agconnect.config.AGConnectServicesConfig { *; }
-dontwarn com.huawei.**

# Amazon
-keep class com.amazon.device.messaging.ADM { *; }
-dontwarn com.amazon.**
