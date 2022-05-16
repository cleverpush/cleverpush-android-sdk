package com.cleverpush.banner.models.blocks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public final class BannerTextBlock extends BannerBlock {
    private String text;
    private String color;
    private int size;
    private Alignment alignment;
    private String family = null;
    private List<BannerBlockScreen> blockScreens;

    private BannerTextBlock() {
    }

    public String getText() {
        return text;
    }

    public String getColor() {
        return color;
    }

    public int getSize() {
        return size;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public String getFamily() {
        return family;
    }

    public List<BannerBlockScreen> getBlocks() {
        return blockScreens;
    }



    public static BannerTextBlock createTextBlock(JSONObject json) throws JSONException {
        BannerTextBlock textBlock = new BannerTextBlock();

        textBlock.type = BannerBlockType.Text;
        textBlock.text = json.optString("text");
        textBlock.color = json.optString("color");
        textBlock.size = json.getInt("size");
        textBlock.alignment = Alignment.fromString(json.getString("alignment"));
        if (json.has("family") && !json.optString("family").isEmpty()) {
            textBlock.family = json.optString("family");
        }
        textBlock.blockScreens = new LinkedList<>();

        if (json.has("screens")) {
            JSONArray screens = json.getJSONArray("screens");
            for (int i = 0; i < screens.length(); ++i) {
                textBlock.blockScreens.add(BannerBlockScreen.create(screens.getJSONObject(i)));
            }
        }
        return textBlock;
    }
}
