package com.cleverpush.stories;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.R;
import com.cleverpush.stories.models.Story;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import static java.security.AccessController.getContext;


public class StoryViewListAdapter extends RecyclerView.Adapter<StoryViewListAdapter.ItemViewHolder> {

    private Context mContext;
    private ArrayList<Story> stories;
    private OnItemClicked onItemClicked;
    TypedArray attrArray;
    private int DEFAULT_BORDER_COLOR = Color.BLACK;
    private int DEFAULT_TEXT_COLOR = Color.BLACK;


    public StoryViewListAdapter(Context mContext, ArrayList<Story> stories, TypedArray attrArray) {
        this.mContext = mContext;
        this.stories = stories;
        this.attrArray = attrArray;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View itemViewStoryHead = inflater.inflate(R.layout.item_view_story, parent, false);
        return new ItemViewHolder(itemViewStoryHead);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        TextView nameTextView = (TextView) holder.itemView.findViewById(R.id.tvTitle);
        ImageView image = (ImageView) holder.itemView.findViewById(R.id.ivChallenge);
        nameTextView.setText(stories.get(position).getTitle());
        nameTextView.setTextColor(attrArray.getColor(R.styleable.StoryView_text_color, DEFAULT_TEXT_COLOR));
        applyFont(nameTextView, attrArray);

        new Thread(() -> {
            try {
                InputStream in = new URL(stories.get(position).getContent().getPreview().getPosterPortraitSrc()).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    image.setImageBitmap(getRoundedCroppedBitmap(bitmap, bitmap.getWidth()));
                }
            } catch (Exception ignored) {

            }
        }).start();

        if (stories.get(position).isOpened()) {
            image.setBackground(null);
        } else {
            GradientDrawable border = new GradientDrawable();
            border.setShape(GradientDrawable.OVAL);
            border.setCornerRadii(new float[]{0, 0, 0, 0, 0, 0, 0, 0});
            border.setColor(0xFFFFFFFF); //white background
            border.setStroke(5, attrArray.getColor(R.styleable.StoryView_border_color, DEFAULT_BORDER_COLOR)); //black border with full opacity
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                image.setBackgroundDrawable(border);
            } else {
                image.setBackground(border);
            }
        }

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClicked != null) {
                    onItemClicked.onItemClicked(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        public ItemViewHolder(View itemView) {
            super(itemView);
        }

    }

    public static Bitmap getRoundedCroppedBitmap(Bitmap bitmap, int radius) {
        Bitmap finalBitmap;
        if (bitmap.getWidth() != radius || bitmap.getHeight() != radius)
            finalBitmap = Bitmap.createScaledBitmap(bitmap, radius, radius,
                    false);
        else
            finalBitmap = bitmap;
        Bitmap output = Bitmap.createBitmap(finalBitmap.getWidth(),
                finalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, finalBitmap.getWidth(),
                finalBitmap.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(finalBitmap.getWidth() / 2 + 0.7f,
                finalBitmap.getHeight() / 2 + 0.7f,
                finalBitmap.getWidth() / 2 + 0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(finalBitmap, rect, rect, paint);
        return output;
    }

    public interface OnItemClicked {
        void onItemClicked(int position);
    }

    public void setOnItemClicked(OnItemClicked onItemClicked) {
        this.onItemClicked = onItemClicked;
    }

    /**
     * Applies a font to a TextView that uses the "fontPath" attribute.
     *
     * @param textView TextView when the font should apply
     * @param attrs    Attributes that contain the "fontPath" attribute with the path to the font file in the assets folder
     */
    public void applyFont(TextView textView, TypedArray attrs) {
        if (attrs != null) {
            Context context = textView.getContext();
            String fontPath = attrs.getString(R.styleable.StoryView_font_family);
            if (!TextUtils.isEmpty(fontPath)) {
                Typeface typeface = getTypeface(context, fontPath);
                if (typeface != null) {
                    textView.setTypeface(typeface);
                }
            }
        }
    }

    /**
     * Gets a Typeface from the cache. If the Typeface does not exist, creates it, cache it and returns it.
     *
     * @param context a Context
     * @param path    Path to the font file in the assets folder. ie "fonts/MyCustomFont.ttf"
     * @return the corresponding Typeface (font)
     * @throws RuntimeException if the font asset is not found
     */
    public Typeface getTypeface(Context context, String path) throws RuntimeException {
        Typeface typeface;
        try {
            typeface = Typeface.createFromAsset(context.getAssets(), path + ".ttf");
        } catch (RuntimeException exception) {
            String message = "Font assets/" + path + " cannot be loaded";
            throw new RuntimeException(message);
        }
        return typeface;
    }
}
