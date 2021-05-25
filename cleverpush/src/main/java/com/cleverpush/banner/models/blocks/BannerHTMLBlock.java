package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public final class BannerHTMLBlock extends BannerBlock {
    private String html;
    private String url ;
    private String height ;

    private BannerHTMLBlock() { }



    public String getHtml() {
        return html;
    }

    public String getUrl() {
        return url;
    }

    public String getHeight() {
        return height;
    }

    public static BannerHTMLBlock createHTMLBlock(JSONObject json) throws JSONException {
        BannerHTMLBlock htmlBlock = new BannerHTMLBlock();
        htmlBlock.type = BannerBlockType.HTML;
        htmlBlock.html = json.getString("html");
        htmlBlock.url = json.getString("url");
        htmlBlock.height = json.getString("height");
        return htmlBlock;
    }
}
