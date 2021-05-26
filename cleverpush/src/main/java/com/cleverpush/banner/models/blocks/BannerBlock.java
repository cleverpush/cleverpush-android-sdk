package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerBlock {
    protected BannerBlockType type;

    protected BannerBlock() {}

    public BannerBlockType getType() { return type; }

    public static BannerBlock create(JSONObject json) throws JSONException, IllegalArgumentException {
        BannerBlockType blockType = BannerBlockType.fromString(json.getString("type"));

        switch (blockType) {
            case Text:
                return BannerTextBlock.createTextBlock(json);
            case Image:
                return BannerImageBlock.createImageBlock(json);
            case Button:
                return BannerButtonBlock.createButtonBlock(json);
            case HTML:
                return BannerHTMLBlock.createHTMLBlock(json);
            default:
                throw new RuntimeException("Not implemented");
        }
    }
}
