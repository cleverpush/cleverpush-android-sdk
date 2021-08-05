# Changelog

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
