package com.cleverpush.banner;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerAction;
import com.cleverpush.listener.ActivityInitializedListener;
import com.cleverpush.listener.AppBannerOpenedListener;
import com.cleverpush.listener.AppBannersListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppBannerModuleTest {

    private static final String TAG = "CleverPush/AppBanner";
    AppBannerModule appBannerModule;
    private MockWebServer mockWebServer;
    private String bannerResponse = "{\n" +
            "\t\"banners\": [{\n" +
            "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
            "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
            "\t\t\"name\": \"BannrOne\",\n" +
            "\t\t\"type\": \"center\",\n" +
            "\t\t\"status\": \"published\",\n" +
            "\t\t\"platformName\": \"Android\",\n" +
            "\t\t\"blocks\": [{\n" +
            "\t\t\t\"type\": \"text\",\n" +
            "\t\t\t\"text\": \"Heading Text\",\n" +
            "\t\t\t\"color\": \"#222\",\n" +
            "\t\t\t\"size\": 18,\n" +
            "\t\t\t\"alignment\": \"center\",\n" +
            "\t\t\t\"family\": \"\"\n" +
            "\t\t}, {\n" +
            "\t\t\t\"type\": \"image\",\n" +
            "\t\t\t\"imageUrl\": \"\",\n" +
            "\t\t\t\"scale\": 100,\n" +
            "\t\t\t\"dismiss\": false\n" +
            "\t\t}, {\n" +
            "\t\t\t\"type\": \"button\",\n" +
            "\t\t\t\"text\": \"Click Me!\",\n" +
            "\t\t\t\"color\": \"#FFF\",\n" +
            "\t\t\t\"background\": \"#07A\",\n" +
            "\t\t\t\"size\": 14,\n" +
            "\t\t\t\"alignment\": \"center\",\n" +
            "\t\t\t\"radius\": 0,\n" +
            "\t\t\t\"action\": {\n" +
            "\t\t\t\t\"dismiss\": true\n" +
            "\t\t\t}\n" +
            "\t\t}],\n" +
            "\t\t\"background\": {\n" +
            "\t\t\t\"imageUrl\": \"\",\n" +
            "\t\t\t\"color\": \"#ffffff\",\n" +
            "\t\t\t\"dismiss\": \"false\"\n" +
            "\t\t},\n" +
            "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
            "\t\t\"dismissType\": \"till_dismissed\",\n" +
            "\t\t\"dismissTimeout\": -1,\n" +
            "\t\t\"stopAtType\": \"forever\",\n" +
            "\t\t\"stopAt\": null,\n" +
            "\t\t\"frequency\": \"once_per_session\",\n" +
            "\t\t\"triggers\": [],\n" +
            "\t\t\"triggerType\": \"app_open\",\n" +
            "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
            "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
            "\t\t\"contentType\": \"block\",\n" +
            "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
            "\t\t\"showOnlySubscribers\": false\n" +
            "\t}]\n" +
            "}";

    private String singleBannerObject = "{\n" +
            "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
            "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
            "\t\t\"name\": \"BannrOne\",\n" +
            "\t\t\"type\": \"center\",\n" +
            "\t\t\"status\": \"published\",\n" +
            "\t\t\"platformName\": \"Android\",\n" +
            "\t\t\"blocks\": [{\n" +
            "\t\t\t\"type\": \"text\",\n" +
            "\t\t\t\"text\": \"Heading Text\",\n" +
            "\t\t\t\"color\": \"#222\",\n" +
            "\t\t\t\"size\": 18,\n" +
            "\t\t\t\"alignment\": \"center\",\n" +
            "\t\t\t\"family\": \"\"\n" +
            "\t\t}, {\n" +
            "\t\t\t\"type\": \"image\",\n" +
            "\t\t\t\"imageUrl\": \"\",\n" +
            "\t\t\t\"scale\": 100,\n" +
            "\t\t\t\"dismiss\": false\n" +
            "\t\t}, {\n" +
            "\t\t\t\"type\": \"button\",\n" +
            "\t\t\t\"text\": \"Click Me!\",\n" +
            "\t\t\t\"color\": \"#FFF\",\n" +
            "\t\t\t\"background\": \"#07A\",\n" +
            "\t\t\t\"size\": 14,\n" +
            "\t\t\t\"alignment\": \"center\",\n" +
            "\t\t\t\"radius\": 0,\n" +
            "\t\t\t\"action\": {\n" +
            "\t\t\t\t\"dismiss\": true\n" +
            "\t\t\t}\n" +
            "\t\t}],\n" +
            "\t\t\"background\": {\n" +
            "\t\t\t\"imageUrl\": \"\",\n" +
            "\t\t\t\"color\": \"#ffffff\",\n" +
            "\t\t\t\"dismiss\": \"false\"\n" +
            "\t\t},\n" +
            "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
            "\t\t\"dismissType\": \"till_dismissed\",\n" +
            "\t\t\"dismissTimeout\": -1,\n" +
            "\t\t\"stopAtType\": \"forever\",\n" +
            "\t\t\"stopAt\": \"2021-08-27T08:21:59.174Z\",\n" +
            "\t\t\"frequency\": \"once_per_session\",\n" +
            "\t\t\"triggers\": [],\n" +
            "\t\t\"triggerType\": \"app_open\",\n" +
            "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
            "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
            "\t\t\"contentType\": \"block\",\n" +
            "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
            "\t\t\"showOnlySubscribers\": false\n" +
            "\t}";

    @Mock
    Activity activity;

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    CleverPush cleverPush;

    @Mock
    SharedPreferences.Editor editor;

    @Mock
    Handler handler;

    @Mock
    JSONObject jsonObject;

    @Mock
    AppBannersListener appBannersListener;

    @Mock
    AppBannerOpenedListener appBannerOpenedListener;

    @Mock
    AppBannerPopup appBannerPopup;

    @Mock
    ActivityLifecycleListener activityLifecycleListener;

    @Mock
    Banner banner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        appBannerModule = spy(AppBannerModule.init("channelId", false, sharedPreferences, editor));
        mockWebServer = new MockWebServer();
    }

    @Test
    void testInitSession() {
        doReturn(activity).when(appBannerModule).getCurrentActivity();
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();

        appBannerModule.initSession("channelId");

        assertThat(appBannerModule.getLastSessionTimestamp()).isLessThan(System.currentTimeMillis());
        assertThat(appBannerModule.getListOfBanners()).isEqualTo(null);
        assertThat(appBannerModule.getFilteredBanners().size()).isEqualTo(0);
        verify(appBannerModule).saveSessions();
        verify(appBannerModule).startup();
    }

    @Test
    void testInitSessionWhenThereIsPopupList() {
        Collection<AppBannerPopup> filteredBanners = new ArrayList<>();
        filteredBanners.add(appBannerPopup);

        doReturn(activity).when(appBannerModule).getCurrentActivity();
        doReturn(filteredBanners).when(appBannerModule).getFilteredBanners();

        appBannerModule.initSession("channelId");

        verify(appBannerPopup).dismiss();
    }

    @Test
    void testLoadBannersWhenDevelopmentModeAndNotificationIdNull() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(true).when(cleverPush).isDevelopmentModeEnabled();
        HttpUrl baseUrl = mockWebServer.url("");
        CleverPushHttpClient.BASE_URL = baseUrl.toString();
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        appBannerModule.loadBanners(null, "channelId");

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).contains("&t=");
            assertThat(recordedRequest.getPath()).doesNotContain("&notificationId=");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testLoadBannersWhenNoDevelopmentModeAndNotificationIdNull() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(false).when(cleverPush).isDevelopmentModeEnabled();
        HttpUrl baseUrl = mockWebServer.url("");
        CleverPushHttpClient.BASE_URL = baseUrl.toString();
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        appBannerModule.loadBanners(null, "channelId");

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).doesNotContain("&t=");
            assertThat(recordedRequest.getPath()).doesNotContain("&notificationId=");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testLoadBannersWhenNoDevelopmentModeAndNotificationIdIsEmpty() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(false).when(cleverPush).isDevelopmentModeEnabled();
        HttpUrl baseUrl = mockWebServer.url("");
        CleverPushHttpClient.BASE_URL = baseUrl.toString();
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        appBannerModule.loadBanners("", "");

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).doesNotContain("&t=");
            assertThat(recordedRequest.getPath()).doesNotContain("&notificationId=");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testLoadBannersWhenNoDevelopmentModeAndNotificationIdNotNull() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(false).when(cleverPush).isDevelopmentModeEnabled();
        HttpUrl baseUrl = mockWebServer.url("");
        CleverPushHttpClient.BASE_URL = baseUrl.toString();
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        appBannerModule.loadBanners("notificationId", "channelId");

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).doesNotContain("&t=");
            assertThat(recordedRequest.getPath()).contains("&notificationId=notificationId");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testLoadBannersWhenDevelopmentModeAndNotificationIdNotNull() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(true).when(cleverPush).isDevelopmentModeEnabled();
        HttpUrl baseUrl = mockWebServer.url("");
        CleverPushHttpClient.BASE_URL = baseUrl.toString();
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        appBannerModule.loadBanners("notificationId", "channelId");

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).contains("&t=");
            assertThat(recordedRequest.getPath()).contains("&notificationId=notificationId");
            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testLoadBannersSuccess() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(true).when(cleverPush).isDevelopmentModeEnabled();
        HttpUrl baseUrl = mockWebServer.url("");
        CleverPushHttpClient.BASE_URL = baseUrl.toString();
        MockResponse mockResponse = new MockResponse().setBody(bannerResponse).setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        appBannerModule.loadBanners("notificationId", "channelId");

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertThat(appBannerModule.getListOfBanners().size()).isEqualTo(1);
    }

    @Test
    void testLoadBannersSuccessWhenThereIsBannersListeners() {
        Collection<AppBannersListener> bannersListeners = new ArrayList<>();
        bannersListeners.add(appBannersListener);

        doReturn(bannersListeners).when(appBannerModule).getBannersListeners();
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(true).when(cleverPush).isDevelopmentModeEnabled();
        HttpUrl baseUrl = mockWebServer.url("");
        CleverPushHttpClient.BASE_URL = baseUrl.toString();
        MockResponse mockResponse = new MockResponse().setBody(bannerResponse).setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        appBannerModule.loadBanners("notificationId", "channelId");

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertThat(appBannerModule.getListOfBanners().size()).isEqualTo(1);
        verify(appBannersListener).ready(any());
    }

    @Test
    void testSendBannerEvent() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        when(cleverPush.isSubscribed()).thenReturn(true);
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpUrl baseUrl = mockWebServer.url("/app-banner/event/");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/app-banner/event/", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        try {
            JSONObject bannerJson = new JSONObject(singleBannerObject);
            Banner banner = Banner.create(bannerJson);
            appBannerModule.sendBannerEvent("event", banner);

            sleep(600);

            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).isEqualTo("/app-banner/event/event");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            mockWebServer.shutdown();

        } catch (JSONException | InterruptedException | IOException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    void testSendBannerEventWhenThereIsJSONException() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(jsonObject).when(appBannerModule).getJsonObject();
        when(cleverPush.isSubscribed()).thenReturn(false);

        try {
            when(jsonObject.put("channelId", "channelId")).thenThrow(new JSONException("Error"));

            JSONObject bannerJson = new JSONObject(singleBannerObject);
            Banner banner = Banner.create(bannerJson);
            appBannerModule.sendBannerEvent("event", banner);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        assertThrows(
                JSONException.class,
                () -> jsonObject.put("channelId", "channelId"),
                "Error"
        );
    }

    @Test
    void testGetBannersWhenNotificationIdIsNullAndNoBanners() {
        appBannerModule.clearBannersListeners();
        doReturn(null).when(appBannerModule).getListOfBanners();
        appBannerModule.getBanners(appBannersListener, null);

        assertThat(appBannerModule.getBannersListeners().size()).isEqualTo(1);
        appBannerModule.clearBannersListeners();
    }

    @Test
    void testGetBannersWhenNotificationIdIsNullAndBannersAreNotNull() {
        Collection<Banner> allBanners = new LinkedList<>();
        doReturn(allBanners).when(appBannerModule).getListOfBanners();
        appBannerModule.getBanners(appBannersListener, null);

        verify(appBannersListener).ready(allBanners);
    }

    @Test
    void testGetBannersWhenNotificationIdIsNotNull() {
        Collection<Banner> allBanners = new LinkedList<>();

        doReturn(activity).when(appBannerModule).getCurrentActivity();
        doReturn(allBanners).when(appBannerModule).getListOfBanners();
        doReturn(handler).when(appBannerModule).getHandler();

        when(handler.post(any(Runnable.class))).thenAnswer((Answer) invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });

        appBannerModule.getBanners(appBannersListener, "notificationId");

        assertThat(appBannerModule.getBannersListeners().size()).isEqualTo(1);
        verify(appBannerModule).loadBanners("notificationId", "channelId");
    }

    @Test
    void testIsBannerTimeAllowedWhenBannerStopAtNotSpecificTime() {
        JSONObject bannerJson = null;
        String singleBannerObject = "{\n" +
                "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
                "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
                "\t\t\"name\": \"BannrOne\",\n" +
                "\t\t\"type\": \"center\",\n" +
                "\t\t\"status\": \"published\",\n" +
                "\t\t\"platformName\": \"Android\",\n" +
                "\t\t\"blocks\": [{\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"text\": \"Heading Text\",\n" +
                "\t\t\t\"color\": \"#222\",\n" +
                "\t\t\t\"size\": 18,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"family\": \"\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"image\",\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"scale\": 100,\n" +
                "\t\t\t\"dismiss\": false\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"button\",\n" +
                "\t\t\t\"text\": \"Click Me!\",\n" +
                "\t\t\t\"color\": \"#FFF\",\n" +
                "\t\t\t\"background\": \"#07A\",\n" +
                "\t\t\t\"size\": 14,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"radius\": 0,\n" +
                "\t\t\t\"action\": {\n" +
                "\t\t\t\t\"dismiss\": true\n" +
                "\t\t\t}\n" +
                "\t\t}],\n" +
                "\t\t\"background\": {\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"color\": \"#ffffff\",\n" +
                "\t\t\t\"dismiss\": \"false\"\n" +
                "\t\t},\n" +
                "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
                "\t\t\"dismissType\": \"till_dismissed\",\n" +
                "\t\t\"dismissTimeout\": -1,\n" +
                "\t\t\"stopAtType\": \"forever\",\n" +
                "\t\t\"stopAt\": \"\",\n" +
                "\t\t\"frequency\": \"once_per_session\",\n" +
                "\t\t\"triggers\": [],\n" +
                "\t\t\"triggerType\": \"app_open\",\n" +
                "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
                "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
                "\t\t\"contentType\": \"block\",\n" +
                "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"showOnlySubscribers\": false\n" +
                "\t}";
        try {
            bannerJson = new JSONObject(singleBannerObject);
            Banner banner = Banner.create(bannerJson);
            assertThat(appBannerModule.isBannerTimeAllowed(banner)).isTrue();
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    void testIsBannerTimeAllowedWhenStopAtIsNull() {
        JSONObject bannerJson = null;
        String singleBannerObject = "{\n" +
                "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
                "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
                "\t\t\"name\": \"BannrOne\",\n" +
                "\t\t\"type\": \"center\",\n" +
                "\t\t\"status\": \"published\",\n" +
                "\t\t\"platformName\": \"Android\",\n" +
                "\t\t\"blocks\": [{\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"text\": \"Heading Text\",\n" +
                "\t\t\t\"color\": \"#222\",\n" +
                "\t\t\t\"size\": 18,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"family\": \"\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"image\",\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"scale\": 100,\n" +
                "\t\t\t\"dismiss\": false\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"button\",\n" +
                "\t\t\t\"text\": \"Click Me!\",\n" +
                "\t\t\t\"color\": \"#FFF\",\n" +
                "\t\t\t\"background\": \"#07A\",\n" +
                "\t\t\t\"size\": 14,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"radius\": 0,\n" +
                "\t\t\t\"action\": {\n" +
                "\t\t\t\t\"dismiss\": true\n" +
                "\t\t\t}\n" +
                "\t\t}],\n" +
                "\t\t\"background\": {\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"color\": \"#ffffff\",\n" +
                "\t\t\t\"dismiss\": \"false\"\n" +
                "\t\t},\n" +
                "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
                "\t\t\"dismissType\": \"till_dismissed\",\n" +
                "\t\t\"dismissTimeout\": -1,\n" +
                "\t\t\"stopAtType\": \"specific_time\",\n" +
                "\t\t\"stopAt\": null,\n" +
                "\t\t\"frequency\": \"once_per_session\",\n" +
                "\t\t\"triggers\": [],\n" +
                "\t\t\"triggerType\": \"app_open\",\n" +
                "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
                "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
                "\t\t\"contentType\": \"block\",\n" +
                "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"showOnlySubscribers\": false\n" +
                "\t}";
        try {
            bannerJson = new JSONObject(singleBannerObject);
            Banner banner = Banner.create(bannerJson);
            assertThat(appBannerModule.isBannerTimeAllowed(banner)).isTrue();
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    void testIsBannerTimeAllowedWhenStopAtAfterCurrentTime() {
        JSONObject bannerJson = null;
        String singleBannerObject = "{\n" +
                "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
                "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
                "\t\t\"name\": \"BannrOne\",\n" +
                "\t\t\"type\": \"center\",\n" +
                "\t\t\"status\": \"published\",\n" +
                "\t\t\"platformName\": \"Android\",\n" +
                "\t\t\"blocks\": [{\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"text\": \"Heading Text\",\n" +
                "\t\t\t\"color\": \"#222\",\n" +
                "\t\t\t\"size\": 18,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"family\": \"\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"image\",\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"scale\": 100,\n" +
                "\t\t\t\"dismiss\": false\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"button\",\n" +
                "\t\t\t\"text\": \"Click Me!\",\n" +
                "\t\t\t\"color\": \"#FFF\",\n" +
                "\t\t\t\"background\": \"#07A\",\n" +
                "\t\t\t\"size\": 14,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"radius\": 0,\n" +
                "\t\t\t\"action\": {\n" +
                "\t\t\t\t\"dismiss\": true\n" +
                "\t\t\t}\n" +
                "\t\t}],\n" +
                "\t\t\"background\": {\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"color\": \"#ffffff\",\n" +
                "\t\t\t\"dismiss\": \"false\"\n" +
                "\t\t},\n" +
                "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
                "\t\t\"dismissType\": \"till_dismissed\",\n" +
                "\t\t\"dismissTimeout\": -1,\n" +
                "\t\t\"stopAtType\": \"specific_time\",\n" +
                "\t\t\"stopAt\": \"2025-09-15T023:21:59.174Z\",\n" +
                "\t\t\"frequency\": \"once_per_session\",\n" +
                "\t\t\"triggers\": [],\n" +
                "\t\t\"triggerType\": \"app_open\",\n" +
                "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
                "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
                "\t\t\"contentType\": \"block\",\n" +
                "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"showOnlySubscribers\": false\n" +
                "\t}";
        try {
            bannerJson = new JSONObject(singleBannerObject);
            Banner banner = Banner.create(bannerJson);
            assertThat(appBannerModule.isBannerTimeAllowed(banner)).isTrue();
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    void testIsBannerTimeAllowedForFalse() {
        JSONObject bannerJson = null;
        String singleBannerObject = "{\n" +
                "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
                "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
                "\t\t\"name\": \"BannrOne\",\n" +
                "\t\t\"type\": \"center\",\n" +
                "\t\t\"status\": \"published\",\n" +
                "\t\t\"platformName\": \"Android\",\n" +
                "\t\t\"blocks\": [{\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"text\": \"Heading Text\",\n" +
                "\t\t\t\"color\": \"#222\",\n" +
                "\t\t\t\"size\": 18,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"family\": \"\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"image\",\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"scale\": 100,\n" +
                "\t\t\t\"dismiss\": false\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"button\",\n" +
                "\t\t\t\"text\": \"Click Me!\",\n" +
                "\t\t\t\"color\": \"#FFF\",\n" +
                "\t\t\t\"background\": \"#07A\",\n" +
                "\t\t\t\"size\": 14,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"radius\": 0,\n" +
                "\t\t\t\"action\": {\n" +
                "\t\t\t\t\"dismiss\": true\n" +
                "\t\t\t}\n" +
                "\t\t}],\n" +
                "\t\t\"background\": {\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"color\": \"#ffffff\",\n" +
                "\t\t\t\"dismiss\": \"false\"\n" +
                "\t\t},\n" +
                "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
                "\t\t\"dismissType\": \"till_dismissed\",\n" +
                "\t\t\"dismissTimeout\": -1,\n" +
                "\t\t\"stopAtType\": \"specific_time\",\n" +
                "\t\t\"stopAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"frequency\": \"once_per_session\",\n" +
                "\t\t\"triggers\": [],\n" +
                "\t\t\"triggerType\": \"app_open\",\n" +
                "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
                "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
                "\t\t\"contentType\": \"block\",\n" +
                "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"showOnlySubscribers\": false\n" +
                "\t}";
        try {
            bannerJson = new JSONObject(singleBannerObject);
            Banner banner = Banner.create(bannerJson);
            assertThat(appBannerModule.isBannerTimeAllowed(banner)).isFalse();
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    void testEnableBanners() {
        Collection<AppBannerPopup> pendingBanners = new ArrayList<>();
        pendingBanners.add(appBannerPopup);

        doReturn(pendingBanners).when(appBannerModule).getPendingBanners();
        doNothing().when(appBannerModule).scheduledFilteredBanners();

        appBannerModule.enableBanners();

        assertThat(appBannerModule.getFilteredBanners().size()).isEqualTo(1);
        assertThat(appBannerModule.getPendingBanners().size()).isEqualTo(0);
        verify(appBannerModule).scheduledFilteredBanners();
    }

    @Test
    void testDisableBanners() {
        appBannerModule.disableBanners();

        verify(appBannerModule).disableBanners();
        assertThat(appBannerModule.getPendingBanners().size()).isEqualTo(0);
    }

    @Test
    void testShowBannerByIdWhenAppBannerDisabled() {
        JSONObject bannerJson = null;

        try {
            bannerJson = new JSONObject(singleBannerObject);
            Banner banner = Banner.create(bannerJson);
            Collection<Banner> allBanners = new LinkedList<>();
            allBanners.add(banner);
            doReturn(allBanners).when(appBannerModule).getListOfBanners();
            doReturn(activityLifecycleListener).when(appBannerModule).getActivityLifecycleListener();
            doReturn(activity).when(appBannerModule).getCurrentActivity();
            doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
            doReturn(true).when(cleverPush).isAppBannersDisabled();

            Answer<Void> appBannersListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    AppBannersListener callback = (AppBannersListener) invocation.getArguments()[0];
                    callback.ready(allBanners);
                    return null;
                }
            };
            Answer<Void> activityInitializedListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    ActivityInitializedListener callback = (ActivityInitializedListener) invocation.getArguments()[0];
                    callback.initialized();
                    return null;
                }
            };
            doAnswer(appBannersListenerAnswer).when(appBannerModule).getBanners(any(AppBannersListener.class));
            doAnswer(activityInitializedListenerAnswer).when(activityLifecycleListener).setActivityInitializedListener(any(ActivityInitializedListener.class));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        appBannerModule.showBanner("xuMpMKmoKhAZ8XRKr", null);

        assertThat(appBannerModule.getPendingBanners().size()).isEqualTo(1);
    }

    @Test
    void testShowBannerByIdWhenAppBannerIsNotDisabled() {
        JSONObject bannerJson = null;

        try {
            bannerJson = new JSONObject(singleBannerObject);
            Banner banner = Banner.create(bannerJson);
            Collection<Banner> allBanners = new LinkedList<>();
            allBanners.add(banner);

            doReturn(allBanners).when(appBannerModule).getListOfBanners();
            doReturn(activityLifecycleListener).when(appBannerModule).getActivityLifecycleListener();
            doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
            doReturn(false).when(cleverPush).isAppBannersDisabled();
            doReturn(handler).when(appBannerModule).getHandler();
            doReturn(appBannerPopup).when(appBannerModule).getAppBannerPopup(banner);
            doReturn("channelId").when(appBannerModule).getChannel();
            doNothing().when(appBannerModule).showBanner(appBannerPopup, true, null, null);

            Answer<Void> appBannersListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    AppBannersListener callback = (AppBannersListener) invocation.getArguments()[0];
                    callback.ready(allBanners);
                    return null;
                }
            };

            Answer<Void> activityInitializedListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    ActivityInitializedListener callback = (ActivityInitializedListener) invocation.getArguments()[0];
                    callback.initialized();
                    return null;
                }
            };

            doAnswer(appBannersListenerAnswer).when(appBannerModule).getBanners(any(AppBannersListener.class));
            doAnswer(activityInitializedListenerAnswer).when(activityLifecycleListener).setActivityInitializedListener(any(ActivityInitializedListener.class));

            when(handler.post(any(Runnable.class))).thenAnswer((Answer) invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            });

            appBannerModule.showBanner("xuMpMKmoKhAZ8XRKr", null);

            verify(appBannerModule).showBanner(appBannerPopup, true, null, null);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    void testShowBannerWhenBannerFrequencyIsOnce() {
        doNothing().when(appBannerPopup).init();
        doNothing().when(appBannerPopup).show();
        doNothing().when(appBannerModule).bannerIsShown("xuMpMKmoKhAZ8XRKr");
        JSONObject bannerJson = null;
        String bannerObject = "{\n" +
                "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
                "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
                "\t\t\"name\": \"BannrOne\",\n" +
                "\t\t\"type\": \"center\",\n" +
                "\t\t\"status\": \"published\",\n" +
                "\t\t\"platformName\": \"Android\",\n" +
                "\t\t\"blocks\": [{\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"text\": \"Heading Text\",\n" +
                "\t\t\t\"color\": \"#222\",\n" +
                "\t\t\t\"size\": 18,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"family\": \"\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"image\",\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"scale\": 100,\n" +
                "\t\t\t\"dismiss\": false\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"button\",\n" +
                "\t\t\t\"text\": \"Click Me!\",\n" +
                "\t\t\t\"color\": \"#FFF\",\n" +
                "\t\t\t\"background\": \"#07A\",\n" +
                "\t\t\t\"size\": 14,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"radius\": 0,\n" +
                "\t\t\t\"action\": {\n" +
                "\t\t\t\t\"dismiss\": true\n" +
                "\t\t\t}\n" +
                "\t\t}],\n" +
                "\t\t\"background\": {\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"color\": \"#ffffff\",\n" +
                "\t\t\t\"dismiss\": \"false\"\n" +
                "\t\t},\n" +
                "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
                "\t\t\"dismissType\": \"till_dismissed\",\n" +
                "\t\t\"dismissTimeout\": -1,\n" +
                "\t\t\"stopAtType\": \"forever\",\n" +
                "\t\t\"stopAt\": \"2021-08-27T08:21:59.174Z\",\n" +
                "\t\t\"frequency\": \"once\",\n" +
                "\t\t\"triggers\": [],\n" +
                "\t\t\"triggerType\": \"app_open\",\n" +
                "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
                "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
                "\t\t\"contentType\": \"block\",\n" +
                "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"showOnlySubscribers\": false\n" +
                "\t}";

        try {
            bannerJson = new JSONObject(bannerObject);
            Banner banner = Banner.create(bannerJson);
            when(appBannerPopup.getData()).thenReturn(banner);
            doReturn(editor).when(sharedPreferences).edit();
            doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
            when(cleverPush.isSubscribed()).thenReturn(true);
            doReturn(activityLifecycleListener).when(appBannerModule).getActivityLifecycleListener();
            ;

            Answer<Void> activityInitializedListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    ActivityInitializedListener callback = (ActivityInitializedListener) invocation.getArguments()[0];
                    callback.initialized();
                    return null;
                }
            };
            doAnswer(activityInitializedListenerAnswer).when(activityLifecycleListener).setActivityInitializedListener(any(ActivityInitializedListener.class));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        appBannerModule.showBanner(appBannerPopup);

        verify(appBannerModule).bannerIsShown("xuMpMKmoKhAZ8XRKr");
        verify(appBannerModule).sendBannerEvent("delivered", appBannerPopup.getData());
    }

    @Test
    void testShowBannerWhenBannerDismissTypeIsTimeout() {
        doNothing().when(appBannerPopup).init();
        doNothing().when(appBannerPopup).show();
        JSONObject bannerJson = null;
        String bannerObject = "{\n" +
                "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
                "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
                "\t\t\"name\": \"BannrOne\",\n" +
                "\t\t\"type\": \"center\",\n" +
                "\t\t\"status\": \"published\",\n" +
                "\t\t\"platformName\": \"Android\",\n" +
                "\t\t\"blocks\": [{\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"text\": \"Heading Text\",\n" +
                "\t\t\t\"color\": \"#222\",\n" +
                "\t\t\t\"size\": 18,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"family\": \"\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"image\",\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"scale\": 100,\n" +
                "\t\t\t\"dismiss\": false\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"button\",\n" +
                "\t\t\t\"text\": \"Click Me!\",\n" +
                "\t\t\t\"color\": \"#FFF\",\n" +
                "\t\t\t\"background\": \"#07A\",\n" +
                "\t\t\t\"size\": 14,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"radius\": 0,\n" +
                "\t\t\t\"action\": {\n" +
                "\t\t\t\t\"dismiss\": true\n" +
                "\t\t\t}\n" +
                "\t\t}],\n" +
                "\t\t\"background\": {\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"color\": \"#ffffff\",\n" +
                "\t\t\t\"dismiss\": \"false\"\n" +
                "\t\t},\n" +
                "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
                "\t\t\"dismissType\": \"timeout\",\n" +
                "\t\t\"dismissTimeout\": -1,\n" +
                "\t\t\"stopAtType\": \"forever\",\n" +
                "\t\t\"stopAt\": \"2021-08-27T08:21:59.174Z\",\n" +
                "\t\t\"frequency\": \"once_per_session\",\n" +
                "\t\t\"triggers\": [],\n" +
                "\t\t\"triggerType\": \"app_open\",\n" +
                "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
                "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
                "\t\t\"contentType\": \"block\",\n" +
                "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"showOnlySubscribers\": false\n" +
                "\t}";

        try {
            bannerJson = new JSONObject(bannerObject);
            Banner banner = Banner.create(bannerJson);
            when(appBannerPopup.getData()).thenReturn(banner);
            when(sharedPreferences.edit()).thenReturn(editor);
            doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
            doReturn(handler).when(appBannerModule).getHandler();
            when(cleverPush.isSubscribed()).thenReturn(true);
            when(handler.postDelayed(any(Runnable.class), anyLong())).thenAnswer((Answer) invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            });
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        appBannerModule.showBanner(appBannerPopup);

        verify(appBannerPopup).dismiss();
        verify(appBannerModule).sendBannerEvent("delivered", appBannerPopup.getData());
    }

    @Test
    void testShowBannerWhenBannerForSetOpenedListener() {
        BannerAction bannerAction = null;
        doNothing().when(appBannerPopup).init();
        doNothing().when(appBannerPopup).show();
        doNothing().when(appBannerModule).bannerIsShown("xuMpMKmoKhAZ8XRKr");
        JSONObject bannerJson = null;
        String bannerObject = "{\n" +
                "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
                "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
                "\t\t\"name\": \"BannrOne\",\n" +
                "\t\t\"type\": \"center\",\n" +
                "\t\t\"status\": \"published\",\n" +
                "\t\t\"platformName\": \"Android\",\n" +
                "\t\t\"blocks\": [{\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"text\": \"Heading Text\",\n" +
                "\t\t\t\"color\": \"#222\",\n" +
                "\t\t\t\"size\": 18,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"family\": \"\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"image\",\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"scale\": 100,\n" +
                "\t\t\t\"dismiss\": false\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"button\",\n" +
                "\t\t\t\"text\": \"Click Me!\",\n" +
                "\t\t\t\"color\": \"#FFF\",\n" +
                "\t\t\t\"background\": \"#07A\",\n" +
                "\t\t\t\"size\": 14,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"radius\": 0,\n" +
                "\t\t\t\"action\": {\n" +
                "\t\t\t\t\"dismiss\": true\n" +
                "\t\t\t}\n" +
                "\t\t}],\n" +
                "\t\t\"background\": {\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"color\": \"#ffffff\",\n" +
                "\t\t\t\"dismiss\": \"false\"\n" +
                "\t\t},\n" +
                "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
                "\t\t\"dismissType\": \"till_dismissed\",\n" +
                "\t\t\"dismissTimeout\": -1,\n" +
                "\t\t\"stopAtType\": \"forever\",\n" +
                "\t\t\"stopAt\": \"2021-08-27T08:21:59.174Z\",\n" +
                "\t\t\"frequency\": \"once\",\n" +
                "\t\t\"triggers\": [],\n" +
                "\t\t\"triggerType\": \"app_open\",\n" +
                "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
                "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
                "\t\t\"contentType\": \"block\",\n" +
                "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"showOnlySubscribers\": false\n" +
                "\t}";

        try {
            bannerJson = new JSONObject(bannerObject);
            Banner banner = Banner.create(bannerJson);
            JSONObject action = new JSONObject("{\n" +
                    "\t\t\t\"dismiss\": true,\n" +
                    "\t\t  \t\"type\" : \"subscribe\";\n" +
                    "\t\t}");
            bannerAction = BannerAction.create(action);

            when(appBannerPopup.getData()).thenReturn(banner);
            doReturn(activityLifecycleListener).when(appBannerModule).getActivityLifecycleListener();
            doReturn(editor).when(sharedPreferences).edit();
            doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
            doReturn(appBannerOpenedListener).when(cleverPush).getAppBannerOpenedListener();
            when(cleverPush.isSubscribed()).thenReturn(true);

            BannerAction finalBannerAction = bannerAction;
            Answer<Void> appBannerOpenedListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    AppBannerOpenedListener callback = (AppBannerOpenedListener) invocation.getArguments()[0];
                    callback.opened(finalBannerAction);
                    return null;
                }
            };

            Answer<Void> activityInitializedListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    ActivityInitializedListener callback = (ActivityInitializedListener) invocation.getArguments()[0];
                    callback.initialized();
                    return null;
                }
            };
            doAnswer(appBannerOpenedListenerAnswer).when(appBannerPopup).setOpenedListener(any(AppBannerOpenedListener.class));
            doAnswer(activityInitializedListenerAnswer).when(activityLifecycleListener).setActivityInitializedListener(any(ActivityInitializedListener.class));

        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        appBannerModule.showBanner(appBannerPopup);

        verify(appBannerModule).bannerIsShown("xuMpMKmoKhAZ8XRKr");
        verify(appBannerModule).sendBannerEvent("delivered", appBannerPopup.getData());
        verify(appBannerOpenedListener).opened(bannerAction);
        verify(cleverPush).subscribe();
    }

    @Test
    void testShowBannerWhenBannerForSetOpenedListenerWhenThereIsNoAppBannerOpenedListenerAndSubscribeAction() {
        BannerAction bannerAction = null;
        doNothing().when(appBannerPopup).init();
        doNothing().when(appBannerPopup).show();
        doNothing().when(appBannerModule).bannerIsShown("xuMpMKmoKhAZ8XRKr");
        JSONObject bannerJson = null;
        String bannerObject = "{\n" +
                "\t\t\"_id\": \"xuMpMKmoKhAZ8XRKr\",\n" +
                "\t\t\"channel\": \"hrPmxqynN7NJ7qtAz\",\n" +
                "\t\t\"name\": \"BannrOne\",\n" +
                "\t\t\"type\": \"center\",\n" +
                "\t\t\"status\": \"published\",\n" +
                "\t\t\"platformName\": \"Android\",\n" +
                "\t\t\"blocks\": [{\n" +
                "\t\t\t\"type\": \"text\",\n" +
                "\t\t\t\"text\": \"Heading Text\",\n" +
                "\t\t\t\"color\": \"#222\",\n" +
                "\t\t\t\"size\": 18,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"family\": \"\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"image\",\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"scale\": 100,\n" +
                "\t\t\t\"dismiss\": false\n" +
                "\t\t}, {\n" +
                "\t\t\t\"type\": \"button\",\n" +
                "\t\t\t\"text\": \"Click Me!\",\n" +
                "\t\t\t\"color\": \"#FFF\",\n" +
                "\t\t\t\"background\": \"#07A\",\n" +
                "\t\t\t\"size\": 14,\n" +
                "\t\t\t\"alignment\": \"center\",\n" +
                "\t\t\t\"radius\": 0,\n" +
                "\t\t\t\"action\": {\n" +
                "\t\t\t\t\"dismiss\": true\n" +
                "\t\t\t}\n" +
                "\t\t}],\n" +
                "\t\t\"background\": {\n" +
                "\t\t\t\"imageUrl\": \"\",\n" +
                "\t\t\t\"color\": \"#ffffff\",\n" +
                "\t\t\t\"dismiss\": \"false\"\n" +
                "\t\t},\n" +
                "\t\t\"startAt\": \"2021-08-27T08:10:11.713Z\",\n" +
                "\t\t\"dismissType\": \"till_dismissed\",\n" +
                "\t\t\"dismissTimeout\": -1,\n" +
                "\t\t\"stopAtType\": \"forever\",\n" +
                "\t\t\"stopAt\": \"2021-08-27T08:21:59.174Z\",\n" +
                "\t\t\"frequency\": \"once\",\n" +
                "\t\t\"triggers\": [],\n" +
                "\t\t\"triggerType\": \"app_open\",\n" +
                "\t\t\"updatedAt\": \"2021-08-27T08:17:35.073Z\",\n" +
                "\t\t\"content\": \"<!-- Paste your code here -->\",\n" +
                "\t\t\"contentType\": \"block\",\n" +
                "\t\t\"createdAt\": \"2021-08-27T08:11:59.174Z\",\n" +
                "\t\t\"showOnlySubscribers\": false\n" +
                "\t}";

        try {
            bannerJson = new JSONObject(bannerObject);
            Banner banner = Banner.create(bannerJson);
            JSONObject action = new JSONObject("{\n" +
                    "\t\t\t\"dismiss\": true,\n" +
                    "\t\t}");
            bannerAction = BannerAction.create(action);

            when(appBannerPopup.getData()).thenReturn(banner);
            doReturn(editor).when(sharedPreferences).edit();
            doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
            doReturn(activityLifecycleListener).when(appBannerModule).getActivityLifecycleListener();
            doReturn(null).when(cleverPush).getAppBannerOpenedListener();
            when(cleverPush.isSubscribed()).thenReturn(true);

            BannerAction finalBannerAction = bannerAction;

            Answer<Void> appBannerOpenedListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    AppBannerOpenedListener callback = (AppBannerOpenedListener) invocation.getArguments()[0];
                    callback.opened(finalBannerAction);
                    return null;
                }
            };

            Answer<Void> activityInitializedListenerAnswer = new Answer<Void>() {
                public Void answer(InvocationOnMock invocation) {
                    ActivityInitializedListener callback = (ActivityInitializedListener) invocation.getArguments()[0];
                    callback.initialized();
                    return null;
                }
            };

            doAnswer(appBannerOpenedListenerAnswer).when(appBannerPopup).setOpenedListener(any(AppBannerOpenedListener.class));
            doAnswer(activityInitializedListenerAnswer).when(activityLifecycleListener).setActivityInitializedListener(any(ActivityInitializedListener.class));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        appBannerModule.showBanner(appBannerPopup);

        verify(appBannerModule).bannerIsShown("xuMpMKmoKhAZ8XRKr");
        verify(appBannerModule).sendBannerEvent("delivered", appBannerPopup.getData());
        verify(appBannerOpenedListener, never()).opened(bannerAction);
        verify(cleverPush, never()).subscribe();
    }

    @Test
    void testScheduledFilteredBannersWhenAppBannersDisabled() {
        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(true).when(cleverPush).isAppBannersDisabled();

        appBannerModule.clearPendingBanners();
        appBannerModule.scheduledFilteredBanners();

        assertThat(appBannerModule.getPendingBanners().size()).isEqualTo(1);
        assertThat(appBannerModule.getFilteredBanners().size()).isEqualTo(0);
    }

    @Test
    void testScheduledFilteredBannersWhenAppBannersIsBeforeCurrentTimeAndNoDelay() {
        Collection<AppBannerPopup> filteredBanners = new ArrayList<>();
        filteredBanners.add(appBannerPopup);
        Date yesterDay = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));

        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(false).when(cleverPush).isAppBannersDisabled();
        doReturn(filteredBanners).when(appBannerModule).getFilteredBanners();
        doReturn(banner).when(appBannerPopup).getData();
        doReturn(yesterDay).when(banner).getStartAt();
        doReturn(handler).when(appBannerModule).getHandler();
        when(handler.post(any(Runnable.class))).thenAnswer((Answer) invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        doNothing().when(appBannerModule).showBanner(appBannerPopup);

        appBannerModule.scheduledFilteredBanners();

        verify(appBannerModule).showBanner(appBannerPopup);
    }

    @Test
    void testScheduledFilteredBannersWhenAppBannersIsBeforeCurrentTimeAndDelay() {
        Collection<AppBannerPopup> filteredBanners = new ArrayList<>();
        filteredBanners.add(appBannerPopup);
        Date yesterDay = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));

        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(false).when(cleverPush).isAppBannersDisabled();
        doReturn(filteredBanners).when(appBannerModule).getFilteredBanners();
        doReturn(banner).when(appBannerPopup).getData();
        doReturn(yesterDay).when(banner).getStartAt();
        doReturn(5).when(banner).getDelaySeconds();
        doReturn(handler).when(appBannerModule).getHandler();
        when(handler.postDelayed(any(Runnable.class), anyLong())).thenAnswer((Answer) invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        doNothing().when(appBannerModule).showBanner(appBannerPopup);

        appBannerModule.scheduledFilteredBanners();

        verify(appBannerModule).showBanner(appBannerPopup);
    }

    @Test
    void testScheduledFilteredBannersWhenAppBannersIsAfterCurrentTime() {
        Collection<AppBannerPopup> filteredBanners = new ArrayList<>();
        filteredBanners.add(appBannerPopup);
        Date yesterDay = new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24));

        doReturn(cleverPush).when(appBannerModule).getCleverPushInstance();
        doReturn(false).when(cleverPush).isAppBannersDisabled();
        doReturn(filteredBanners).when(appBannerModule).getFilteredBanners();
        doReturn(banner).when(appBannerPopup).getData();
        doReturn(yesterDay).when(banner).getStartAt();
        doReturn(5).when(banner).getDelaySeconds();
        doReturn(handler).when(appBannerModule).getHandler();
        when(handler.postDelayed(any(Runnable.class), anyLong())).thenAnswer((Answer) invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        doNothing().when(appBannerModule).showBanner(appBannerPopup);

        appBannerModule.scheduledFilteredBanners();

        verify(appBannerModule).showBanner(appBannerPopup);
    }

    @AfterEach
    public void validate() {
        validateMockitoUsage();
    }
}
