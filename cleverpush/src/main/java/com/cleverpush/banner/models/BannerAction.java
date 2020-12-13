package com.cleverpush.banner.models;

import com.cleverpush.banner.models.blocks.BannerBackground;
import com.cleverpush.banner.models.blocks.BannerBlock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class BannerAction {
	private String type;
	private String name;
    private String url;
    private String urlType;
    private boolean dismiss;

    private BannerAction() {}

	public String getType() { return type; }

    public String getName() { return name; }

	public String getUrl() { return url; }

	public String getUrlType() { return urlType; }

	public boolean getDismiss() { return dismiss; }

    public static BannerAction create(JSONObject json) throws JSONException {
        BannerAction banner = new BannerAction();

        if (json != null) {
			banner.type = json.optString("type");
			banner.name = json.optString("name");
			banner.url = json.optString("url");
			banner.urlType = json.optString("urlType");
			banner.dismiss = json.optBoolean("dismiss");
		}

        return banner;
    }
}
