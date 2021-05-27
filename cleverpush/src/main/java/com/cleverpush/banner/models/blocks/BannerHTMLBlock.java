package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public final class BannerHTMLBlock extends BannerBlock {
    private String url ;
    private String height ;

    private BannerHTMLBlock() { }

    public String getUrl() {
        return url;
    }

    public String getHeight() {
        return height;
    }

    public static BannerHTMLBlock createHTMLBlock(JSONObject json) {
        BannerHTMLBlock htmlBlock = new BannerHTMLBlock();
        htmlBlock.type = BannerBlockType.HTML;
        htmlBlock.url = json.optString("url");
        if (json.optString("height").equalsIgnoreCase("")) {
            htmlBlock.height = "50";
        } else {
            htmlBlock.height = json.optString("height");
        }
        return htmlBlock;
    }
}
