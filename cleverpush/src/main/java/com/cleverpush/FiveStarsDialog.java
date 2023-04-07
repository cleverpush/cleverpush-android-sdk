package com.cleverpush;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import com.cleverpush.util.Logger;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.cleverpush.listener.NegativeReviewListener;
import com.cleverpush.listener.ReviewListener;

import java.lang.reflect.Field;

public class FiveStarsDialog implements DialogInterface.OnClickListener {

  private final static String DEFAULT_TITLE = "Rate this app";
  private final static String DEFAULT_TEXT = "How much do you love our app?";
  private final static String DEFAULT_POSITIVE = "Ok";
  private final static String DEFAULT_NEGATIVE = "Not Now";
  private final static String DEFAULT_NEVER = "Never";
  private static final String TAG = FiveStarsDialog.class.getSimpleName();
  private final Context context;
  private boolean isForceMode = false;
  private String supportEmail;
  private TextView contentTextView;
  private RatingBar ratingBar;
  private String title = null;
  private String rateText = null;
  private AlertDialog alertDialog;
  private View dialogView;
  private int upperBound = 4;
  private NegativeReviewListener negativeReviewListener;
  private ReviewListener reviewListener;
  private int starColor;
  private String positiveButtonText;
  private String negativeButtonText;
  private String neverButtonText;

  public FiveStarsDialog(Context context, String supportEmail) {
    this.context = context;
    this.supportEmail = supportEmail;
  }

  private void build() {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    LayoutInflater inflater = LayoutInflater.from(context);
    dialogView = inflater.inflate(R.layout.stars, null);
    String titleToAdd = (title == null) ? DEFAULT_TITLE : title;
    String textToAdd = (rateText == null) ? DEFAULT_TEXT : rateText;
    contentTextView = dialogView.findViewById(R.id.text_content);
    contentTextView.setText(textToAdd);
    ratingBar = dialogView.findViewById(R.id.ratingBar);
    ratingBar.setOnRatingBarChangeListener((ratingBar, v, b) -> {
      Logger.d(TAG, "Rating changed : " + v);
      if (isForceMode && v >= upperBound) {
        openMarket();
        if (reviewListener != null) {
          reviewListener.onReview((int) ratingBar.getRating());
        }
      }
    });

    if (starColor != -1) {
      LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
      stars.getDrawable(1).setColorFilter(starColor, PorterDuff.Mode.SRC_ATOP);
      stars.getDrawable(2).setColorFilter(starColor, PorterDuff.Mode.SRC_ATOP);
    }

    alertDialog = builder.setTitle(titleToAdd)
        .setView(dialogView)
        .setNegativeButton((negativeButtonText == null) ? DEFAULT_NEGATIVE : negativeButtonText, this)
        .setPositiveButton((positiveButtonText == null) ? DEFAULT_POSITIVE : positiveButtonText, this)
        .setNeutralButton((neverButtonText == null) ? DEFAULT_NEVER : neverButtonText, this)
        .create();
  }

  private void openMarket() {
    final String appPackageName = context.getPackageName();
    try {
      context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
    } catch (android.content.ActivityNotFoundException anfe) {
      context.startActivity(
          new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
    }
  }

  private void sendEmail() {
    final Intent emailIntent = new Intent(Intent.ACTION_SEND);
    emailIntent.setType("text/email");
    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {supportEmail});
    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Report (" + getApplicationName(context) + ")");
    emailIntent.putExtra(Intent.EXTRA_TEXT, getDeviceInfo());
    context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
  }

  public void show() {
    this.build();
    alertDialog.show();
  }

  @Override
  public void onClick(DialogInterface dialogInterface, int i) {
    if (i == DialogInterface.BUTTON_POSITIVE) {
      if (ratingBar.getRating() < upperBound) {
        if (negativeReviewListener == null) {
          sendEmail();
        } else {
          negativeReviewListener.onNegativeReview((int) ratingBar.getRating());
        }

      } else if (!isForceMode) {
        openMarket();
      }
      if (reviewListener != null) {
        reviewListener.onReview((int) ratingBar.getRating());
      }
    }
    if (i == DialogInterface.BUTTON_NEUTRAL) {

    }
    if (i == DialogInterface.BUTTON_NEGATIVE) {

    }
    alertDialog.hide();
  }

  public FiveStarsDialog setTitle(String title) {
    this.title = title;
    return this;
  }

  public FiveStarsDialog setSupportEmail(String supportEmail) {
    this.supportEmail = supportEmail;
    return this;
  }

  public FiveStarsDialog setRateText(String rateText) {
    this.rateText = rateText;
    return this;
  }

  public FiveStarsDialog setStarColor(int color) {
    starColor = color;
    return this;
  }

  public FiveStarsDialog setPositiveButtonText(String positiveButtonText) {
    this.positiveButtonText = positiveButtonText;
    return this;
  }

  public FiveStarsDialog setNegativeButtonText(String negativeButtonText) {
    this.negativeButtonText = negativeButtonText;
    return this;
  }

  public FiveStarsDialog setNeverButtonText(String neverButtonText) {
    this.neverButtonText = neverButtonText;
    return this;
  }

  public FiveStarsDialog setForceMode(boolean isForceMode) {
    this.isForceMode = isForceMode;
    return this;
  }

  public FiveStarsDialog setUpperBound(int bound) {
    this.upperBound = bound;
    return this;
  }

  public FiveStarsDialog setNegativeReviewListener(NegativeReviewListener listener) {
    this.negativeReviewListener = listener;
    return this;
  }

  public FiveStarsDialog setReviewListener(ReviewListener listener) {
    this.reviewListener = listener;
    return this;
  }

  public static String getApplicationName(Context context) {
    ApplicationInfo applicationInfo = context.getApplicationInfo();
    int stringId = applicationInfo.labelRes;
    return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
  }

  private String getDeviceInfo() {
    Field[] fields = Build.VERSION_CODES.class.getFields();
    String deviceInfo = "";
    deviceInfo += "\n OS: " + fields[Build.VERSION.SDK_INT].getName();
    deviceInfo += "\n OS Version: " + android.os.Build.VERSION.SDK_INT;
    deviceInfo += "\n Manufacturer: " + Build.MANUFACTURER;
    deviceInfo += "\n Device: " + android.os.Build.DEVICE;
    deviceInfo += "\n Model: " + android.os.Build.MODEL;
    return deviceInfo;
  }
}
