package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerButtonBlock extends BannerBlock {
    private String text;
    private String color;
    private String background;
    private int size;
    private Alignment alignment;
    private boolean dismiss;
    private int radius;

    private BannerButtonBlock() { }

    public String getText() { return text; }

    public String getColor() { return color; }

    public String getBackground() { return background; }

    public int getSize() { return size; }

    public Alignment getAlignment() { return alignment; }

    public boolean dismissOnClick() { return dismiss; }

    public int getRadius() { return radius; }

    public static BannerButtonBlock createButtonBlock(JSONObject json) throws JSONException {
        BannerButtonBlock buttonBlock = new BannerButtonBlock();

        buttonBlock.type = BannerBlockType.Button;
        buttonBlock.text = json.getString("text");
        buttonBlock.color = json.getString("color");
        buttonBlock.background = json.getString("background");
        buttonBlock.size = json.getInt("size");
        buttonBlock.alignment = Alignment.fromString(json.getString("alignment"));
        buttonBlock.dismiss = json.getBoolean("dismiss");
        buttonBlock.radius = json.getInt("radius");

        return buttonBlock;
    }
}
