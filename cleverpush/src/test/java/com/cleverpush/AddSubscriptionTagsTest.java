package com.cleverpush;

import android.content.SharedPreferences;

import com.cleverpush.listener.AddTagCompletedListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddSubscriptionTagsTest {

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    AddTagCompletedListener addTagCompletedListener;

    @Mock
    JSONObject jsonObject;

    private MockWebServer mockWebServer;
    private AddSubscriptionTags addSubscriptionTags;
    private String tagIds[] = {"tagId"};

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        addSubscriptionTags = Mockito.spy(new AddSubscriptionTags("subscriptionId", "channelId", sharedPreferences, tagIds));
    }

    @Test
    void testAddSubscriptionTagWhenThereISJSONException() {
        doReturn(jsonObject).when(addSubscriptionTags).getJsonObject();

        try {
            when(jsonObject.put("channelId", "channelId")).thenThrow(new JSONException("Error"));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        addSubscriptionTags.addSubscriptionTag(addTagCompletedListener, 0);
        assertThrows(
                JSONException.class,
                () -> jsonObject.put("channelId", "channelId"),
                "Error"
        );
    }

    @Test
    void testAddSubscriptionTagWhenSubscriptionTagAlreadyHasTagAndNotNullAddTagCompletedListener() {
        Set<String> tags = new HashSet<String>();
        tags.add("tagId");

        doReturn(jsonObject).when(addSubscriptionTags).getJsonObject();
        doReturn(tags).when(addSubscriptionTags).getSubscriptionTags();

        addSubscriptionTags.addSubscriptionTag(addTagCompletedListener, 0);

        assertThat(addSubscriptionTags.tags.size()).isEqualTo(1);
        verify(addTagCompletedListener).tagAdded(0);
    }

    @Test
    void testAddSubscriptionTagWhenSubscriptionTagAlreadyHasTagAndNullAddTagCompletedListener() {
        Set<String> tags = new HashSet<String>();
        tags.add("tagId");

        doReturn(jsonObject).when(addSubscriptionTags).getJsonObject();
        doReturn(tags).when(addSubscriptionTags).getSubscriptionTags();

        addSubscriptionTags.addSubscriptionTag(null, 0);

        assertThat(addSubscriptionTags.tags.size()).isEqualTo(1);
        verify(addTagCompletedListener, never()).tagAdded(0);
    }

    @Test
    void testAddSubscriptionTagWhenSubscriptionTagsDoNotHaveTag() {
        Set<String> tags = new HashSet<String>();
        tags.add("newTagId");

        doReturn(jsonObject).when(addSubscriptionTags).getJsonObject();
        doReturn(tags).when(addSubscriptionTags).getSubscriptionTags();

        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        HttpUrl baseUrl = mockWebServer.url("/subscription/tag");
        CleverPushHttpClient.BASE_URL = baseUrl.toString().replace("/subscription/tag", "");
        MockResponse mockResponse = new MockResponse().setBody("{}").setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        addSubscriptionTags.addSubscriptionTag(addTagCompletedListener, 0);

        try {
            sleep(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(addSubscriptionTags.tags.size()).isEqualTo(2);

        try {
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getPath()).isEqualTo("/subscription/tag");
            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
            mockWebServer.shutdown();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
