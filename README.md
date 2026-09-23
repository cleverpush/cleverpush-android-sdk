# CleverPush Android SDK

## Documentation

https://developers.cleverpush.com/

## Geofencing

`play-services-location` is optional and is not included transitively. Apps that used geofencing via the SDK's previous transitive dependency must add it themselves after upgrading:

```gradle
implementation 'com.google.android.gms:play-services-location:21.3.0'
```

Without this dependency, geofencing is skipped at runtime.
