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

public class Banner {
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private String id;
    private String channel;
    private String name;
    private BannerType type;
    private BannerStatus status;
    private List<BannerBlock> blocks;
    private BannerBackground background;
    private Date startAt;
    private BannerDismissType dismissType;
    private int dismissTimeout;
    private BannerStopAtType stopAtType;
    private Date stopAt;
    private BannerFrequency frequency;
    private Date createdAt;

    private Banner() {}

    public String getId() { return id; }

    public String getChannel() { return channel; }

    public String getName() { return name; }

    public BannerType getType() { return type; }

    public BannerStatus getStatus() { return status; }

    public List<BannerBlock> getBlocks() { return blocks; }

    public BannerBackground getBackground() { return background; }

    public Date getStartAt() { return startAt; }

    public BannerDismissType getDismissType() { return dismissType; }

    public int getDismissTimeout() { return dismissTimeout; }

    public BannerStopAtType getStopAtType() { return stopAtType; }

    public Date getStopAt() { return stopAt; }

    public BannerFrequency getFrequency() { return frequency; }

    public Date getCreatedAt() { return createdAt; }

    public static Banner create(JSONObject json) throws JSONException {
        Banner banner = new Banner();

        banner.id = json.getString("_id");
        banner.channel = json.getString("channel");
        banner.name = json.getString("name");
        banner.type = BannerType.fromString(json.getString("type"));
        banner.status = BannerStatus.fromString(json.getString("status"));
        banner.blocks = new LinkedList<>();

        JSONArray blockArray = json.getJSONArray("blocks");
        for(int i = 0; i < blockArray.length(); ++i) {
            banner.blocks.add(BannerBlock.create(blockArray.getJSONObject(i)));
        }

        banner.background = BannerBackground.create(json.getJSONObject("background"));

        try {
            SimpleDateFormat format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
            banner.startAt = format.parse(json.getString("startAt"));
        } catch (ParseException e) {
            banner.startAt = new Date();
        }

        banner.dismissType = BannerDismissType.fromString(json.getString("dismissType"));
        banner.dismissTimeout = json.getInt("dismissTimeout");
        banner.stopAtType = BannerStopAtType.fromString(json.getString("stopAtType"));

        try {
            SimpleDateFormat format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
            banner.stopAt = json.isNull("stopAt") ? null : format.parse(json.getString("stopAt"));
        } catch (ParseException e) {
            banner.stopAt = null;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
            banner.createdAt = json.isNull("createdAt") ? null : format.parse(json.getString("createdAt"));
        } catch (ParseException e) {
            banner.createdAt = null;
        }

        banner.frequency = BannerFrequency.fromString(json.getString("frequency"));

        return banner;
    }
}
