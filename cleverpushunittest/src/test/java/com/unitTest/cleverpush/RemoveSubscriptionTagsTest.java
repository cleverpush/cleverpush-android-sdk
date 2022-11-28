package com.unitTest.cleverpush;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.cleverpush.RemoveSubscriptionTags;
import com.cleverpush.listener.RemoveTagCompletedListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

class RemoveSubscriptionTagsTest {

    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    RemoveTagCompletedListener removeTagCompletedListener;

    @Mock
    JSONObject jsonObject;

    private RemoveSubscriptionTags removeSubscriptionTags;
    String[] tagIds = {"tagId"};

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        removeSubscriptionTags = Mockito.spy(new RemoveSubscriptionTags("subscriptionId", "channelId", sharedPreferences, tagIds));
    }

    @Test
    void testRemoveSubscriptionTagWhenThereISJSONException() {
        doReturn(jsonObject).when(removeSubscriptionTags).getJsonObject();
        try {
            when(jsonObject.put("channelId", "channelId")).thenThrow(new JSONException("Error"));
        } catch (JSONException exception) {
            exception.printStackTrace();
        }

        removeSubscriptionTags.removeSubscriptionTag(removeTagCompletedListener, 0);

        assertThrows(
                JSONException.class,
                () -> jsonObject.put("channelId", "channelId"),
                "Error"
        );
    }

    @Test
    void testRemoveSubscriptionTag() {
        Set<String> tags = new HashSet<String>();
        tags.add("tagId");

        doReturn(jsonObject).when(removeSubscriptionTags).getJsonObject();
        doReturn(tags).when(removeSubscriptionTags).getSubscriptionTags();

        removeSubscriptionTags.removeSubscriptionTag(removeTagCompletedListener, 0);

        assertThat(removeSubscriptionTags.getSubscriptionTags().size()).isEqualTo(0);
    }
}
