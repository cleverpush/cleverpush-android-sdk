## Changelog

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
