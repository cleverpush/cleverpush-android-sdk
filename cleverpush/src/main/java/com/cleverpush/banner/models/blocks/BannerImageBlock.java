package com.cleverpush.banner.models.blocks;

import com.cleverpush.banner.models.BannerAction;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerImageBlock extends BannerBlock {
    private String imageUrl;
    private int scale;
    private boolean dismiss;
    private BannerAction action;

    private BannerImageBlock() { }

    public String getImageUrl() { return imageUrl; }

    public float getScale() { return scale; }

    public boolean dismissOnClick() { return dismiss; }

    public BannerAction getAction() {
        return action;
    }

    public static BannerImageBlock createImageBlock(JSONObject json) throws JSONException {
        BannerImageBlock imageBlock = new BannerImageBlock();

        imageBlock.type = BannerBlockType.Image;
        if (json.getString("imageUrl") != null) {
            imageBlock.imageUrl = json.getString("imageUrl");
        }
        imageBlock.scale = json.getInt("scale");
        imageBlock.dismiss = json.getBoolean("dismiss");
        if (json.has("action")) {
            imageBlock.action = BannerAction.create(json.getJSONObject("action"));
        }

        return imageBlock;
    }
}
