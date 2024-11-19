package com.cleverpush.stories;

import static com.cleverpush.stories.StoryView.DEFAULT_BACKGROUND_COLOR;

import android.animation.ValueAnimator;
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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.cleverpush.R;
import com.cleverpush.stories.listener.OnItemClickListener;
import com.cleverpush.stories.models.Story;
import com.cleverpush.util.RoundedLinearLayout;

import java.util.ArrayList;

public class StoryViewListAdapter extends RecyclerView.Adapter<StoryViewHolder> {

  private int DEFAULT_BORDER_COLOR = Color.BLACK;
  private int DEFAULT_TEXT_COLOR = Color.BLACK;
  private int DEFAULT_UNREAD_COUNT_BACKGROUND_COLOR = Color.BLACK;
  private int DEFAULT_UNREAD_COUNT_TEXT_COLOR = Color.WHITE;

  private Context context;
  private ArrayList<Story> stories;
  private OnItemClickListener onItemClickListener;
  private TypedArray typedArray;
  public static StoryViewListAdapter storyViewListAdapter;
  private int parentLayoutWidth;
  private boolean isGroupStoryCategories;
  private static final String TAG = "CleverPush/StoryViewAdapter";
  boolean isDarkModeEnabled;
  boolean updateView = false;

  public StoryViewListAdapter(Context context, ArrayList<Story> stories, TypedArray typedArray, OnItemClickListener onItemClickListener,
                              int parentLayoutWidth, boolean isGroupStoryCategories, boolean isDarkModeEnabled) {
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
    this.parentLayoutWidth = parentLayoutWidth;
    this.isGroupStoryCategories = isGroupStoryCategories;
    this.isDarkModeEnabled = isDarkModeEnabled;
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
      TextView unreadCountTextView = (TextView) holder.itemView.findViewById(R.id.tvUnreadCount);
      FrameLayout unreadCountFrameLayout = (FrameLayout) holder.itemView.findViewById(R.id.unreadCountFrameLayout);
      RelativeLayout unreadCountRelativeLayout = (RelativeLayout) holder.itemView.findViewById(R.id.unreadCountRelativeLayout);
      ImageView image = (ImageView) holder.itemView.findViewById(R.id.ivChallenge);
      RoundedLinearLayout cardView = (RoundedLinearLayout) holder.itemView.findViewById(R.id.ivChallengeCardView);
      RoundedLinearLayout cardViewShadow = (RoundedLinearLayout) holder.itemView.findViewById(R.id.cardViewShadow);
      LinearLayout borderLayout = (LinearLayout) holder.itemView.findViewById(R.id.borderLayout);
      FrameLayout shadowFrame = (FrameLayout) holder.itemView.findViewById(R.id.shadowFrame);
      LinearLayout storyLayout = (LinearLayout) holder.itemView.findViewById(R.id.storyLayout);
      LinearLayout imageLayout = (LinearLayout) holder.itemView.findViewById(R.id.imageLayout);
      LinearLayout parentLayout = (LinearLayout) holder.itemView.findViewById(R.id.parentLayout);
      RelativeLayout titleInsideLayout = (RelativeLayout) holder.itemView.findViewById(R.id.titleInsideLayout);
      TextView tvTitleInside = (TextView) holder.itemView.findViewById(R.id.tvTitleInside);

      int iconHeight = (int) typedArray.getDimension(R.styleable.StoryView_story_icon_height, 206);
      int iconHeightPercentage = typedArray.getInt(R.styleable.StoryView_story_icon_height_percentage, 0);
      int iconWidth = (int) typedArray.getDimension(R.styleable.StoryView_story_icon_width, 206);
      boolean iconShadow = typedArray.getBoolean(R.styleable.StoryView_story_icon_shadow, false);
      int borderVisibility = typedArray.getInt(R.styleable.StoryView_border_visibility, View.VISIBLE);
      float borderMargin = typedArray.getDimension(R.styleable.StoryView_border_margin, 13.0F);
      int borderWidth = (int) typedArray.getDimension(R.styleable.StoryView_border_width, 5);
      float cornerRadius = typedArray.getDimension(R.styleable.StoryView_story_icon_corner_radius, -1);
      int subStoryUnreadCount = typedArray.getInt(R.styleable.StoryView_sub_story_unread_count_visibility, View.GONE);
      int restrictToItems = typedArray.getInt(R.styleable.StoryView_restrict_to_items, 0);
      float iconSpace = typedArray.getDimension(R.styleable.StoryView_story_icon_space, -1);
      int titlePosition = typedArray.getInt(R.styleable.StoryView_title_position, 0);
      int titleVisibility = typedArray.getInt(R.styleable.StoryView_title_visibility, View.VISIBLE);
      int unreadCountBadgeHeight = (int) typedArray.getDimension(R.styleable.StoryView_sub_story_unread_count_badge_height, 78);
      int unreadCountBadgeWidth = (int) typedArray.getDimension(R.styleable.StoryView_sub_story_unread_count_badge_width, 78);

      int storyViewBackgroundColor = 0;
      if (isDarkModeEnabled) {
        storyViewBackgroundColor = typedArray.getColor(R.styleable.StoryView_background_color_dark_mode, DEFAULT_BACKGROUND_COLOR);
      } else {
        storyViewBackgroundColor = typedArray.getColor(R.styleable.StoryView_background_color, DEFAULT_BACKGROUND_COLOR);
      }
      int textColor = 0;
      if (isDarkModeEnabled) {
        textColor = typedArray.getColor(R.styleable.StoryView_text_color_dark_mode, DEFAULT_TEXT_COLOR);
      } else {
        textColor = typedArray.getColor(R.styleable.StoryView_text_color, DEFAULT_TEXT_COLOR);
      }

      parentLayout.setBackgroundColor(storyViewBackgroundColor);

      int padding = convertDpToPx(context, 3);

      parentLayout.setPadding(padding, padding, padding, padding);

      if (restrictToItems > 0) {
        float width = (float) parentLayoutWidth / restrictToItems;
        if (iconSpace != -1) {
          width = (width - (iconSpace * 2));
        }
        if (subStoryUnreadCount == 0) {
          width = width - 30;
        }
        width = width - (padding * 2);
        float decimalPart = width - (int) width;

        if (decimalPart >= 0.5) {
          iconWidth = (int) Math.ceil(width);
        } else {
          iconWidth = (int) Math.floor(width);
          iconWidth += (int) (decimalPart * 10);
        }

        if (subStoryUnreadCount == 0) {
          iconWidth += 9;
        }
      }

      if (iconHeightPercentage > 0) {
        iconHeight = (int) ((iconWidth * iconHeightPercentage) / 100.0);
      }

      if (subStoryUnreadCount == 0) {
        if (stories.get(position).getUnreadCount() <= 0) {
          unreadCountTextView.setVisibility(View.GONE);
        } else {
          ViewGroup.LayoutParams unreadCountTextViewLayoutParams = unreadCountTextView.getLayoutParams();
          unreadCountTextViewLayoutParams.height = unreadCountBadgeHeight;
          unreadCountTextViewLayoutParams.width = unreadCountBadgeWidth;
          unreadCountTextView.setLayoutParams(unreadCountTextViewLayoutParams);

          unreadCountTextView.setVisibility(View.VISIBLE);
          unreadCountTextView.setText(stories.get(position).getUnreadCount() + "");
        }
        int unreadCountTextColor = 0;
        if (isDarkModeEnabled) {
          unreadCountTextColor = typedArray.getColor(R.styleable.StoryView_sub_story_unread_count_text_color_dark_mode, DEFAULT_UNREAD_COUNT_TEXT_COLOR);
        } else {
          unreadCountTextColor = typedArray.getColor(R.styleable.StoryView_sub_story_unread_count_text_color, DEFAULT_UNREAD_COUNT_TEXT_COLOR);
        }
        unreadCountTextView.setTextColor(unreadCountTextColor);

        GradientDrawable circleDrawable = new GradientDrawable();
        circleDrawable.setShape(GradientDrawable.OVAL);
        int backgroundColor = 0;
        if (isDarkModeEnabled) {
          backgroundColor = typedArray.getColor(R.styleable.StoryView_sub_story_unread_count_background_color_dark_mode, DEFAULT_UNREAD_COUNT_BACKGROUND_COLOR);;
        } else {
          backgroundColor = typedArray.getColor(R.styleable.StoryView_sub_story_unread_count_background_color, DEFAULT_UNREAD_COUNT_BACKGROUND_COLOR);
        }
        circleDrawable.setColor(backgroundColor);
        int unreadCountBorderWidth = 3;
        circleDrawable.setStroke(unreadCountBorderWidth, unreadCountTextColor);

        unreadCountTextView.setBackground(circleDrawable);

        ViewGroup.LayoutParams unreadCountFrameLayoutParams = unreadCountFrameLayout.getLayoutParams();
        unreadCountFrameLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (cornerRadius == -1) {
          unreadCountFrameLayoutParams.width = iconWidth;
        } else {
          unreadCountFrameLayoutParams.width = iconWidth + 25;
        }
        unreadCountFrameLayout.setLayoutParams(unreadCountFrameLayoutParams);

        ViewGroup.LayoutParams unreadCountRelativeLayoutParams = unreadCountRelativeLayout.getLayoutParams();
        unreadCountRelativeLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (cornerRadius == -1) {
          unreadCountRelativeLayoutParams.width = iconWidth;
        } else {
          unreadCountRelativeLayoutParams.width = iconWidth + 25;
        }
        unreadCountRelativeLayout.setLayoutParams(unreadCountRelativeLayoutParams);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) storyLayout.getLayoutParams();
        params.setMargins(params.leftMargin, 40, params.rightMargin, params.bottomMargin);
        storyLayout.setLayoutParams(params);
      } else {
        unreadCountTextView.setVisibility(View.GONE);

        ViewGroup.LayoutParams unreadCountFrameLayoutParams = unreadCountFrameLayout.getLayoutParams();
        unreadCountFrameLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        unreadCountFrameLayoutParams.width = iconWidth;
        unreadCountFrameLayout.setLayoutParams(unreadCountFrameLayoutParams);

        ViewGroup.LayoutParams unreadCountRelativeLayoutParams = unreadCountRelativeLayout.getLayoutParams();
        unreadCountRelativeLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        unreadCountRelativeLayoutParams.width = iconWidth;
        unreadCountRelativeLayout.setLayoutParams(unreadCountRelativeLayoutParams);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) storyLayout.getLayoutParams();
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin);
        storyLayout.setLayoutParams(params);
      }

      if (borderVisibility == 0 && !stories.get(position).isOpened()) {
        ViewGroup.LayoutParams imageParams = image.getLayoutParams();
        if (cornerRadius == -1) {
          imageParams.height = (int) (iconHeight - borderMargin - 12);
        } else {
          imageParams.height = iconHeight;
        }
        imageParams.width = (int) (iconWidth - borderMargin - 12);
        image.setLayoutParams(imageParams);

        ViewGroup.LayoutParams titleInsideLayoutParams = titleInsideLayout.getLayoutParams();
        if (cornerRadius == -1) {
          titleInsideLayoutParams.height = (int) (iconHeight - borderMargin - 12);
        } else {
          titleInsideLayoutParams.height = iconHeight;
        }
        titleInsideLayoutParams.width = (int) (iconWidth - borderMargin - 12);
        titleInsideLayout.setLayoutParams(titleInsideLayoutParams);

        ViewGroup.LayoutParams cardParams = cardView.getLayoutParams();
        if (cornerRadius == -1) {
          cardParams.height = (int) (iconHeight - borderMargin - 12);
        } else {
          cardParams.height = iconHeight;
        }
        cardParams.width = (int) (iconWidth - borderMargin - 12);
        cardView.setLayoutParams(cardParams);

        ViewGroup.LayoutParams imageLayoutParams = imageLayout.getLayoutParams();
        imageLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        imageLayoutParams.width = (int) (iconWidth - borderMargin - 12);
        imageLayout.setLayoutParams(imageLayoutParams);

        ViewGroup.LayoutParams cardViewShadowParams = cardViewShadow.getLayoutParams();
        if (iconShadow) {
          cardViewShadowParams.height = iconHeight + 7;
          shadowFrame.setBackgroundColor(Color.parseColor("#838383"));
        } else {
          if (cornerRadius == -1) {
            cardViewShadowParams.height = (int) (iconHeight - borderMargin - 12);
          } else {
            cardViewShadowParams.height = iconHeight;
          }
        }
        cardViewShadowParams.width = (int) (iconWidth - borderMargin - 12);
        cardViewShadow.setLayoutParams(cardViewShadowParams);
      } else {
        ViewGroup.LayoutParams imageParams = image.getLayoutParams();
        imageParams.height = iconHeight;
        if (cornerRadius == -1) {
          imageParams.width = iconWidth;
        } else {
          imageParams.width = (int) (iconWidth - borderMargin - 12);
        }
        image.setLayoutParams(imageParams);

        ViewGroup.LayoutParams titleInsideLayoutParams = titleInsideLayout.getLayoutParams();
        titleInsideLayoutParams.height = iconHeight;
        if (cornerRadius == -1) {
          titleInsideLayoutParams.width = iconWidth - 12;
        } else {
          titleInsideLayoutParams.width = (int) (iconWidth - borderMargin - 12);
        }
        titleInsideLayout.setLayoutParams(titleInsideLayoutParams);

        ViewGroup.LayoutParams cardParams = cardView.getLayoutParams();
        cardParams.height = iconHeight;
        if (cornerRadius == -1) {
          cardParams.width = iconWidth - 12;
        } else {
          cardParams.width = (int) (iconWidth - borderMargin - 12);
        }
        cardView.setLayoutParams(cardParams);

        ViewGroup.LayoutParams imageLayoutParams = imageLayout.getLayoutParams();
        imageLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (cornerRadius == -1) {
          imageLayoutParams.width = iconWidth - 12;
        } else {
          imageLayoutParams.width = (int) (iconWidth - borderMargin - 12);
        }
        imageLayout.setLayoutParams(imageLayoutParams);

        ViewGroup.LayoutParams cardViewShadowParams = cardViewShadow.getLayoutParams();
        if (iconShadow) {
          cardViewShadowParams.height = iconHeight + 7;
          shadowFrame.setBackgroundColor(Color.parseColor("#838383"));
        } else {
          cardViewShadowParams.height = iconHeight;
        }
        if (cornerRadius == -1) {
          cardViewShadowParams.width = iconWidth - 12;
        } else {
          cardViewShadowParams.width = (int) (iconWidth - borderMargin - 12);
        }
        cardViewShadow.setLayoutParams(cardViewShadowParams);
      }

      ViewGroup.LayoutParams storyLayoutParams = storyLayout.getLayoutParams();
      storyLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
      storyLayoutParams.width = iconWidth;
      storyLayout.setLayoutParams(storyLayoutParams);

      if (titleVisibility == 0) {
        if (titlePosition == 0) {
          nameTextView.setVisibility(View.VISIBLE);
          titleInsideLayout.setVisibility(View.GONE);

          ViewGroup.LayoutParams nameTextViewParams = nameTextView.getLayoutParams();
          nameTextViewParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
          nameTextViewParams.width = iconWidth;
          nameTextView.setLayoutParams(nameTextViewParams);

          int titleTextSize = typedArray.getDimensionPixelSize(R.styleable.StoryView_title_text_size, 32);
          nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize);
          String titleText = getTitleText(stories, position);
          nameTextView.setText(titleText);
          nameTextView.setTextColor(textColor);
          applyFont(nameTextView, typedArray);
        } else {
          nameTextView.setVisibility(View.GONE);
          titleInsideLayout.setVisibility(View.VISIBLE);

          int titleTextSize = typedArray.getDimensionPixelSize(R.styleable.StoryView_title_text_size, 32);
          int minTitleTextSize = typedArray.getDimensionPixelSize(R.styleable.StoryView_title_min_text_size, 12);
          int maxTitleTextSize = typedArray.getDimensionPixelSize(R.styleable.StoryView_title_max_text_size, 32);

          tvTitleInside.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize);

          String titleText = getTitleText(stories, position);
          tvTitleInside.setText(titleText);
          tvTitleInside.setTextColor(textColor);
          applyFont(tvTitleInside, typedArray);

          // Measure the available width for the title
          int availableWidth = holder.itemView.getWidth() - (tvTitleInside.getPaddingLeft() + tvTitleInside.getPaddingRight());

          if (updateView) {
            availableWidth = 0;
          }

          // Calculate the appropriate text size
          float textSize = Math.min(maxTitleTextSize, titleTextSize); // Start with the smaller of max or defined size
          Paint paint = new Paint();
          paint.setTextSize(textSize);

          while (paint.measureText(titleText) > availableWidth && textSize > minTitleTextSize) {
            textSize--; // Decrease text size until it fits or reaches the minimum
            paint.setTextSize(textSize);
          }

          // Set the final text size
          tvTitleInside.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

          RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tvTitleInside.getLayoutParams();
          layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
          layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);

          if (titlePosition == 1) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
          } else if (titlePosition == 2) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
          }

          tvTitleInside.setLayoutParams(layoutParams);
        }
      } else {
        nameTextView.setVisibility(View.GONE);
        titleInsideLayout.setVisibility(View.GONE);
      }

      loadImage(position, image, isDarkModeEnabled);

      if (cornerRadius != -1) {
        cardView.setCornerRadius(cornerRadius);
        cardViewShadow.setCornerRadius(cornerRadius);
      } else {
        cardView.setCornerRadius(350);
        cardViewShadow.setCornerRadius(350);
      }

      if (iconSpace != -1) {
        ViewGroup.MarginLayoutParams parentLayoutParams = (ViewGroup.MarginLayoutParams) parentLayout.getLayoutParams();
        parentLayoutParams.setMargins((int) iconSpace, parentLayoutParams.topMargin, (int) iconSpace, parentLayoutParams.bottomMargin);
        parentLayout.setLayoutParams(parentLayoutParams);
      }

      if (subStoryUnreadCount == 0) {
        ViewGroup.MarginLayoutParams parentLayoutParams = (ViewGroup.MarginLayoutParams) parentLayout.getLayoutParams();
        parentLayoutParams.setMargins(parentLayoutParams.leftMargin, parentLayoutParams.topMargin, parentLayoutParams.rightMargin - 5, parentLayoutParams.bottomMargin);
        parentLayout.setLayoutParams(parentLayoutParams);
      }

      applyIconBorder(position, borderLayout, cornerRadius, borderWidth, borderMargin, imageLayout, storyViewBackgroundColor, isDarkModeEnabled);

      int finalStoryViewBackgroundColor = storyViewBackgroundColor;
      image.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (onItemClickListener == null) {
            return;
          }
          applyGradientIconBorder(position, borderLayout, cornerRadius, borderWidth, borderMargin, imageLayout, finalStoryViewBackgroundColor, isDarkModeEnabled);
          onItemClickListener.onClicked(position);
        }
      });
    } catch (Exception e) {
      Logger.e(TAG, "Error in onBindViewHolder of StoryViewListAdapter", e);
    }
  }

  private String getTitleText( ArrayList<Story> stories, int position) {
    if (isGroupStoryCategories) {
      if (stories.get(position).getContent().getSubtitle() != null && !stories.get(position).getContent().getSubtitle().isEmpty()) {
        return stories.get(position).getContent().getSubtitle();
      } else if (stories.get(position).getContent().getTitle() != null && !stories.get(position).getContent().getTitle().isEmpty()) {
        return stories.get(position).getContent().getTitle();
      }
    } else {
      if (stories.get(position).getTitle() != null && !stories.get(position).getTitle().isEmpty()) {
        return stories.get(position).getTitle();
      } else if (stories.get(position).getContent().getTitle() != null && !stories.get(position).getContent().getTitle().isEmpty()) {
        return stories.get(position).getContent().getTitle();
      }
    }
    return "";
  }

  public void applyIconBorder(int position, LinearLayout borderLayout, float cornerRadius, int borderWidth, float borderMargin,
                              LinearLayout imageLayout, int storyViewBackgroundColor, boolean isDarkModeEnabled) {
    try {
      GradientDrawable border = new GradientDrawable();

      if (cornerRadius == -1) {
        // No corner radius provided, display as a circle
        border.setShape(GradientDrawable.OVAL);
        border.setCornerRadii(new float[]{0, 0, 0, 0, 0, 0, 0, 0});
      } else {
        // Set the corner radius
        border.setCornerRadius(cornerRadius + 5);
      }

      int borderVisibility = typedArray.getInt(R.styleable.StoryView_border_visibility, View.VISIBLE);
      if (borderVisibility == 0) {
        if (stories.get(position).isOpened()) {
          border.setColor(storyViewBackgroundColor); // Transparent background
          border.setStroke(borderWidth, storyViewBackgroundColor); // Transparent stroke
        } else {
          border.setColor(0xFFFFFFFF); // White background
          int borderColor = 0;
          if (isDarkModeEnabled) {
            borderColor = typedArray.getColor(R.styleable.StoryView_border_color_dark_mode, DEFAULT_BORDER_COLOR);
          } else {
            borderColor = typedArray.getColor(R.styleable.StoryView_border_color, DEFAULT_BORDER_COLOR);
          }
          border.setStroke(borderWidth, borderColor); // Black or desired border color
        }
      } else {
        border.setColor(storyViewBackgroundColor); // Transparent background
        border.setStroke(borderWidth, storyViewBackgroundColor); // Transparent stroke
      }

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        borderLayout.setBackgroundDrawable(border);
      } else {
        borderLayout.setBackground(border);
      }

      ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imageLayout.getLayoutParams();
      params.setMargins((int) borderMargin, (int) borderMargin, (int) borderMargin, (int) borderMargin);
      imageLayout.setLayoutParams(params);
    } catch (Exception e) {
      Logger.e(TAG, "Error while applying border to icon. " + e.getLocalizedMessage(), e);
    }
  }

  public void applyGradientIconBorder(int position, LinearLayout borderLayout, float cornerRadius, int borderWidth, float borderMargin,
                                      LinearLayout imageLayout, int storyViewBackgroundColor, boolean isDarkModeEnabled) {
    try {
      int DEFAULT_ANIM_COLOR;
      if (isDarkModeEnabled) {
        DEFAULT_ANIM_COLOR = typedArray.getColor(R.styleable.StoryView_border_color_dark_mode, DEFAULT_BORDER_COLOR);
      } else {
        DEFAULT_ANIM_COLOR = typedArray.getColor(R.styleable.StoryView_border_color, DEFAULT_BORDER_COLOR);
      }
      int borderAnimColor;
      if (isDarkModeEnabled) {
        borderAnimColor = typedArray.getColor(R.styleable.StoryView_border_color_loading_dark_mode, DEFAULT_ANIM_COLOR);
      } else {
        borderAnimColor = typedArray.getColor(R.styleable.StoryView_border_color_loading, DEFAULT_ANIM_COLOR);
      }

      GradientDrawable gradientBorder = new GradientDrawable(
          GradientDrawable.Orientation.LEFT_RIGHT,
          new int[]{borderAnimColor, borderAnimColor, borderAnimColor}
      );
      gradientBorder.setShape(cornerRadius == -1 ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);

      if (cornerRadius != -1) {
        gradientBorder.setCornerRadius(cornerRadius + borderWidth);
      }

      GradientDrawable innerDrawable = new GradientDrawable();
      innerDrawable.setShape(cornerRadius == -1 ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);

      if (cornerRadius != -1) {
        innerDrawable.setCornerRadius(cornerRadius);
      }

      if (stories.get(position).isOpened()) {
        innerDrawable.setColor(storyViewBackgroundColor); // Transparent background
      } else {
        innerDrawable.setColor(0xFFFFFFFF); // White background
      }

      // Layer the gradient border over the inner drawable
      LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{gradientBorder, innerDrawable});
      layerDrawable.setLayerInset(1, borderWidth, borderWidth, borderWidth, borderWidth);

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        borderLayout.setBackgroundDrawable(layerDrawable);
      } else {
        borderLayout.setBackground(layerDrawable);
      }

      ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imageLayout.getLayoutParams();
      params.setMargins((int) borderMargin, (int) borderMargin, (int) borderMargin, (int) borderMargin);
      imageLayout.setLayoutParams(params);

      // Add fade-in/out animation by changing the alpha of the gradient border
      ValueAnimator alphaAnimator = ValueAnimator.ofInt(0, 255); // Alpha values from transparent to fully opaque
      alphaAnimator.setDuration(1000); // Duration of each fade in/out cycle
      alphaAnimator.setRepeatCount(ValueAnimator.INFINITE); // Repeat indefinitely
      alphaAnimator.setRepeatMode(ValueAnimator.REVERSE); // Reverse back to start alpha

      alphaAnimator.addUpdateListener(animator -> {
        int alphaValue = (int) animator.getAnimatedValue();
        gradientBorder.setAlpha(alphaValue); // Apply the animated alpha to the gradient
        borderLayout.invalidate(); // Redraw the view to apply the alpha change
      });

      alphaAnimator.start();

    } catch (Exception e) {
      Logger.e(TAG, "Error while applying gradient border to icon. " + e.getLocalizedMessage(), e);
    }
  }

  public static int convertDpToPx(Context context, int dp) {
    float density = context.getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
  }

  @Override
  public int getItemCount() {
    return stories.size();
  }

  private void loadImage(int position, ImageView image, boolean isDarkModeEnabled) {
    try {
      ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          String widgetUrl = "";
          if (isDarkModeEnabled) {
            widgetUrl = stories.get(position).getContent().getPreview().getWidgetDarkSrc();
          } else {
            widgetUrl = stories.get(position).getContent().getPreview().getWidgetSrc();
          }
          String posterPortraitUrl = stories.get(position).getContent().getPreview().getPosterPortraitSrc();

          RequestOptions options = new RequestOptions()
              .fitCenter()
              .placeholder(R.drawable.ic_story_placeholder)
              .error(R.drawable.ic_story_placeholder)
              .priority(Priority.HIGH);

          if (widgetUrl != null && !widgetUrl.isEmpty()) {
            Glide.with(context)
                .load(widgetUrl)
                .apply(options)
                .error(
                    Glide.with(context)
                        .load(posterPortraitUrl) // Fallback to posterPortraitUrl if widgetUrl fails
                        .apply(options)
                        .error(R.drawable.ic_story_placeholder) // Final fallback placeholder
                )
                .into(image);
          } else {
            Glide.with(context)
                .load(posterPortraitUrl)
                .apply(options)
                .into(image);
          }
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

  public void updateStories(ArrayList<Story> stories) {
    updateView = true;
    this.stories = stories;
    notifyDataSetChanged();
  }

  public static void setStoryViewListAdapter(StoryViewListAdapter storyViewAdapter) {
    storyViewListAdapter = storyViewAdapter;
  }

  public static StoryViewListAdapter getStoryViewListAdapter() {
    return storyViewListAdapter;
  }

}
