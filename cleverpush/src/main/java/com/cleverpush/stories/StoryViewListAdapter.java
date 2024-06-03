package com.cleverpush.stories;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;
import com.cleverpush.util.FontUtils;
import com.cleverpush.util.Logger;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.R;
import com.cleverpush.stories.listener.OnItemClickListener;
import com.cleverpush.stories.models.Story;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class StoryViewListAdapter extends RecyclerView.Adapter<StoryViewHolder> {

  private int DEFAULT_BORDER_COLOR = Color.BLACK;
  private int DEFAULT_TEXT_COLOR = Color.BLACK;

  private Context context;
  private ArrayList<Story> stories;
  private OnItemClickListener onItemClickListener;
  private TypedArray typedArray;
  private static final String TAG = "CleverPush/StoryViewAdapter";

  public StoryViewListAdapter(Context context, ArrayList<Story> stories, TypedArray typedArray,
                              OnItemClickListener onItemClickListener) {
    if (context == null) {
      if (CleverPush.getInstance(CleverPush.context).getCurrentContext() != null) {
        this.context = CleverPush.getInstance(CleverPush.context).getCurrentContext();
      }
    } else {
      this.context = context;
    }
    this.stories = stories;
    this.typedArray = typedArray;
    this.onItemClickListener = onItemClickListener;
  }

  @Override
  public StoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    try {
      if (context == null) {
        Logger.e(TAG, "Context is null");
        return null;
      }
      LayoutInflater inflater = LayoutInflater.from(context);
      View itemViewStoryHead = inflater.inflate(R.layout.item_view_story, parent, false);
      return new StoryViewHolder(itemViewStoryHead);
    } catch (Exception e) {
      Logger.e(TAG, "Error in onCreateViewHolder of StoryViewListAdapter", e);
      return null;
    }
  }

  @SuppressLint("ResourceType")
  @Override
  public void onBindViewHolder(StoryViewHolder holder, int position) {
    try {
      TextView nameTextView = (TextView) holder.itemView.findViewById(R.id.tvTitle);
      CircleImageView image = (CircleImageView) holder.itemView.findViewById(R.id.ivChallenge);

      ViewGroup.LayoutParams params = image.getLayoutParams();
      params.height =
              (int) typedArray.getDimension(R.styleable.StoryView_story_icon_height, 206);
      params.width =
              (int) typedArray.getDimension(R.styleable.StoryView_story_icon_width, 206);
      image.setLayoutParams(params);

      nameTextView.setVisibility(typedArray.getInt(R.styleable.StoryView_title_visibility, View.VISIBLE));
      int titleTextSize = typedArray.getDimensionPixelSize(R.styleable.StoryView_title_text_size, 32);
      nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize);
      nameTextView.setText(stories.get(position).getTitle());
      nameTextView.setTextColor(typedArray.getColor(R.styleable.StoryView_text_color, DEFAULT_TEXT_COLOR));
      applyFont(nameTextView, typedArray);

      loadImage(position, image);

      if (stories.get(position).isOpened()) {
        image.setBackground(null);
      } else {
        GradientDrawable border = new GradientDrawable();
        border.setShape(GradientDrawable.OVAL);
        border.setCornerRadii(new float[]{0, 0, 0, 0, 0, 0, 0, 0});
        border.setColor(0xFFFFFFFF); //white background
        border.setStroke(5, typedArray.getColor(R.styleable.StoryView_border_color,
                DEFAULT_BORDER_COLOR)); //black border with full opacity
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          image.setBackgroundDrawable(border);
        } else {
          image.setBackground(border);
        }
      }

      image.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (onItemClickListener == null) {
            return;
          }
          onItemClickListener.onClicked(position);
        }
      });
    } catch (Exception e) {
      Logger.e(TAG, "Error in onBindViewHolder of StoryViewListAdapter", e);
    }
  }

  @Override
  public int getItemCount() {
    return stories.size();
  }

  private void loadImage(int position, CircleImageView image) {
    try {
      ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          String imageUrl = stories.get(position).getContent().getPreview().getPosterPortraitSrc();

          RequestOptions options = new RequestOptions()
                  .fitCenter()
                  .placeholder(R.drawable.ic_story_placeholder)
                  .error(R.drawable.ic_story_placeholder)
                  .priority(Priority.HIGH);

          Glide.with(context)
                  .load(imageUrl)
                  .apply(options)
                  .into(image);
        }
      });
    } catch (Exception exception) {
      Logger.e(TAG, "Error while loading image in StoryViewListAdapter", exception);
    }
  }

  public static Bitmap getRoundedCroppedBitmap(Bitmap bitmap, int radius) {
    Bitmap finalBitmap;
    if (bitmap.getWidth() != radius || bitmap.getHeight() != radius) {
      finalBitmap = Bitmap.createScaledBitmap(bitmap, radius, radius, false);

    } else {
      finalBitmap = bitmap;
    }
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

  /**
   * Applies a font to a TextView that uses the "fontPath" attribute.
   *
   * @param textView   TextView when the font should apply
   * @param typedArray Attributes that contain the "fontPath" attribute with the path to the font file in the assets folder
   */
  public void applyFont(TextView textView, TypedArray typedArray) {
    if (typedArray != null) {
      Context context = textView.getContext();
      String fontPath = typedArray.getString(R.styleable.StoryView_font_family);
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
      typeface = FontUtils.findFont(context, path);
    } catch (RuntimeException exception) {
      String message = "Font assets/" + path + " cannot be loaded";
      throw new RuntimeException(message);
    }
    return typeface;
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemViewType(int position) {
    return position;
  }
}
