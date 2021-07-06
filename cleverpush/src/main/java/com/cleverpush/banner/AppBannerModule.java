package com.cleverpush.banner;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerDismissType;
import com.cleverpush.banner.models.BannerFrequency;
import com.cleverpush.banner.models.BannerStatus;
import com.cleverpush.banner.models.BannerStopAtType;
import com.cleverpush.banner.models.BannerTrigger;
import com.cleverpush.banner.models.BannerTriggerCondition;
import com.cleverpush.banner.models.BannerTriggerConditionType;
import com.cleverpush.banner.models.BannerTriggerType;
import com.cleverpush.listener.AppBannersListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppBannerModule {
	@FunctionalInterface
	private interface OnBannerLoaded {
		void notify(List<Banner> banners);
	}

	private static final String TAG = "CleverPush/AppBanner";
	private static final String APP_BANNER_SHARED_PREFS = "com.cleverpush.appbanner";
	private static final String SHOWN_APP_BANNER_PREF = "shownAppBanners";
	private static final long MIN_SESSION_LENGTH = 30 * 60 * 1000L;

	private static AppBannerModule instance;

	public static AppBannerModule init(Activity activity, String channel) {
		return init(activity, channel, false);
	}

	public static AppBannerModule init(Activity activity, String channel, boolean showDrafts) {
		if (instance == null) {
			instance = new AppBannerModule(activity, channel, showDrafts);
		}
		return instance;
	}

	private Activity activity;
	private String channel;
	private boolean showDrafts;
	private long lastSessionTimestamp;
	private int sessions;
	private boolean loading = false;

	private Collection<AppBannerPopup> popups = new ArrayList<>();
	private Collection<AppBannerPopup> pendingBanners = new ArrayList<>();
	private Collection<Banner> banners = null;
	private Collection<AppBannersListener> getBannersListeners = new ArrayList<>();

	private Map<String, String> events = new HashMap<>();

	private HandlerThread handlerThread = new HandlerThread("AppBannerModule");
	private Handler handler;

	private View getRoot() {
		return activity.getWindow().getDecorView().getRootView();
	}

	private AppBannerModule(Activity activity, String channel, boolean showDrafts) {
		this.activity = activity;
		this.channel = channel;
		this.showDrafts = showDrafts;
		this.sessions = this.getSessions();

		handlerThread.start();
		handler = new Handler(handlerThread.getLooper());
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(CleverPushPreferences.APP_BANNERS_IS_POP_UP_SHOWING, false);
		editor.commit();
	}

	private void loadBanners() {
		loadBanners(null);
	}

	private void loadBanners(String notificationId) {
		if (loading) {
			return;
		}
		loading = true;

		String bannersPath = "/channel/" + channel + "/app-banners?platformName=Android";

		if (CleverPush.getInstance(activity).isDevelopmentModeEnabled()) {
			bannersPath += "&t=" + System.currentTimeMillis();
		}

		if (notificationId != null && !notificationId.isEmpty()) {
			bannersPath += "&notificationId=" + notificationId;
		}

		Log.d("CleverPush/AppBanner", "Loading banners: " + bannersPath);

		CleverPushHttpClient.get(bannersPath, new CleverPushHttpClient.ResponseHandler() {
			@Override
			public void onSuccess(String response) {
				loading = false;
				banners = new LinkedList<>();
				try {
					JSONObject responseJson = new JSONObject(response);
					JSONArray rawBanners = responseJson.getJSONArray("banners");

					for (int i = 0; i < rawBanners.length(); ++i) {
						JSONObject rawBanner = rawBanners.getJSONObject(i);
						Banner banner = Banner.create(rawBanner);

						banners.add(banner);
					}

					for (AppBannersListener listener : getBannersListeners) {
						listener.ready(banners);
					}
					getBannersListeners = new ArrayList<>();
				} catch (Exception ex) {
					Log.e(TAG, ex.getMessage(), ex);
				}
			}

			@Override
			public void onFailure(int statusCode, String response, Throwable throwable) {
				loading = false;
				Log.e(TAG, "Something went wrong when loading banners." +
						"\nStatus code: " + statusCode +
						"\nResponse: " + response
				);
			}
		});
	}

	private void sendBannerEvent(String event, Banner banner) {
		Log.d(TAG, "sendBannerEvent: " + event);

		String subscriptionId = null;
		if (CleverPush.getInstance(activity).isSubscribed()) {
			subscriptionId = CleverPush.getInstance(activity).getSubscriptionId();
		}

		JSONObject jsonBody = new JSONObject();
		try {
			jsonBody.put("bannerId", banner.getId());
			jsonBody.put("channelId", channel);
			jsonBody.put("subscriptionId", subscriptionId);
		} catch (JSONException ex) {
			Log.e("CleverPush", ex.getMessage(), ex);
		}

		CleverPushHttpClient.post("/app-banner/event/" + event, jsonBody, new CleverPushHttpClient.ResponseHandler() {
			@Override
			public void onSuccess(String response) {

			}

			@Override
			public void onFailure(int statusCode, String response, Throwable throwable) {
				Log.e(TAG, "App Banner Event failed." +
						"\nStatus code: " + statusCode +
						"\nResponse: " + response
				);
			}
		});
	}

	public void initSession(String channel) {
		this.channel = channel;

		if (
				!CleverPush.getInstance(activity).isDevelopmentModeEnabled()
						&& lastSessionTimestamp > 0
						&& (System.currentTimeMillis() - lastSessionTimestamp) < MIN_SESSION_LENGTH
		) {
			return;
		}

		if (popups.size() > 0) {
			for (AppBannerPopup popup : popups) {
				popup.dismiss();
			}
			popups = new ArrayList<>();
		}

		lastSessionTimestamp = System.currentTimeMillis();

		sessions += 1;
		this.saveSessions();

		banners = null;
		handler.post(this::loadBanners);
		this.startup();
	}

	private int getSessions() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
		return sharedPreferences.getInt(CleverPushPreferences.APP_BANNER_SESSIONS, 0);
	}

	private void saveSessions() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(CleverPushPreferences.APP_BANNER_SESSIONS, sessions);
		editor.apply();
	}

	public void triggerEvent(String key, String value) {
		events.put(key, value);

		this.startup();
	}

	public void getBanners(AppBannersListener listener) {
		this.getBanners(listener, null);
	}

	public void getBanners(AppBannersListener listener, String notificationId) {
		if (listener != null) {
			if (notificationId != null) {
				// reload banners because the banner might have been created just seconds ago
				getBannersListeners.add(listener);
				handler.post(() -> {
					this.loadBanners(notificationId);
				});
			} else {
				if (banners == null) {
					getBannersListeners.add(listener);
				} else {
					listener.ready(banners);
				}
			}
		}
	}

	private void startup() {
		Log.d(TAG, "startup");

		this.getBanners(banners -> {
			createBanners(banners);
			scheduleBanners();
		});
	}

	private void createBanners(Collection<Banner> banners) {
		for (Banner banner : banners) {
			if (banner.getStatus() == BannerStatus.Draft && !showDrafts) {
				Log.d(TAG, "Skipping Banner because: Draft");
				continue;
			}

			if (banner.getFrequency() == BannerFrequency.Once && isBannerShown(banner.getId())) {
				Log.d(TAG, "Skipping Banner because: Frequency");
				continue;
			}

			if (banner.getTriggerType() == BannerTriggerType.Conditions) {
				boolean triggers = false;
				for (BannerTrigger trigger : banner.getTriggers()) {
					boolean triggerTrue = false;
					for (BannerTriggerCondition condition : trigger.getConditions()) {
						boolean conditionTrue = false;
						if (condition.getType() != null) {
							if (condition.getType().equals(BannerTriggerConditionType.Duration)) {
								banner.setDelaySeconds(condition.getSeconds());
								conditionTrue = true;
							}
							if (condition.getType().equals(BannerTriggerConditionType.Sessions)) {
								if (condition.getRelation().equals("lt")) {
									conditionTrue = sessions < condition.getSessions();
								} else {
									conditionTrue = sessions > condition.getSessions();
								}
							}
							if (condition.getType().equals(BannerTriggerConditionType.Event)) {
								String event = events.get(condition.getKey());
								conditionTrue = event != null && event.equals(condition.getValue());
							}
						}

						if (conditionTrue) {
							triggerTrue = true;
							break;
						}
					}

					if (triggerTrue) {
						triggers = true;
						break;
					}
				}

				if (!triggers) {
					Log.d(TAG, "Skipping Banner because: Trigger not satisfied " + sessions);
					continue;
				}
			}

			boolean contains = false;
			for (AppBannerPopup popup : popups) {
				if (popup.getData().getId().equals(banner.getId())) {
					contains = true;
					break;
				}
			}

			if (!contains) {
				popups.add(new AppBannerPopup(activity, banner));
			}
		}
	}

	private void scheduleBanners() {
		if (CleverPush.getInstance(activity).areAppBannersDisabled()) {
			pendingBanners.addAll(popups);
			popups.removeAll(pendingBanners);
			return;
		}

		Date now = new Date();
		for (AppBannerPopup bannerPopup : popups) {
			Banner banner = bannerPopup.getData();

			if (banner.isScheduled()) {
				continue;
			}

			banner.setScheduled();

			if (banner.getStartAt().before(now)) {
				if (banner.getDelaySeconds() > 0) {
					handler.postDelayed(() -> showBanner(bannerPopup), 1000 * banner.getDelaySeconds());
				} else {
					handler.post(() -> showBanner(bannerPopup));
				}
			} else {
				long delay = banner.getStartAt().getTime() - now.getTime();
				handler.postDelayed(() -> showBanner(bannerPopup), delay + (1000 * banner.getDelaySeconds()));
			}
		}
	}

	public void showBannerById(String bannerId) {
		showBannerById(bannerId, null);
	}

	public void showBannerById(String bannerId, String notificationId) {
		Log.d("CleverPush/AppBanner", "showBannerById: " + bannerId);
		this.getBanners(banners -> {
			for (Banner banner : banners) {
				if (banner.getId().equals(bannerId)) {
					AppBannerPopup popup = new AppBannerPopup(activity, banner);

					if (CleverPush.getInstance(activity).areAppBannersDisabled()) {
						pendingBanners.add(popup);
						break;
					}

					handler.post(() -> showBanner(popup));
					break;
				}
			}
		}, notificationId);
	}

	private void showBanner(AppBannerPopup bannerPopup) {
		Date now = new Date();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);

		if(sharedPreferences.getBoolean(CleverPushPreferences.APP_BANNERS_IS_POP_UP_SHOWING,false)) {
			Log.d(TAG, "Skipping Banner because: One Banner already on screen");
			return;
		}

		if (bannerPopup.getData().getStopAtType() == BannerStopAtType.SpecificTime && bannerPopup.getData().getStopAt().before(now)) {
			Log.d(TAG, "Skipping Banner because: Time");
			return;
		}

		bannerPopup.init();
		bannerPopup.show();

		if (bannerPopup.getData().getFrequency() == BannerFrequency.Once) {
			bannerIsShown(bannerPopup.getData().getId());
		}

		if (bannerPopup.getData().getDismissType() == BannerDismissType.Timeout) {
			long timeout = Math.max(0, bannerPopup.getData().getDismissTimeout());
			handler.postDelayed(bannerPopup::dismiss, timeout * 1000);
		}

		bannerPopup.setOpenedListener(action -> {
			this.sendBannerEvent("clicked", bannerPopup.getData());

			if (CleverPush.getInstance(activity).getAppBannerOpenedListener() != null) {
				CleverPush.getInstance(activity).getAppBannerOpenedListener().opened(action);
			}

			if (action.getType().equals("subscribe")) {
				CleverPush.getInstance(activity).subscribe();
			}
		});

		this.sendBannerEvent("delivered", bannerPopup.getData());
	}

	private boolean isBannerShown(String id) {
		if (this.activity == null) {
			return false;
		}

		SharedPreferences sharedPreferences = this.activity.getSharedPreferences(APP_BANNER_SHARED_PREFS, Context.MODE_PRIVATE);
		Set<String> shownBanners = sharedPreferences.getStringSet(SHOWN_APP_BANNER_PREF, new HashSet<>());

		if (shownBanners == null) {
			return false;
		}

		return shownBanners.contains(id);
	}

	private void bannerIsShown(String id) {
		SharedPreferences sharedPreferences = this.activity.getSharedPreferences(APP_BANNER_SHARED_PREFS, Context.MODE_PRIVATE);
		Set<String> shownBanners = sharedPreferences.getStringSet(SHOWN_APP_BANNER_PREF, new HashSet<>());

		assert shownBanners != null;
		shownBanners.add(id);

		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.remove(SHOWN_APP_BANNER_PREF).apply();
		editor.putStringSet(SHOWN_APP_BANNER_PREF, shownBanners);
		editor.commit();
	}

	public void enableBanners() {
		if (pendingBanners != null && pendingBanners.size() > 0) {
			popups.addAll(pendingBanners);
			pendingBanners.clear();
			this.scheduleBanners();
		}
	}

	public void disableBanners() {
		pendingBanners = new ArrayList<>();
	}

	@Override
	protected void finalize() throws Throwable {
		handlerThread.quit();
		super.finalize();
	}
}
