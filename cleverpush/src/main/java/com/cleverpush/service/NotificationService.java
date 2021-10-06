package com.cleverpush.service;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cleverpush.BadgeHelper;
import com.cleverpush.CleverPush;
import com.cleverpush.Notification;
import com.cleverpush.NotificationCarouselItem;
import com.cleverpush.NotificationCategory;
import com.cleverpush.NotificationCategoryGroup;
import com.cleverpush.NotificationOpenedActivity;
import com.cleverpush.NotificationOpenedReceiver;
import com.cleverpush.R;
import com.cleverpush.Subscription;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationService {
    private static NotificationService sInstance;

    private int GET_BITMAP_TIMEOUT = 20 * 1000;

    private NotificationService() {

    }

    public static NotificationService getInstance() {
        if (sInstance == null) {
            sInstance = new NotificationService();
        }

        return sInstance;
    }

    private int getDrawableId(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    private int getSmallIcon(Context context) {
        int id = getDrawableId(context, "cleverpush_notification_icon");
        if (id != 0) {
            return id;
        }
        return getDrawableId(context, "default_notification_icon");
    }

    private Bitmap getBitmapFromUrl(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(GET_BITMAP_TIMEOUT);
            connection.setReadTimeout(GET_BITMAP_TIMEOUT);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception exception) {
            Log.d("CleverPush", "NotificationService: Exception while loading image", exception);
            return null;
        }
    }

    boolean applicationInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> services = activityManager.getRunningAppProcesses();
        boolean isActivityFound = false;

        if (services.get(0).processName
                .equalsIgnoreCase(context.getPackageName()) && services.get(0).importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            isActivityFound = true;
        }

        return isActivityFound;
    }

    private NotificationCompat.Builder createBasicNotification(Context context, String notificationStr, String subscriptionStr, Notification notification, int requestCode) {
        boolean isBroadcast = false;
        Class<?> notificationOpenedClass;

        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(context, NotificationOpenedReceiver.class);
        intent.setPackage(context.getPackageName());
        if (packageManager.queryBroadcastReceivers(intent, 0).size() > 0) {
            isBroadcast = true;
            notificationOpenedClass = NotificationOpenedReceiver.class;
        } else {
            notificationOpenedClass = NotificationOpenedActivity.class;
        }

        String title = notification.getTitle();
        String text = notification.getText();
        String iconUrl = notification.getIconUrl();
        String mediaUrl = notification.getMediaUrl();

        Intent targetIntent = new Intent(context, notificationOpenedClass);
        if (!isBroadcast) {
            targetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        targetIntent.putExtra("notification", notificationStr);
        targetIntent.putExtra("subscription", subscriptionStr);

        PendingIntent contentIntent;
        if (isBroadcast) {
            contentIntent = PendingIntent.getBroadcast(context, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            contentIntent = PendingIntent.getActivity(context, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (notification.getCategory() != null) {
                NotificationCategory category = notification.getCategory();

                NotificationChannel channel = new NotificationChannel(category.getId(), category.getName(), NotificationManager.IMPORTANCE_DEFAULT);

                String description = category.getDescription();
                if (description != null) {
					channel.setDescription(description);
				}

				String importance = category.getImportance();
                if (importance != null) {
                	if (importance.equalsIgnoreCase("URGENT")) {
						channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
					} else if (importance.equalsIgnoreCase("HIGH")) {
						channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
					} else if (importance.equalsIgnoreCase("MEDIUM")) {
						channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
					} else if (importance.equalsIgnoreCase("LOW")) {
						channel.setImportance(NotificationManager.IMPORTANCE_LOW);
					}
				}

                String lockScreen = category.getLockScreen();
                if (lockScreen != null) {
					if (lockScreen.equalsIgnoreCase("PUBLIC")) {
						channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
					} else if (lockScreen.equalsIgnoreCase("PRIVATE")) {
						channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
					} else if (lockScreen.equalsIgnoreCase("SECRET")) {
						channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_SECRET);
					}
				}

				String ledColor = category.getLedColor();
				if (category.getLedColorEnabled() && ledColor != null) {
					int parsedLedColor = parseColor(ledColor);
					if (parsedLedColor > 0) {
						channel.setLightColor(parsedLedColor);
					}
				}

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

				NotificationCategoryGroup categoryGroup = category.getGroup();
				if (categoryGroup != null) {
					NotificationChannelGroup group = new NotificationChannelGroup(categoryGroup.getId(), categoryGroup.getName());
					notificationManager.createNotificationChannelGroup(group);
					channel.setGroup(group.getId());
				}

                notificationManager.createNotificationChannel(channel);

				notificationBuilder = new NotificationCompat.Builder(context, category.getId());

				String foregroundColor = category.getForegroundColor();
				if (foregroundColor != null) {
					int parsedForegroundColor = parseColor(foregroundColor);
					if (parsedForegroundColor != 0) {
						notificationBuilder.setColor(parsedForegroundColor);
					}
				}

            } else {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("default", "Default", importance);
                channel.setDescription("default");
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);

				notificationBuilder = new NotificationCompat.Builder(context,"default");
            }

        } else {
            notificationBuilder = new NotificationCompat.Builder(context);
        }

        if (notification.getSoundFilename() != null && !notification.getSoundFilename().isEmpty()) {
            Resources resources = context.getResources();
            String packageName = context.getPackageName();
            int soundId = resources.getIdentifier(notification.getSoundFilename(), "raw", packageName);
            if (soundId != 0) {
                Uri trySoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + soundId);
                if (trySoundUri != null) {
                    soundUri = trySoundUri;
                }
            }
        }

        notificationBuilder = notificationBuilder
                .setContentIntent(contentIntent)
				.setDeleteIntent(this.getNotificationDeleteIntent(context))
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(getSmallIcon(context))
                .setAutoCancel(true)
                .setSound(soundUri);

        if (iconUrl != null && !iconUrl.isEmpty()) {
			try {
				Bitmap icon = getBitmapFromUrl(iconUrl);
				if (icon != null) {
					notificationBuilder = notificationBuilder.setLargeIcon(icon);
				}
			} catch (Exception ignored) {
			}
        }

        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            try {
                Bitmap media = getBitmapFromUrl(mediaUrl);
                if (media != null) {
                    notificationBuilder = notificationBuilder.setStyle(
						new NotificationCompat.BigPictureStyle().bigPicture(media)
                    );
                }
            } catch (Exception ignored) {
            }
        } else if (text != null && text.length() > 0) {
            notificationBuilder.setStyle(
                    new NotificationCompat.BigTextStyle().bigText(text)
            );
        }

        // from NotificationExtenderService
        if (notification.getExtender() != null) {
			notificationBuilder.extend(notification.getExtender());
		}

        return notificationBuilder;
    }

	private static void applyTextColorToRemoteViews(RemoteViews remoteViews, View view, int color) {
		if (view instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) view;
			for (int i = 0, count = vg.getChildCount(); i < count; i++) {
				applyTextColorToRemoteViews(remoteViews, vg.getChildAt(i), color);
			}
		} else if (view instanceof TextView) {
			remoteViews.setTextColor(view.getId(), color);
		}
	}

    int parseColor(String hexStr) {
    	if (hexStr == null) {
    		return 0;
		}

		if (hexStr.startsWith("rgb(")) {
			Pattern c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
			Matcher m = c.matcher(hexStr);
			if (m.matches()) {
				hexStr = String.format("#%02x%02x%02x",  Integer.parseInt(m.group(1)),  Integer.parseInt(m.group(2)),  Integer.parseInt(m.group(3)));
			}
		}

    	if (!hexStr.startsWith("#")) {
			hexStr = "#" + hexStr;
		}

    	return Color.parseColor(hexStr);
	}

	int getRequestId(Context context, Notification notification) {
    	// check for existing notifications which have the same tag and should be replaced. If found, use their request code.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StatusBarNotification[] activeNotifs = BadgeHelper.getActiveNotifications(context);
                for (StatusBarNotification activeNotif : activeNotifs) {
                    if (activeNotif.getTag() != null && notification.getTag() != null && activeNotif.getTag().equals(notification.getTag())) {
                        return activeNotif.getId();
                    }
                }
            }
        } catch (Exception ex) {

        }

        // We'll generate a random int and use it as the notification's request code.
		Random random = new SecureRandom();
        return random.nextInt();
    }

    int showNotification(Context context, Notification notification, Subscription subscription) {
    	String notificationStr = notification.getRawPayload();
    	String subscriptionStr = subscription.getRawPayload();

    	int requestId;
		if (notification.getCarouselLength() > 0 && notification.isCarouselEnabled()) {
			requestId = NotificationService.getInstance().createAndShowCarousel(context, notification, notificationStr, subscriptionStr);
		} else {
			requestId = NotificationService.getInstance().sendNotification(context, notification, notificationStr, subscriptionStr);
		}

		BadgeHelper.update(context, CleverPush.getInstance(context).getIncrementBadge());

		return requestId;
	}

    int sendNotification(Context context, Notification notification, String notificationStr, String subscriptionStr) {
        int requestId = getRequestId(context, notification);
        NotificationCompat.Builder notificationBuilder = NotificationService.getInstance().createBasicNotification(context, notificationStr, subscriptionStr, notification, requestId);
        if (notificationBuilder != null) {
            NotificationManagerCompat.from(context).notify(notification.getTag(), requestId, notificationBuilder.build());
        }
        return requestId;
    }

    int createAndShowCarousel(Context context, Notification message, String notificationStr, String subscriptionStr) {
        int requestId = getRequestId(context, message);
        createAndShowCarousel(context, message, notificationStr, subscriptionStr, 0, requestId);
        return requestId;
    }

    void createAndShowCarousel(Context context, Notification message, String notificationStr, String subscriptionStr, int targetIndex, int requestId) {
        Log.i("CleverPush", "NotificationService: createAndShowCarousel");
        NotificationCompat.Builder builder = createBasicNotification(context, notificationStr, subscriptionStr, message, requestId);
        if (builder != null) {
            android.app.Notification notification = builder.build();

			notification.bigContentView = getCarouselImage(context, message, notificationStr, subscriptionStr, targetIndex, requestId);

			builder.setDeleteIntent(getCarouselNotificationDeleteIntent(context, message, notificationStr, subscriptionStr));

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(message.getTag(), requestId, notification);
            }
        }
    }

	private PendingIntent getNotificationDeleteIntent(Context context) {
		Intent delIntent = new Intent(context, NotificationDismissIntentService.class);
		return PendingIntent.getService(context, (int) System.currentTimeMillis(), delIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
	}

    private PendingIntent getCarouselNotificationDeleteIntent(Context context, Notification message, String notificationStr, String subscriptionStr) {
        Intent delIntent = new Intent(context, CarouselNotificationIntentService.class);
        delIntent.setAction(CarouselNotificationIntentService.ACTION_NOTIFICATION_DELETE);

        HashMap<String, String> data = new HashMap<>();
        data.put("notification", notificationStr);
        data.put("subscription", subscriptionStr);

        delIntent.putExtra("notification", message);
        delIntent.putExtra("data", data);

        return PendingIntent.getService(context, (int) System.currentTimeMillis(), delIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    private RemoteViews getCarouselImage(Context context, Notification message, String notificationStr, String subscriptionStr, int currentIndex, int requestId) {
        RemoteViews contentView = null;

        contentView = new RemoteViews(context.getPackageName(), R.layout.notification_carousel_layout);

        setBasicNotificationData(context, message, contentView);

        if (message != null && message.getCarouselLength() > 0) {
            contentView.setViewVisibility(R.id.big_picture, View.VISIBLE);

            if (message.getCarouselLength() > 1) {
                contentView.setViewVisibility(R.id.next_button, View.VISIBLE);
                contentView.setViewVisibility(R.id.prev_button, View.VISIBLE);
            }

            NotificationCarouselItem[] elements = message.getCarouselItems();
            if (currentIndex < elements.length) {
                NotificationCarouselItem item = elements[currentIndex];

                String imageFileName = getImageFileName(item.getMediaUrl());
                Bitmap bitmap = loadImageFromDisc(context, imageFileName);

                if (bitmap == null) {
                    downloadCarouselImages(context, message);
                    bitmap = loadImageFromDisc(context, imageFileName);
                }

                if (bitmap != null) {
                    contentView.setImageViewBitmap(R.id.big_picture, bitmap);
                }

                contentView.setOnClickPendingIntent(
                        R.id.big_picture,
                        getCarouselImageClickPendingIntent(context, message, notificationStr, subscriptionStr, item, requestId)
                );

                contentView.setOnClickPendingIntent(
                        R.id.next_button,
                        getNavigationPendingIntent(context, message, notificationStr, subscriptionStr, message.getNextCarouselIndex(currentIndex), requestId)
                );

                contentView.setOnClickPendingIntent(
                        R.id.prev_button,
                        getNavigationPendingIntent(context, message, notificationStr, subscriptionStr, message.getPreviousCarouselIndex(currentIndex), requestId)
                );
            }
        }

        return contentView;
    }

    private PendingIntent getNavigationPendingIntent(Context context, Notification message, String notificationStr, String subscriptionStr, int targetIndex, int requestId) {
        Log.i("CleverPush", "NotificationService: getNavigationPendingIntent");

        Intent intent = new Intent(context, CarouselNotificationIntentService.class);
        intent.setAction(CarouselNotificationIntentService.ACTION_CAROUSEL_IMG_CHANGE);

        intent.putExtra("carouselIndex", targetIndex);
        intent.putExtra("notificationId", requestId);
        intent.putExtra("notification", message);

        HashMap<String, String> data = new HashMap<>();
        data.put("notification", notificationStr);
        data.put("subscription", subscriptionStr);
        intent.putExtra("data", data);

        return PendingIntent.getService(context, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent getCarouselImageClickPendingIntent(Context context, Notification message, String notificationStr, String subscriptionStr, NotificationCarouselItem element, int requestId) {
        Bundle bundle = new Bundle();
        bundle.putInt("notificationId", requestId);
        bundle.putSerializable("notification", message);

        boolean isBroadcast = false;
        Class<?> notificationOpenedClass;

        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(context, NotificationOpenedReceiver.class);
        intent.setPackage(context.getPackageName());
        if (packageManager.queryBroadcastReceivers(intent, 0).size() > 0) {
            isBroadcast = true;
            notificationOpenedClass = NotificationOpenedReceiver.class;
        } else {
            notificationOpenedClass = NotificationOpenedActivity.class;
        }

        Intent targetIntent = new Intent(context, notificationOpenedClass);
        if (!isBroadcast) {
            targetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        targetIntent.putExtra("notification", notificationStr);
        targetIntent.putExtra("subscription", subscriptionStr);

        int requestCode = (int) System.currentTimeMillis();

        PendingIntent contentIntent;
        if (isBroadcast) {
            contentIntent = PendingIntent.getBroadcast(context, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            contentIntent = PendingIntent.getActivity(context, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        return contentIntent;
    }

    private void setBasicNotificationData(Context context, Notification message, RemoteViews contentView) {
        if (message != null && contentView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setViewVisibility(R.id.icon_group, View.GONE);

                int smallIconResId = this.getSmallIcon(context);
                if (smallIconResId != 0) {
                    contentView.setViewVisibility(R.id.notification_small_icon, View.VISIBLE);
                    contentView.setImageViewResource(R.id.notification_small_icon, smallIconResId);
                }
            }

            contentView.setTextViewText(R.id.notification_content_title, message.getTitle());
            contentView.setTextViewText(R.id.notification_content_text, message.getText());

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				contentView.setTextViewTextSize(R.id.notification_content_title, TypedValue.COMPLEX_UNIT_SP, 14);
				contentView.setTextViewTextSize(R.id.notification_content_text, TypedValue.COMPLEX_UNIT_SP, 14);
			} else {
				contentView.setTextViewTextSize(R.id.notification_content_title, TypedValue.COMPLEX_UNIT_SP, 14);
				contentView.setTextViewTextSize(R.id.notification_content_text, TypedValue.COMPLEX_UNIT_SP, 14);
			}
		}
    }

    private static String getImageFileName(String url) {
        if (url == null) return null;

        return url.substring(url.lastIndexOf('/') + 1);
    }

    private static Bitmap resizeImageForDevice(Context context, Bitmap sourceBitmap) {
        Bitmap resizedBitmap = null;

        if (sourceBitmap != null) {
            if (sourceBitmap.getWidth() > sourceBitmap.getHeight()) {
                DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

                int newWidth = displayMetrics.widthPixels;
                int newHeight = newWidth / 2;

                resizedBitmap = scaleBitmapAndKeepRation(sourceBitmap, newWidth, newHeight);
            }
        }

        if (resizedBitmap == null) {
            resizedBitmap = sourceBitmap;
        }

        return resizedBitmap;
    }

    private static Bitmap scaleBitmapAndKeepRation(Bitmap targetBmp, int reqWidthInPixels, int reqHeightInPixels) {
        Matrix matrix = new Matrix();
        matrix.setRectToRect(new RectF(0, 0, targetBmp.getWidth(), targetBmp.getHeight()), new RectF(0, 0, reqWidthInPixels, reqHeightInPixels), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(targetBmp, 0, 0, targetBmp.getWidth(), targetBmp.getHeight(), matrix, true);
    }

    private static void downloadCarouselImages(Context context, Notification message) {
        if (context != null && message != null) {
            NotificationCarouselItem[] NotificationCarouselItems = message.getCarouselItems();
            if (NotificationCarouselItems != null) {
                for (NotificationCarouselItem element : NotificationCarouselItems) {
                    FileOutputStream fileOutputStream = null;
                    try {
                        if (element != null) {
                            URL imageURL = new URL(element.getMediaUrl());
                            Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());

                            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                            int ivWidth = displayMetrics.widthPixels;
                            int currentBitmapWidth = bitmap.getWidth();
                            int currentBitmapHeight = bitmap.getHeight();

                            bitmap = scaleBitmapAndKeepRation(bitmap, ivWidth, 90);

                            String imageUrl = element.getMediaUrl();
                            String fileName = getImageFileName(imageUrl);

                            if (!TextUtils.isEmpty(fileName)) {
                                if (bitmap != null) {
                                    fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                                    fileOutputStream.close();
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.e("CleverPush", e.getMessage());
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                Log.e("CleverPush", e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    private static Bitmap loadImageFromDisc(Context context, String fileName) {
        Bitmap bitmap = null;

        File imageFile = context.getFileStreamPath(fileName);
        if (imageFile.exists()) {
            InputStream inputStream = null;
            try {
                inputStream = context.openFileInput(fileName);
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            } catch (FileNotFoundException e) {
                Log.e("CleverPush", e.getMessage());
            } catch (IOException e) {
                Log.e("CleverPush", e.getMessage());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e("CleverPush", e.getMessage());
                    }
                }
            }
        }

        return bitmap;
    }
}
