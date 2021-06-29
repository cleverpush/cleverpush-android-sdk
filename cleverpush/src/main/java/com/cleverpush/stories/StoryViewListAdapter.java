package com.cleverpush.stories;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.R;
import com.cleverpush.stories.models.Story;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;


public class StoryViewListAdapter extends RecyclerView.Adapter<StoryViewListAdapter.ItemViewHolder> {

    private Context mContext;
    private ArrayList<Story> stories;
    private OnItemClicked onItemClicked;
    private int borderColor;

    public StoryViewListAdapter(Context mContext, ArrayList<Story> stories, int borderColor) {
        this.mContext = mContext;
        this.stories = stories;
        this.borderColor = borderColor;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View itemViewStoryHead = inflater.inflate(R.layout.item_view_story, parent, false);
        return new ItemViewHolder(itemViewStoryHead);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        TextView nameTextView = (TextView) holder.itemView.findViewById(R.id.tvTitle);
        ImageView image = (ImageView) holder.itemView.findViewById(R.id.ivChallenge);
        nameTextView.setText(stories.get(position).getTitle());

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
            border.setStroke(5, borderColor); //black border with full opacity
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

}
