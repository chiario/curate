package com.example.curate.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.curate.R;
import com.example.curate.activities.MainActivity;

public class NotificationHelper {
	private static final String CHANNEL_ID = "CurateChannel";
	private static final int NOTIFICATION_ID = 70; // Only ever show 1 notification

	// Timings subject to change
	private static final int NOTIFICATION_THRESHOLD = 10 * 60 * 1000; // 10 minutes in millis
	private static final int NOTIFICATION_CHECK_TIME = 60 * 1000; // 1 minute in millis

	private static Handler notificationHandler;

	private static long lastInteractionTime;

	private static Context mContext;

	public static void initializeNotifications(Context context) {
		lastInteractionTime = SystemClock.elapsedRealtime();
		mContext = context;
		createNotificationChannel(mContext);
		notificationHandler = new Handler();
		notificationHandler.post(addSongsNotification);
	}

	private static void createNotification(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_notification)
				.setColor(context.getResources().getColor(R.color.colorAccent))
				.setContentTitle("Add a song to party!")
				.setContentText("It's been a while, don't miss out on the fun")
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				// Set the intent that will fire when the user taps the notification
				.setContentIntent(pendingIntent)
				.setAutoCancel(true);
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
		updateInteractionTime();
	}

	private static void createNotificationChannel(Context context) {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

			// Return if notification channel is already created
			if(notificationManager.getNotificationChannel(CHANNEL_ID) != null) return;

			String name = "Curate Channel";
			String description = "Notifies user to add song";
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);

			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			notificationManager.createNotificationChannel(channel);
		}
	}

	public static void removeNotifications(Context context) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
				notificationManager.deleteNotificationChannel(CHANNEL_ID);
			}
			notificationHandler.removeCallbacks(addSongsNotification);
	}

	private static Runnable addSongsNotification = new Runnable() {
		@Override
		public void run() {
			if(SystemClock.elapsedRealtime() - lastInteractionTime > NOTIFICATION_THRESHOLD) createNotification(mContext);

			// Check interactions in NOTIFICATION_CHECK_TIME millis
			new Handler().postDelayed(addSongsNotification, NOTIFICATION_CHECK_TIME);
		}
	};

	public static void updateInteractionTime() {
		lastInteractionTime = SystemClock.elapsedRealtime();
	}
}
