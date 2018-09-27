# Installation

1. Add the needed libraries to your Gradle config

   ```groovy
   implementation 'com.cleverpush:cleverpush:+'
   implementation 'com.android.support:support-v4:+'
   implementation 'com.google.firebase:firebase-messaging:+'
   implementation 'com.google.code.gson:gson:2.8.5'
   ```

2. Add the following tags to your AndroidManifest.xml file

   ```xml
   <application ...>

       <meta-data android:name="CLEVERPUSH_CHANNEL_ID" android:value="[CLEVERPUSH.CHANNEL.ID]" />
    
       <service
           android:name="com.cleverpush.service.CleverPushFcmListenerService">
           <intent-filter>
               <action android:name="com.google.firebase.MESSAGING_EVENT" />
           </intent-filter>
       </service>
       <service
           android:name="com.cleverpush.service.CleverPushInstanceIDListenerService">
           <intent-filter>
               <action android:name="com.google.firebase.INSTANCE_ID_EVENT" />
           </intent-filter>
       </service>
    
   </application>
   ```

   Be sure to insert your correct `[CLEVERPUSH.CHANNEL.ID]`, which can be found in the CleverPush settings and to replace `[YOUR.PACKAGE.NAME]` with your package name.

3. In the `onCreate` method of your Main activity, call `CleverPush.getInstance(this).init();`

   ```java
   public class MainActivity extends Activity {
       public void onCreate(Bundle savedInstanceState) {
           CleverPush.getInstance(this).init();
       }
   }
   ```


   You can also add a `NotificationOpenedListener`


   ```java
   public class MainActivity extends Activity {
       public void onCreate(Bundle savedInstanceState) {
           CleverPush.getInstance(this).init(new NotificationOpenedListener() {
               notificationOpened(NotificationOpenedResult result) {
                  System.out.println("Opened CleverPush Notification with URL: " + result.getNotification().getUrl());
              };
           });
       }
   }
   ```


   And a `SubscribedListener`


   ```java
   public class MainActivity extends Activity {
      public void onCreate(Bundle savedInstanceState) {
          CleverPush.getInstance(this).init(new NotificationOpenedListener() {
              notificationOpened(NotificationOpenedResult result) {
                 System.out.println("Opened CleverPush Notification with URL: " + result.getNotification().getUrl());
             };
          }, new SubscribedListener() {
               subscribed(String subscriptionId) {
                  System.out.println("CleverPush Subscription ID: " + subscriptionId);
              };
           });
      }
   }
   ```


If you use ProGuard, you need to add these exceptions:

```
-keep class com.cleverpush.** { *; }
-keep interface com.cleverpush.** { *; }
```
