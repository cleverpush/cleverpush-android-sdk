package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerImageBlock extends BannerBlock {
    private String imageUrl;
    private int scale;
    private boolean dismiss;

    private BannerImageBlock() { }

    public String getImageUrl() { return imageUrl; }

    public float getScale() { return scale; }

    public boolean dismissOnClick() { return dismiss; }

    public static BannerImageBlock createImageBlock(JSONObject json) throws JSONException {
        BannerImageBlock imageBlock = new BannerImageBlock();

        imageBlock.type = BannerBlockType.Image;
        imageBlock.imageUrl = json.getString("imageUrl");
        imageBlock.scale = json.getInt("scale");
        imageBlock.dismiss = json.getBoolean("dismiss");

        return imageBlock;
    }
}
