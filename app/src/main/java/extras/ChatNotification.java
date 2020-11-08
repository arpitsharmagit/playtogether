package extras;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.ninja.playtogether.ChatActivity;
import com.ninja.playtogether.R;

import java.util.Calendar;

import services.NotificationReceiver;

public class ChatNotification {

    Context context;
    String channelId;
    static int NOTIFICATION_ID =401;

    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;
    NotificationCompat.MessagingStyle messagingStyle;
    String Title = "PlayTogether";

    boolean mStarted;

    public ChatNotification(Context context){
        this.context = context;
        channelId = "fcm_default_channel";
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        messagingStyle = new NotificationCompat.MessagingStyle("Unknown")
                .setConversationTitle("New Messages");
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Intent resultIntent = new Intent(context, ChatActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
        PendingIntent piResult = PendingIntent.getActivity(context, 0, resultIntent, 0);
        notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_music_default)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("PlayTogether")
                .setContentText("Messages")
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentIntent(piResult);
        if(Build.VERSION.SDK_INT> Build.VERSION_CODES.KITKAT)
        {
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        }
    }

    public PendingIntent getLaunchIntent(int notificationId, Context context) {

        Intent intent = new Intent(context, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("notificationId", notificationId);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void headsUpNotification(String from, String text) {

        int NOTIFICATION_ID = 1;
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_music_default)
                        .setContentTitle(from)
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.journaldev.com/15126/swift-function"));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Intent buttonIntent = new Intent(context, NotificationReceiver.class);
        buttonIntent.putExtra("notificationId", NOTIFICATION_ID);
        PendingIntent dismissIntent = PendingIntent.getBroadcast(context, 0, buttonIntent, 0);

        builder.addAction(android.R.drawable.ic_menu_view, "VIEW", pendingIntent);
        builder.addAction(android.R.drawable.ic_delete, "DISMISS", dismissIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void clearNotification(Intent intent) {
        int notificationId = intent.getIntExtra("notificationId", 0);
        notificationManager.cancel(notificationId);
    }

    public void messageStyleNotification(String from, String text) {
        int NOTIFICATION_ID = 1;

        PendingIntent launchIntent = getLaunchIntent(NOTIFICATION_ID, context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        builder.setSmallIcon(R.drawable.ic_music_default);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_music_default));
        builder.setContentTitle("Messages");
        builder.setStyle(new NotificationCompat.MessagingStyle(from).setConversationTitle("Messages")
                .addMessage(text, Calendar.getInstance().getTime().getTime(), from));
        builder.setAutoCancel(true);
        builder.setContentIntent(launchIntent);

        // Will display the notification in the notification bar
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void notifyMessage(String from, String text) {

        messagingStyle
                .addMessage(text, Calendar.getInstance().getTime().getTime(), from);// Pass in null for user.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,channelId,NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        notificationBuilder.setStyle(messagingStyle);
        notificationManager.notify(NOTIFICATION_ID,notificationBuilder.build());
    }

    public void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

}
