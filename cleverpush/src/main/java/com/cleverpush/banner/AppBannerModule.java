package com.cleverpush.banner;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerDismissType;
import com.cleverpush.banner.models.BannerFrequency;
import com.cleverpush.banner.models.BannerStatus;
import com.cleverpush.banner.models.BannerStopAtType;
import com.cleverpush.banner.models.BannerSubscribedType;
import com.cleverpush.banner.models.BannerTrigger;
import com.cleverpush.banner.models.BannerTriggerCondition;
import com.cleverpush.banner.models.BannerTriggerConditionType;
import com.cleverpush.banner.models.BannerTriggerType;
import com.cleverpush.banner.models.CheckFilterRelation;
import com.cleverpush.banner.models.VersionComparison;
import com.cleverpush.listener.ActivityInitializedListener;
import com.cleverpush.listener.AppBannersListener;
import com.cleverpush.responsehandlers.SendBannerEventResponseHandler;
import com.cleverpush.util.Logger;
import com.cleverpush.util.VersionComparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class AppBannerModule {

    private static final String TAG = "CleverPush/AppBanner";
    private static final String APP_BANNER_SHARED_PREFS = "com.cleverpush.appbanner";
    private static final String SHOWN_APP_BANNER_PREF = "shownAppBanners";
    private static final long MIN_SESSION_LENGTH = 30 * 60 * 1000L;
    private static AppBannerModule instance;
    private String channel;
    private final boolean showDrafts;
    private long lastSessionTimestamp;
    private int sessions;
    private boolean loading = false;
    private Collection<AppBannerPopup> popups = new ArrayList<>();
    private Collection<AppBannerPopup> pendingBanners = new ArrayList<>();
    private Collection<Banner> banners = null;
    private Collection<AppBannersListener> bannersListeners = new ArrayList<>();
    private final Map<String, String> events = new HashMap<>();
    private final HandlerThread handlerThread = new HandlerThread("AppBannerModule");
    private final Handler handler;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private AppBannerModule(String channel, boolean showDrafts, SharedPreferences sharedPreferences, SharedPreferences.Editor editor) {
        this.channel = channel;
        this.showDrafts = showDrafts;
        this.sharedPreferences = sharedPreferences;
        this.editor = editor;
        this.sessions = this.getSessions();

        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        editor.putBoolean(CleverPushPreferences.APP_BANNER_SHOWING, false);
        editor.commit();
    }

    private View getRoot() {
        return getCurrentActivity().getWindow().getDecorView().getRootView();
    }

    public static AppBannerModule init(String channel, SharedPreferences sharedPreferences, SharedPreferences.Editor editor) {
        return init(channel, false, sharedPreferences, editor);
    }

    public static AppBannerModule init(String channel, boolean showDrafts, SharedPreferences sharedPreferences, SharedPreferences.Editor editor) {
        if (instance == null) {
            instance = new AppBannerModule(channel, showDrafts, sharedPreferences, editor);
        }
        return instance;
    }

    private void loadBanners(String channelId) {
        loadBanners(null, channelId);
    }

    private void loadBanners() {
        loadBanners(null, channel);
    }

    void loadBanners(String notificationId, String channelId) {
        if (isLoading()) {
            return;
        }
        setLoading(true);
        String bannersPath = "/channel/" + channelId + "/app-banners?platformName=Android";
        if (getCleverPushInstance().isDevelopmentModeEnabled()) {
            bannersPath += "&t=" + System.currentTimeMillis();
        }
        if (notificationId != null && !notificationId.isEmpty()) {
            bannersPath += "&notificationId=" + notificationId;
        }
        Logger.d(TAG, "Loading banners: " + bannersPath);
        CleverPushHttpClient.get(bannersPath, new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                setLoading(false);
                banners = new LinkedList<>();
                try {
                    JSONObject responseJson = new JSONObject(response);
                    JSONArray rawBanners = responseJson.getJSONArray("banners");

                    for (int i = 0; i < rawBanners.length(); ++i) {
                        JSONObject rawBanner = rawBanners.getJSONObject(i);
                        Banner banner = Banner.create(rawBanner);
                        banners.add(banner);
                    }

                    for (AppBannersListener listener : getBannersListeners()) {
                        listener.ready(banners);
                    }

                    bannersListeners = new ArrayList<>();
                } catch (Exception ex) {
                    Logger.e(TAG, ex.getMessage(), ex);
                }

            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                setLoading(false);
                Logger.e(TAG, "Something went wrong when loading banners." +
                        "\nStatus code: " + statusCode +
                        "\nResponse: " + response
                );
            }
        });
    }

    void sendBannerEvent(String event, Banner banner) {
        Logger.d(TAG, "sendBannerEvent: " + event);

        String subscriptionId = null;
        if (getCleverPushInstance().isSubscribed()) {
            subscriptionId = getCleverPushInstance().getSubscriptionId();
        }

        JSONObject jsonBody = getJsonObject();
        try {
            jsonBody.put("bannerId", banner.getId());
            if (banner.getTestId() != null) {
                jsonBody.put("testId", banner.getTestId());
            }
            jsonBody.put("channelId", channel);
            jsonBody.put("subscriptionId", subscriptionId);
        } catch (JSONException ex) {
            Logger.e(LOG_TAG, ex.getMessage(), ex);
        }

        CleverPushHttpClient.post("/app-banner/event/" + event, jsonBody, new SendBannerEventResponseHandler().getResponseHandler());
    }

    public void initSession(String channel) {
        this.channel = channel;
        if (!getCleverPushInstance().isDevelopmentModeEnabled()
                && lastSessionTimestamp > 0
                && (System.currentTimeMillis() - lastSessionTimestamp) < MIN_SESSION_LENGTH) {
            return;
        }

        if (getPopups().size() > 0) {
            for (AppBannerPopup popup : getPopups()) {
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

    public int getSessions() {
        return sharedPreferences.getInt(CleverPushPreferences.APP_BANNER_SESSIONS, 0);
    }

    void saveSessions() {
        editor.putInt(CleverPushPreferences.APP_BANNER_SESSIONS, sessions);
        editor.apply();
    }

    public void triggerEvent(String key, String value) {
        events.put(key, value);
        this.startup();
    }

    public void getBannerList(AppBannersListener listener, String channelId) {
        if (listener == null) {
            return;
        }
        if (channelId != null) {
            bannersListeners.add(listener);
            getHandler().post(() -> {
                this.loadBanners(channelId);
            });
        }
    }

    public void getBanners(AppBannersListener listener) {
        this.getBanners(listener, null);
    }

    public void getBanners(AppBannersListener listener, String notificationId) {
        if (listener == null) {
            return;
        }
        if (notificationId != null) {
            // reload banners because the banner might have been created just seconds agox
            bannersListeners.add(listener);

            getHandler().post(() -> {
                this.loadBanners(notificationId, channel);
            });
        } else {
            if (getListOfBanners() == null) {
                bannersListeners.add(listener);
            } else {
                listener.ready(getListOfBanners());
            }
        }
    }

    void startup() {
        Logger.d(TAG, "startup");

        this.getBanners(banners -> {
            createBanners(banners);
            scheduleBanners();
        });
    }

    boolean isBannerTimeAllowed(Banner banner) {
        Date now = new Date();
        if (banner == null) {
            return false;
        }
        return banner.getStopAtType() != BannerStopAtType.SpecificTime
                || banner.getStopAt() == null
                || banner.getStopAt().after(now);
    }

    boolean isBannerTargetingAllowed(Banner banner) {
        if (banner == null) {
            return false;
        }

        boolean allowed = true;

        if (banner.getSubscribedType() == BannerSubscribedType.Subscribed && !getCleverPushInstance().isSubscribed()) {
            allowed = false;
        }

        if (banner.getSubscribedType() == BannerSubscribedType.Unsubscribed && getCleverPushInstance().isSubscribed()) {
            allowed = false;
        }

        if (banner.getTags() != null && banner.getTags().size() > 0) {
            allowed = false;

            for (String tag : banner.getTags()) {
                if (getCleverPushInstance().hasSubscriptionTag(tag)) {
                    allowed = true;
                    break;
                }
            }
        }

        if (allowed && banner.getExcludeTags() != null && banner.getExcludeTags().size() > 0) {
            for (String tag : banner.getExcludeTags()) {
                if (getCleverPushInstance().hasSubscriptionTag(tag)) {
                    allowed = false;
                    break;
                }
            }
        }

        if (allowed && banner.getTopics() != null && banner.getTopics().size() > 0) {
            allowed = false;

            for (String topic : banner.getTopics()) {
                if (getCleverPushInstance().hasSubscriptionTopic(topic)) {
                    allowed = true;
                    break;
                }
            }
        }

        if (allowed && banner.getExcludeTopics() != null && banner.getExcludeTopics().size() > 0) {
            for (String topic : banner.getExcludeTopics()) {
                if (getCleverPushInstance().hasSubscriptionTopic(topic)) {
                    allowed = false;
                    break;
                }
            }
        }

        if (allowed && banner.getAttributes() != null && banner.getAttributes().size() > 0) {
            allowed = false;
            for (HashMap<String, String> attribute : banner.getAttributes()) {
                String attributeId = attribute.get("id");
                String compareAttributeValue = attribute.get("value");
                String fromVersion = attribute.get("fromVersion");
                String toVersion = attribute.get("toVersion");
                String relationString = attribute.get("relation");
                if (relationString == null) {
                    relationString = "equals";
                }
                String attributeValue = (String) getCleverPushInstance().getSubscriptionAttribute(attributeId);
                if (this.checkRelationFilter(true, CheckFilterRelation.fromString(relationString), compareAttributeValue, attributeValue, fromVersion, toVersion)) {
                    allowed = true;
                    break;
                }
            }
        }

        allowed = appVersionFilter(allowed, banner);

        return allowed;
    }

    /**
     * App Banner Version Filter
     */
    private boolean appVersionFilter(boolean allowed, Banner banner) {
        try {
            PackageInfo pInfo = this.getCurrentActivity().getPackageManager().getPackageInfo(this.getCurrentActivity().getPackageName(), 0);
            String versionName = pInfo.versionName;
            return this.checkRelationFilter(allowed, banner.getBannerAppVersionFilterRelation(), versionName, banner.getAppVersionFilterValue(), banner.getFromVersion(), banner.getToVersion());
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG, "Error checking app version filter", e);
        }

        return allowed;
    }

    private boolean checkRelationFilter(boolean allowed, CheckFilterRelation relation, String appVersion, String compareValue, String fromVersion, String toVersion) {
        VersionComparator vc = new VersionComparator();

        VersionComparison result = vc.compare(appVersion, compareValue);
        VersionComparison resultFrom = vc.compare(appVersion, fromVersion);
        VersionComparison resultTo = vc.compare(appVersion, toVersion);

        if (relation == null) {
            return allowed;
        }

        try {
            if (allowed && relation.equals(CheckFilterRelation.Equals)) {
                if (result != VersionComparison.EQUALS) {
                    allowed = false;
                }
            }

            if (allowed && relation.equals(CheckFilterRelation.Between)) {
                if (resultFrom != VersionComparison.EQUALS && resultFrom != VersionComparison.GREATER_THAN && resultTo != VersionComparison.EQUALS && resultTo != VersionComparison.GREATER_THAN) {
                    allowed = false;
                }
            }

            if (allowed && relation.equals(CheckFilterRelation.NotEqual)) {
                if (result == VersionComparison.EQUALS) {
                    allowed = false;
                }
            }

            if (allowed && relation.equals(CheckFilterRelation.GreaterThan)) {
                if (result != VersionComparison.GREATER_THAN) {
                    allowed = false;
                }
            }

            if (allowed && relation.equals(CheckFilterRelation.LessThan)) {
                if (result != VersionComparison.LESS_THAN) {
                    allowed = false;
                }
            }

            if (allowed && relation.equals(CheckFilterRelation.Contains)) {
                if (!compareValue.contains(appVersion)) {
                    allowed = false;
                }
            }

            if (allowed && relation.equals(CheckFilterRelation.NotContains)) {
                if (compareValue.contains(appVersion)) {
                    allowed = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG, "Error checking app version filter", e);
        }

        return allowed;
    }

    private void createBanners(Collection<Banner> banners) {
        for (Banner banner : banners) {
            if (banner.getStatus() == BannerStatus.Draft && !showDrafts) {
                Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Draft");
                continue;
            }

            if (banner.getFrequency() == BannerFrequency.Once && isBannerShown(banner.getId())) {
                Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Frequency");
                continue;
            }

            if (!isBannerTimeAllowed(banner)) {
                Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Time");
                continue;
            }

            if (!isBannerTargetingAllowed(banner)) {
                Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Targeting");
                continue;
            }

            if (banner.getTriggerType() == BannerTriggerType.Conditions) {
                boolean triggers = false;
                for (BannerTrigger trigger : banner.getTriggers()) {
                    boolean triggerTrue = true;
                    for (BannerTriggerCondition condition : trigger.getConditions()) {
                        // true by default to make the AND check work
                        boolean conditionTrue = true;
                        if (condition.getType() != null) {
                            if (condition.getType().equals(BannerTriggerConditionType.Duration)) {
                                banner.setDelaySeconds(condition.getSeconds());
                            } else if (condition.getType().equals(BannerTriggerConditionType.Sessions)) {
                                if (condition.getRelation().equals("lt")) {
                                    conditionTrue = sessions < condition.getSessions();
                                } else {
                                    conditionTrue = sessions > condition.getSessions();
                                }
                            } else if (condition.getType().equals(BannerTriggerConditionType.Event)) {
                                String event = events.get(condition.getKey());
                                conditionTrue = event != null && event.equals(condition.getValue());
                            } else {
                                conditionTrue = false;
                            }
                        }
                        if (!conditionTrue) {
                            triggerTrue = false;
                            break;
                        }
                    }
                    if (triggerTrue) {
                        triggers = true;
                        break;
                    }
                }

                if (!triggers) {
                    Logger.d(TAG, "Skipping Banner because: Trigger not satisfied " + sessions);
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
                getActivityLifecycleListener().setActivityInitializedListener(() -> popups.add(new AppBannerPopup(getCurrentActivity(), banner)));
            }
        }
    }

    void scheduleBanners() {
        if (getCleverPushInstance().isAppBannersDisabled()) {
            pendingBanners.addAll(getPopups());
            getPopups().removeAll(pendingBanners);
            return;
        }

        Date now = new Date();
        for (AppBannerPopup bannerPopup : getPopups()) {
            Banner banner = bannerPopup.getData();

            if (banner.isScheduled()) {
                continue;
            }

            banner.setScheduled();

            if (banner.getStartAt().before(now)) {
                if (banner.getDelaySeconds() > 0) {
                    getHandler().postDelayed(() -> showBanner(bannerPopup), 1000L * banner.getDelaySeconds());
                } else {
                    getHandler().post(() -> showBanner(bannerPopup));
                }
            } else {
                long delay = banner.getStartAt().getTime() - now.getTime();
                getHandler().postDelayed(() -> showBanner(bannerPopup), delay + (1000L * banner.getDelaySeconds()));
            }
        }
    }

    public void showBannerById(String bannerId) {
        showBannerById(bannerId, null);
    }

    public void showBannerById(String bannerId, String notificationId) {
        getActivityLifecycleListener().setActivityInitializedListener(new ActivityInitializedListener() {
            @Override
            public void initialized() {
                Logger.d(TAG, "showBannerById: " + bannerId);
                getBanners(banners -> {
                    for (Banner banner : banners) {
                        if (banner.getId().equals(bannerId)) {
                            AppBannerPopup popup = getAppBannerPopup(banner);

                            if (getCleverPushInstance().isAppBannersDisabled()) {
                                pendingBanners.add(popup);
                                break;
                            }
                            getHandler().post(() -> showBanner(popup));
                            break;
                        }
                    }
                }, notificationId);
            }
        });
    }

    public AppBannerPopup getAppBannerPopup(Banner banner) {
        return new AppBannerPopup(getCurrentActivity(), banner);
    }

    void showBanner(AppBannerPopup bannerPopup) {
        if (sharedPreferences.getBoolean(CleverPushPreferences.APP_BANNER_SHOWING, false)) {
            Logger.d(TAG, "Skipping Banner because: A Banner is already on the screen");
            return;
        }

        if (!isBannerTimeAllowed(bannerPopup.getData())) {
            Logger.d(TAG, "Skipping Banner because: Stop Time");
            return;
        }

        bannerPopup.init();
        bannerPopup.show();

        if (bannerPopup.getData().getFrequency() == BannerFrequency.Once) {
            getActivityLifecycleListener().setActivityInitializedListener(new ActivityInitializedListener() {
                @Override
                public void initialized() {
                    bannerIsShown(bannerPopup.getData().getId());
                }
            });
        }

        if (bannerPopup.getData().getDismissType() == BannerDismissType.Timeout) {
            long timeout = Math.max(0, bannerPopup.getData().getDismissTimeout());
            getHandler().postDelayed(bannerPopup::dismiss, timeout * 1000);
        }

        bannerPopup.setOpenedListener(action -> {
            sendBannerEvent("clicked", bannerPopup.getData());

            if (getCleverPushInstance().getAppBannerOpenedListener() != null) {
                getCleverPushInstance().getAppBannerOpenedListener().opened(action);
            }

            if (action.getType().equals("subscribe")) {
                getCleverPushInstance().subscribe();
            }

            if (action.getType().equals("addTags")) {
                getCleverPushInstance().addSubscriptionTags(action.getTags().toArray(new String[0]));
            }

            if (action.getType().equals("removeTags")) {
                getCleverPushInstance().removeSubscriptionTags(action.getTags().toArray(new String[0]));
            }

            if (action.getType().equals("addTopics")) {
                Set<String> topics = getCleverPushInstance().getSubscriptionTopics();
                topics.addAll(action.getTopics());
                getCleverPushInstance().setSubscriptionTopics(topics.toArray(new String[0]));
            }

            if (action.getType().equals("removeTopics")) {
                Set<String> topics = getCleverPushInstance().getSubscriptionTopics();
                topics.removeAll(action.getTopics());
                getCleverPushInstance().setSubscriptionTopics(topics.toArray(new String[0]));
            }

            if (action.getType().equals("setAttribute")) {
                getCleverPushInstance().setSubscriptionAttribute(action.getAttributeId(), action.getAttributeValue());
            }

            if (action.getType().equals("switchScreen")) {
            }
        });

        this.sendBannerEvent("delivered", bannerPopup.getData());
    }

    private boolean isBannerShown(String id) {
        if (getCurrentActivity() == null) {
            return false;
        }

        SharedPreferences sharedPreferences = getCurrentActivity().getSharedPreferences(APP_BANNER_SHARED_PREFS, Context.MODE_PRIVATE);
        Set<String> shownBanners = sharedPreferences.getStringSet(SHOWN_APP_BANNER_PREF, new HashSet<>());

        if (shownBanners == null) {
            return false;
        }

        return shownBanners.contains(id);
    }

    void bannerIsShown(String id) {
        if (getCurrentActivity() == null) {
            return;
        }

        SharedPreferences sharedPreferences = getCurrentActivity().getSharedPreferences(APP_BANNER_SHARED_PREFS, Context.MODE_PRIVATE);
        Set<String> shownBanners = sharedPreferences.getStringSet(SHOWN_APP_BANNER_PREF, new HashSet<>());

        assert shownBanners != null;
        shownBanners.add(id);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(SHOWN_APP_BANNER_PREF);
        editor.apply();
        editor.putStringSet(SHOWN_APP_BANNER_PREF, shownBanners);
        editor.commit();
    }

    public void enableBanners() {
        if (getPendingBanners() != null && getPendingBanners().size() > 0) {
            popups.addAll(getPendingBanners());
            getPendingBanners().clear();
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

    public long getLastSessionTimestamp() {
        return lastSessionTimestamp;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public Collection<Banner> getListOfBanners() {
        return banners;
    }

    public Collection<AppBannerPopup> getPopups() {
        return popups;
    }

    public CleverPush getCleverPushInstance() {
        return CleverPush.getInstance(CleverPush.context, true);
    }

    public Collection<AppBannersListener> getBannersListeners() {
        return bannersListeners;
    }

    public void clearBannersListeners() {
        bannersListeners.clear();
    }

    public JSONObject getJsonObject() {
        return new JSONObject();
    }

    public Handler getHandler() {
        return handler;
    }

    public Collection<AppBannerPopup> getPendingBanners() {
        return pendingBanners;
    }

    public void clearPendingBanners() {
        pendingBanners.clear();
    }

    public Activity getCurrentActivity() {
        return CleverPush.getInstance(CleverPush.context).getCurrentActivity();
    }

    public ActivityLifecycleListener getActivityLifecycleListener() {
        return ActivityLifecycleListener.getInstance();
    }

    public String getChannel() {
        return channel;
    }
}
