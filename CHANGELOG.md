# Changelog

## 1.33.2 (05.01.2024)
* Added feature in `AppBanner` to support GIF image.
* Added feature to display Action Buttons in notification.

## 1.33.1 (06.12.2023)
* Fixed a bug in `AppBanner`, remove space between text and image.

## 1.33.0 (05.12.2023)
* Support app banner targeting from previously tracked events
* Stop follow-up campaigns on revoked tracking consents

## 1.32.4 (01.12.2023)
* Automatically remove targeting data (tags, attributes) when tracking consent has been revoked

## 1.32.3 (28.11.2023)
* Added new method `setSubscriptionAttribute` to support multiple attribute value as array

## 1.32.2 (27.11.2023)
* Added feature to disable notification permission dialog `setAutoRequestNotificationPermission`
* Added feature to auto resubscribe `setAutoResubscribe`

## 1.32.1 (26.11.2023)
* Fixed version in gradle.properties

## 1.32.0 (26.11.2023)
* IAB TCF compatibility added
* Support `every time on trigger` app banner trigger type

## 1.31.19
* Fixed a potential crash when subscribe or notification object is null in HMS push `onMessageReceived`
* Optimized HTTP calls with retry functionality

## 1.31.18
* Fixed `pullSubscriptionAttributeValue` behaviour
* Optimized app banner voucher codes

## 1.31.17
* Fixed a bug in `AppBanner`, add JavaScriptInterface in HTML block
* Fixed a bug in `AppBanner`, background color not applying when enable multiple screen 

## 1.31.16
* Fixed a bug in `AppBanner`, image scale is not being applied.
* Improved analytics for `AppBanner`: track unique-total click and unique-total delivered

## 1.31.15
* Fixed a potential crash when `createdAt` is null in Notification

## 1.31.14
* Fixed a potential crash when `currentActivity` is null in GeoFences

## 1.31.13
* Optimized `setAuthorizerToken` method for GET request
* Add provision for the .otf font family and button's text font family in `AppBanner`

## 1.31.12
* Fixed a bug in `ChatView`, removed setWebContentsDebuggingEnabled

## 1.31.11
* Fixed a bug in `AppBanner`, JavaScript is not working in WebView 
* Fixed a race condition for `AppBanner` not displaying from notification 

## 1.31.10
* Fixed a bug with `StoryView` which could result in crashes
* Fixed race condition in `init` for `subscribe` and `AppBanner`
* Fixed a bug for `setSubscriptionAttribute`

## 1.31.9
* Fixed a bug with AppBanner AppVersion Targeting

## 1.31.8
* Added feature of auto handle deep link for notification
* Code optimization for ANR

## 1.31.7
* Fixed bug for `trackEvent`

## 1.31.6
* Implemented `setAppBannerShownListener`

## 1.31.5
* Added support for `InitializeListener` in `init()` method

## 1.31.4
* Optimized ChatView behaviour when Channel ID changes

## 1.31.3
* Optimized `subscribe` method for ChatView

## 1.31.2
* Fixed logic for displaying text alignment in app banner
* Fixed bug for inbox view detail screen 

## 1.31.1
* Added feature for displaying voucher code in app banner comes from notification
* Fixed bug for displaying topic dialog
* Added feature to set an authorization token that will be used in an API call.

## 1.31.0
* Added feature for app banner unsubscribe trigger
* Fixed logic for displaying app banners image block
* Fixed bug for material TabLayout in app banner

## 1.30.15
* Increased supported Google Play Services Location Version to 21.0.X

## 1.30.14
* Added support for preview ChatView (unsubscribed)

## 1.30.13
* Added loading spinner
* Fixed a bug in `AppBanner`, displayed duplicate elements in banner

## 1.30.12
* Added feature to set the app banner close button position 
* Added feature for chat view theme color customization

## 1.30.11
* Fixed logic in topics dialog when feature "show topics dialog again on changed topics" is enabled

## 1.30.10
* Fixed a bug with logging which could result in crashes

## 1.30.9
* Fixed Topics Dialog theme
* Support custom fonts for app banners in `res/**` folders

## 1.30.8
* Added callback to SetSubscriptionAttributeResponseHandler

## 1.30.7
* Added StoryViewOpenedListener

## 1.30.6
* App Banners: disabled animations for images while loading
* App Banners: filter banners for timing + targeting before returning in `getAppBannersByGroup`
* App Banners: timing fixed for Android 6
* App Banners: support for notch background color

## 1.30.5
* App Banners: Support "Copy to clipboard" action

## 1.30.4
* Optimized storyview performance and fixed a bug

## 1.30.3
* Fixed sorting and parallel showing of multiple app banners

## 1.30.2
* Fixed HTML AppBanner background color transparent by default

## 1.30.1
* Added more customization options and `setWidgetId` function for `StoryView`

## 1.30.0
* Implemented inbox view detail screen
* Fixed topics dialog style

## 1.29.5
* Fixed notification tracking for conversion events

## 1.29.4
* Added `getTitle`, `getDescription` and `getMediaUrl` to banners
* Optimized `StoryView` activity logic

## 1.29.3
* Further Optimized HTML AppBanner support (added more JS bridge methods)

## 1.29.2
* Fixed a bug connected to removeSubscriptionTopic
* Optimized HTML AppBanner support
* Fixed base64 encoding for AppBanner webview

## 1.29.1
* Fixed logic for app banner conditions when using properties

## 1.29.0
* Removed `triggerAppBannerEvent` method. App banners can now be triggered with the `trackEvent` method.

## 1.28.1
* Added optional failure callbacks for `addSubscriptionTag`, `removeSubscriptionTag`, `addSubscriptionTopic` and `removeSubscriptionTopic`

## 1.28.0
* Support app banner dark mode settings + connected banners (see app banner settings in CleverPush for more information)

## 1.27.7
* Prevent crash in `PermissionActivity` when callback is null

## 1.27.6
* Fixed missing activity in case onActivityResumed is not called

## 1.27.5
* Added try catch for possible race condition

## 1.27.4
* Fixed missing `fcmId` in request to CleverPush API.

## 1.27.3
* Fixed crash when runnable throws an exception

## 1.27.2
* Release new `NotificationOpenedCallbackListener` (see documentation)

## 1.27.1
* Added `getDeviceToken` method

## 1.27.0
* Make `areNotificationsEnabled` method public

## 1.26.9
* Improve permission activity styling

## 1.26.8
* Added ability to specify custom `dialogActivity` in `subscribe` method
* Improved app banner targeting behaviour

## 1.26.7
* Fixed app banner attribute targeting
* Added new method to support showing app banner drafts: `setAppBanerDraftsEnabled`

## 1.26.6
* Fixed race condition in `showTopicsDialog` method

## 1.26.5
* Support app banner language filters
* Implement methods to optionally disable app banner statistic tracking

## 1.26.4
* Updated proguard to prevent gson issue

## 1.26.3
* Implemented optional callback for `unsubscribe` method

## 1.26.2
* Publish maven package with `api` scope for dependencies to potentially fix errors where proguard rules from dependencies were not included

## 1.26.1
* Added more logs and try-catch to prevent potential crash

## 1.26.0
* Implemented optional Device ID feature to prevent duplicate notifications on multiple apps of the same package name group.

## 1.25.2
* Fixed potential crashes in AppBanners

## 1.25.1
* Support disabling of badge counts via Notification Categories

## 1.25.0
* Android 13 support

## 1.24.2
* Improved app banner version filter conditions

## 1.24.1
* Added ability to set own log listener via `setLogListener`

## 1.24.0
* Added support for attribute filter relations
* Implemented setMaximumNotificationCount method
* Implemented add/removeSubscriptionTopic methods

## 1.23.6
* Optimized app banner trigger conditions

## 1.23.5
* Implemented `setKeepTargetingDataOnUnsubscribe` method

## 1.23.4
* Optimized layout for app banners with multiple buttons

## 1.23.3
* Implemented `setCustomActivity` method

## 1.23.2
* Optimized ActivityLifecycleListener behaviour

## 1.23.1
* Implemented `SubscribeCallbackListener` which can be passed as an argument to the `subscribe` method

## 1.23.0
* Disable `subscribe` calls when notification permission has been disabled.
* Added version filters for app banners
* Added multiple screen support for app banners without carousel

## 1.22.1
* Implemented `isAppOpen` method used by Cordova SDK

## 1.22.0
* Implemented `triggerFollowUpEvent` method

## 1.21.2
* Implemented FCM `onNewToken` listener

## 1.21.1
* Fixed topics dialog scrolling

## 1.21.0
* Optimized Activity resume behaviour when notifications are opened
* Implemented meta data entry for disabling default activity launching: `<meta-data android:name="com.cleverpush.notification_open_activity_disabled" android:value="false" />`

## 1.20.2
* Fixed topics dialog behaviour on cancel

## 1.20.1
* Implemented `setIgnoreDisabledNotificationPermission` to subscribe users, even when the notification permission has not been accepted (disabled by default).

## 1.20.0
* Optimized topics dialog behaviour
* Fixed a race condition when adding tag directly after subscribing

## 1.19.9
* Keep image aspect ratio in TEXT_WITH_IMAGE notification style

## 1.19.8
* Make sure tags are added and removed one after another to prevent race conditions

## 1.19.7
* Optimized Notification category setup

## 1.19.6
* Added condition in ActivityLifecycleListener to prevent NPE in Android 7

## 1.19.5
* Optimized edge case in parseColor utility method

## 1.19.4
* Reverted SerializedName change in Notification class

## 1.19.3
* Further optimized cursor pagination for `getNotificationsCombined`

## 1.19.2
* Optimized cursor pagination for `getNotificationsCombined`

## 1.19.1
* Added pagination cursor for `getNotifications` call

## 1.19.0
* Updated several dependencies (AndroidX, maximum FCM & Google play services versions)
* Added more try-catch handlers to prevent crashes in edge cases

## 1.18.15
* Added safety check in ActivityLifecycleListener to prevent NPE

## 1.18.14
* Added `getAppBanners` method

## 1.18.13
* Added App Banner targeting filter: subscribed state

## 1.18.12
* Added `removeNotification(id)` method

## 1.18.11
* Optimized `createdAt` field for `Notification` class

## 1.18.10
* Do not use Topics Dialog title from config if it's empty
* Optimized displaying of app banners - wait until root view is ready

## 1.18.9
* Added ability to disable nightmode adaption via `disableNightModeAdaption()` (e.g. for topics dialog checkboxes)

## 1.18.8
* Further optimized getInstance calls to CleverPush main class from AppBannerModule

## 1.18.7
* Optimized getInstance calls to CleverPush main class from AppBannerModule

## 1.18.6
* Fixed Release

## 1.18.5
* Optimized setUpNotificationCategoryGroups function to prevent exception

## 1.18.4
* Fixed another NullPointerException in App Banners when Activity is null

## 1.18.3
* Automatically set up notification categories on init
* Fixed NullPointerException in App Banners when Activity is null

## 1.18.2
* Fixed NullPointerException when opening App Banner URL in WebView

## 1.18.1
* Added ability to set listeners outside of `init` methods

## 1.18.0
* Automatically re-sync when device token changes (might happen during development)
* Add ability to specify custom Notification Styles

## 1.17.0
* Updates for Android 12

## 1.16.5
* Delayed CleanUpService start until first Activity is ready
* Added new getNotifications(true, callback) method which can combine notifications from local storage and from the API

## 1.16.4
* Fixed ActivityLifecycleListener removing CleverPush instance too early

## 1.16.3
* Improved 'autoRegister: true' behaviour when user has unsubscribed.

## 1.16.2
* Fixed NotificationOpenedHandler firing twice when re-opening app from background
* Addressed possible memory leaks

## 1.16.1
* Optimized app review feedback email

## 1.16.0
* Added App Stories
* Added methods for adding / removing multiple tags at once

## 1.15.6
* Improved Topics Dialog behaviour

## 1.15.5
* App Banners: Optimized app banner behaviour

## 1.15.4
* Optimized behaviour for downloading Notification images

## 1.15.3
* Added ProGuard consumer rules

## 1.15.2
* App Banners: prevent showing multiple banners at the same time
* App Banners: Validate "stopAt" field for banners triggered manually or by notification

## 1.15.1
* Catch FCM exceptions in onReceivedMessage listener to prevent crash on invalid payloads

## 1.15.0
* Added `pushSubscriptionAttribute`, `pullSubscriptionAttribute` and `hasSubscriptionAttributeValue` methods for array attributes
* Result from getSubscriptionAttributes added to Map<String, Object>

## 1.14.5
* Improved style of topics dialog

## 1.14.4
* Improved Behaviour of App Banners
* Improved internal storage of received notifications

## 1.14.3
* Improved Behaviour of App Banners

## 1.14.2
* Optimized Topics Dialog for Dark Mode

## 1.14.1
* Optimized Topics Dialog behaviour when shown multiple times

## 1.14.0
* Improved App Banner Positions (Top, Center, Bottom)
* Added Support for Firebase SDK 22+
* Improved "Uncheck all topics" behaviour
* Added methods for enabling an disabling App Banners

## 1.13.1
* Improved App Banner Layout for tablets

## 1.13.0
* Improved App Banner Conditions
* New App Banner block type: HTML
* Support custom fonts in App Banner
* Open App Banner via Notification

## 1.12.1
* Improved behaviour of automatic incrementing of badge count

## 1.12.0
* Introduced optional 'Deselect all' checkbox in TopicsDialog (can be enabled in your CleverPush topic settings).
* Support app banners with HTML content

## 1.11.1
* Optimized silent notifications

## 1.11.0
* Added support for silent push notifications

## 1.10.0
* Provide new methods for tracking deliveries and clicks when using the `NotificationExtenderService` with own Notifications
* Updated to latest Firebase dependencies

## 1.9.1
* Resolved a race condition where the NotificationOpenedHandler might yield the wrong notification for multiple notification received at the exact same time.

## 1.9.0
* Added extra null check for subscription_id (#4)

## 1.8.12
* Allow notifications with empty titles

## 1.8.11
* Custom Data support for Topics

## 1.8.10
* Fixed translation

## 1.8.9
* Fixed Notification colors

## 1.8.8
* Added ability to specify a Theme ID (`R.style.Theme_…`) for the Topics Dialog

## 1.8.7
* Use AlertDialog from AndroidX

## 1.8.6
* Fixed Exception in NotificationCategory class

## 1.8.5
* Added platform name to App Banner path to make filtering by platform possible in the backend.

## 1.8.4
* Add try-catch black inside NotificationExtenderService

## 1.8.3
* Clear subscription data incl. Topics, Tags and Attributes when changing Channel ID

## 1.8.2
* Allow automatic automatic replacing of active Notifications in status bar when the same Tag / ID is used.

## 1.8.1
* Fixes for App Banner Images

## 1.8.0
* New App Banners

## 1.7.5
* Added `externalId` to ChannelTopics

## 1.7.4
* Reset Channel Config when Channel ID changes in `init`

## 1.7.3
* Added `enableDevelopmentMode` to bypass config cache
* Automatically re-subscribe when Channel ID changes

## 1.7.2
* Fixed sorting of getAvailableTopics result

## 1.7.1
* Fixed empty FCM ID when calling subscribe before init has finished

## 1.7.0
* New Badge Support

## 1.6.7
* Fixed JSONException inside `getAvailableTopicsFromConfig`

## 1.6.6
* Handle empty FCM notification data

## 1.6.5
* Update ChannelTopic object

## 1.6.4
* Added various Notification Category features

## 1.6.3
* Fixed ExtenderService with Carousel Notifications

## 1.6.2
* Support child topics

## 1.6.1
* Modified `setTrackingConsent` behaviour: If called with `false` no more future tracking calls will be queued and all recent queued calls will be removed

## 1.6.0
* Added `TrackingConsentListener`

## 1.5.0
* Support custom `NotificationExtenderService` (see docs)

## 1.4.1
* Fix for Notification Channel sounds

## 1.4.0
* Custom sound support

## 1.3.0
* Easier tagging support

## 1.2.0
* Support for Android Notification Channels

## 1.1.0
* Support for HMS (Huawei Mobile Services)

## 1.0.0
* Breaking Change: Migrated to AndroidX
* Breaking Change: Set minSdkVersion to 16
* Updated Gradle Plugin to 3.5.3
* Updated Google Services Plugin to 4.3.3
* Updated Gson to 2.8.6
* Deprecated CleverPushInstanceIDListenerService
* Compatible with newest Firebase libraries

## 0.6.0
* Updated google services + gradle

## 0.5.9
* Added `init` method only with channel ID parameter

## 0.5.8
* Improved App Review dialog

## 0.5.7
* Add ability to delay topics dialog by opens / days / seconds

## 0.5.6
* Minor fix

## 0.5.5
* Transmit last received notification

## 0.5.4
* Event Tracking fix

## 0.5.3
* Added createdAt property for Notifications

## 0.5.2
* Crash Fixes in Fcm Listener

## 0.5.1
* Geo Fence Fixes

## 0.5.0
* Added Geo Fences

## 0.4.3
* Added `TopicsDialogListener`

## 0.4.2
* Sync subscribed topics from API to Client

## 0.4.1
* Added `NotificationReceivedCallbackListener` with ability to set if notification should be shown

## 0.4.0
* Added `trackEvent`
* Added Carousel Notifications

## 0.3.3
* Added `getAvailableTopics`
* Fixed `ChatSubscribeListener`

## 0.3.2
* Topics dialog optimizations

## 0.3.1
* Optimized ChatView: Show error when there is no internet connection available

## 0.3.0
* Fixed ChatUrlOpenedListener
* Added TopicsChangedListener
* Deprecated sync `getChannelConfig` and `getSubscriptionId` methods
* Added ActivityLifecycleListener and removed internal `activity` variable.
* added `isInitialized` method

## 0.2.0
* Added `lockChat` for ChatView
* Optimizations in ChatView
* Automatically call `subscribe` in `init` after getting channel config to prevent potential blocks

## 0.1.13
* Added ability to provide custom activity for Topics Dialog

## 0.1.12
* optimized Topics Dialog

## 0.1.11
* optimized ChatView

## 0.1.10
* added ChatView

## 0.1.9
* catch BadTokenException when showing AppBanner

## 0.1.8
* bug fixes

## 0.1.7
* bug fixes

## 0.1.6
* added new `NotificationReceivedListener` which will be called everytime a notification will be received (in foreground or background)
* app review support

## 0.0.14
* timezone support

## 0.0.13
* fix where launcher activity was not opened sometimes when cold-starting app from notification
* added `setSubscriptionLanguage` and `setSubscriptionCountry`

## 0.0.12
* support big text and big image notification styles

## 0.0.11
* fixes

## 0.0.10
* added `subscribe` and `unsubscribe` methods

## 0.0.9
* make all method async
* deprecate `getAvailableTags` & `getAvailableAttributes` without callback listeners
* add optional parameter `autoRegister` for `init` method (defaults to `true`)
