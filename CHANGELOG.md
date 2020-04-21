## Changelog

== 1.0.0 ==
* Breaking Change: Migrated to AndroidX
* Breaking Change: Set minSdkVersion to 16
* Updated Gradle Plugin to 3.5.3
* Updated Google Services Plugin to 4.3.3
* Updated Gson to 2.8.6
* Deprecated CleverPushInstanceIDListenerService
* Compatible with newest Firebase libraries

== 0.6.0
* Updated google services + gradle

== 0.5.9 ==
* Added `init` method only with channel ID parameter

== 0.5.8 ==
* Improved App Review dialog

== 0.5.7 ==
* Add ability to delay topics dialog by opens / days / seconds

== 0.5.6 ==
* Minor fix

== 0.5.5 ==
* Transmit last received notification

== 0.5.4 ==
* Event Tracking fix

== 0.5.3 ==
* Added createdAt property for Notifications

== 0.5.2 ==
* Crash Fixes in Fcm Listener

== 0.5.1 ==
* Geo Fence Fixes

== 0.5.0 ==
* Added Geo Fences

== 0.4.3 ==
* Added `TopicsDialogListener`

== 0.4.2 ==
* Sync subscribed topics from API to Client

== 0.4.1 ==
* Added `NotificationReceivedCallbackListener` with ability to set if notification should be shown

== 0.4.0 ==
* Added `trackEvent`
* Added Carousel Notifications

== 0.3.3 ==
* Added `getAvailableTopics`
* Fixed `ChatSubscribeListener`

== 0.3.2 ==
* Topics dialog optimizations

== 0.3.1 ==
* Optimized ChatView: Show error when there is no internet connection available

== 0.3.0 ==
* Fixed ChatUrlOpenedListener
* Added TopicsChangedListener
* Deprecated sync `getChannelConfig` and `getSubscriptionId` methods
* Added ActivityLifecycleListener and removed internal `activity` variable.
* added `isInitialized` method

== 0.2.0 ==
* Added `lockChat` for ChatView
* Optimizations in ChatView
* Automatically call `subscribe` in `init` after getting channel config to prevent potential blocks

== 0.1.13 ==
* Added ability to provide custom activity for Topics Dialog

== 0.1.12 ==
* optimized Topics Dialog

== 0.1.11 ==
* optimized ChatView

== 0.1.10 ==
* added ChatView

== 0.1.9 ==
* catch BadTokenException when showing AppBanner

== 0.1.8 ==
* bug fixes

== 0.1.7 ==
* bug fixes

== 0.1.6 ==
* added new `NotificationReceivedListener` which will be called everytime a notification will be received (in foreground or background)
* app review support

== 0.0.14 ==
* timezone support

== 0.0.13 ==
* fix where launcher activity was not opened sometimes when cold-starting app from notification
* added `setSubscriptionLanguage` and `setSubscriptionCountry`

== 0.0.12 ==
* support big text and big image notification styles

== 0.0.11 ==
* fixes

== 0.0.10 ==
* added `subscribe` and `unsubscribe` methods

== 0.0.9 ==
* make all method async
* deprecate `getAvailableTags` & `getAvailableAttributes` without callback listeners
* add optional parameter `autoRegister` for `init` method (defaults to `true`)
