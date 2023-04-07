package com.cleverpush.inbox;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import com.cleverpush.util.Logger;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;
import com.cleverpush.Notification;
import com.cleverpush.R;
import com.cleverpush.inbox.listener.OnItemClickListener;
import com.cleverpush.listener.ChannelConfigListener;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class InboxViewListAdapter extends RecyclerView.Adapter<InboxViewHolder> {

  private static final String TAG = "CleverPush/InboxView";
  private int DEFAULT_COLOR = Color.BLACK;
  private int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
  private Context context;
  private ArrayList<Notification> notificationArrayList;
  private TypedArray typedArray;
  private OnItemClickListener onItemClickListener;

  public InboxViewListAdapter(Context context, ArrayList<Notification> notifications, TypedArray typedArray, OnItemClickListener onItemClickListener) {
    this.context = context;
    this.notificationArrayList = notifications;
    this.typedArray = typedArray;
    this.onItemClickListener = onItemClickListener;
  }

  @NonNull
  @Override
  public InboxViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(context);
    View itemViewInbox = inflater.inflate(R.layout.item_view_inbox, parent, false);
    return new InboxViewHolder(itemViewInbox);
  }

  @SuppressLint("ResourceType")
  @Override
  public void onBindViewHolder(InboxViewHolder holder, int position) {

    LinearLayout linearLayout = (LinearLayout) holder.itemView.findViewById(R.id.llItemViewInBox);
    TextView titleTextView = (TextView) holder.itemView.findViewById(R.id.tvTitle);
    TextView dateTextView = (TextView) holder.itemView.findViewById(R.id.tvDate);
    View view = (View) holder.itemView.findViewById(R.id.divider);
    ImageView imageView = (ImageView) holder.itemView.findViewById(R.id.image);

    loadImage(position, imageView);

    int notificationTextSize = typedArray.getDimensionPixelSize(R.styleable.InboxView_notification_text_size, 16);
    titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, notificationTextSize);

    int dateTextSize = typedArray.getDimensionPixelSize(R.styleable.InboxView_date_text_size, 12);
    dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateTextSize);

    view.setBackgroundColor(typedArray.getColor(R.styleable.InboxView_divider_colour, DEFAULT_COLOR));

    titleTextView.setText(notificationArrayList.get(position).getTitle());
    titleTextView.setTextColor(typedArray.getColor(R.styleable.InboxView_notification_text_color, DEFAULT_COLOR));
    applyFont(titleTextView, typedArray, false);

    dateTextView.setText(formatDate(notificationArrayList.get(position).getCreatedAt()));
    dateTextView.setTextColor(typedArray.getColor(R.styleable.InboxView_date_text_color, DEFAULT_COLOR));
    dateTextView.setTextColor(typedArray.getColor(R.styleable.InboxView_date_text_color, DEFAULT_COLOR));
    applyFont(dateTextView, typedArray, true);

    if (notificationArrayList.get(position).getRead()) {
      linearLayout.setBackgroundColor(typedArray.getColor(R.styleable.InboxView_read_color, DEFAULT_BACKGROUND_COLOR));
      titleTextView.setTypeface(Typeface.create(titleTextView.getTypeface(), Typeface.NORMAL));
    } else {
      linearLayout.setBackgroundColor(typedArray.getColor(R.styleable.InboxView_unread_color, DEFAULT_BACKGROUND_COLOR));
      titleTextView.setTypeface(titleTextView.getTypeface(), Typeface.BOLD);
    }

    linearLayout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onItemClickListener.onClicked(position);
      }
    });

  }

  @Override
  public int getItemCount() {
    return notificationArrayList.size();
  }

  /**
   * Applies a font to a TextView that uses the "fontPath" attribute.
   *
   * @param textView   TextView when the font should apply
   * @param typedArray Attributes that contain the "fontPath" attribute with the path to the font file in the assets folder
   */
  public void applyFont(TextView textView, TypedArray typedArray, boolean isDate) {
    if (typedArray != null) {
      Context context = textView.getContext();
      String fontPath;
      if (isDate) {
        fontPath = typedArray.getString(R.styleable.InboxView_date_text_font_family);
      } else {
        fontPath = typedArray.getString(R.styleable.InboxView_notification_text_font_family);
      }
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
  private Typeface getTypeface(Context context, String path) throws RuntimeException {
    Typeface typeface;
    try {
      typeface = Typeface.createFromAsset(context.getAssets(), path + ".ttf");
    } catch (RuntimeException exception) {
      String message = "Font assets/" + path + " cannot be loaded";
      throw new RuntimeException(message);
    }
    return typeface;
  }

  private String formatDate(String dateToFormat) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    Date date = null;
    try {
      date = sdf.parse(dateToFormat);
    } catch (ParseException e) {
      Logger.e(TAG, e.getLocalizedMessage());
    }
    java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);

    if (date == null) {
      return "";
    }

    return dateFormat.format(date);
  }

  private void loadImage(int position, ImageView image) {
    new Thread(() -> {
      try {
        final InputStream[] inputStream = {null};
        if (notificationArrayList.get(position).getMediaUrl() != null) {
          inputStream[0] = new URL(notificationArrayList.get(position).getMediaUrl()).openStream();
          Logger.e("image", notificationArrayList.get(position).getMediaUrl());
        } else if (notificationArrayList.get(position).getIconUrl() != null) {
          inputStream[0] = new URL(notificationArrayList.get(position).getIconUrl()).openStream();
          Logger.e("image", notificationArrayList.get(position).getIconUrl());
        } else {
          CleverPush.getInstance(context).getChannelConfig(new ChannelConfigListener() {
            @Override
            public void ready(JSONObject channelConfig) {
              try {
                inputStream[0] = new URL(channelConfig.optString("channelIcon")).openStream();
              } catch (MalformedURLException e) {
                e.printStackTrace();
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          });
        }

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream[0]);
        if (bitmap != null) {
          ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              image.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 50, 50, false));
            }
          });
        }
      } catch (Exception exception) {
        Logger.e(TAG, exception.getLocalizedMessage());
      }
    }).start();
  }
}
