package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public final class BannerTextBlock extends BannerBlock {
    private String text;
    private String color;
    private int size;
    private Alignment alignment;
    private String family = null;

    private BannerTextBlock() { }

    public String getText() { return text; }

    public String getColor() { return color; }

    public int getSize() { return size; }

    public Alignment getAlignment() { return alignment; }

    public String getFamily() {
        return family;
    }

    public static BannerTextBlock createTextBlock(JSONObject json) throws JSONException {
        BannerTextBlock textBlock = new BannerTextBlock();

        textBlock.type = BannerBlockType.Text;
        textBlock.text = json.getString("text");
        textBlock.color = json.getString("color");
        textBlock.size = json.getInt("size");
        textBlock.alignment = Alignment.fromString(json.getString("alignment"));
        if (json.has("family")) {
            textBlock.family = json.getString("family");
        }
        return textBlock;
    }
}
