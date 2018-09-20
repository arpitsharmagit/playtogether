package extras;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

public class ChatNotification {

    Context context;
    String channelId;
    static int NOTIFICATION_ID =401;

    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;
    NotificationCompat.MessagingStyle messagingStyle;

    boolean mStarted;

    public ChatNotification(Context context){
        this.context = context;
        channelId = "fcm_default_channel";
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        messagingStyle = new NotificationCompat.MessagingStyle("Unknown")
                .setConversationTitle("New Messages");
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        //
        Intent resultIntent = new Intent(context, ChatActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent piResult = PendingIntent.getActivity(context, 0, resultIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_music_notify)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("PlayTogether")
                .setContentText("Messages")
//                .setDefaults(Notification.DEFAULT_SOUND)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentIntent(piResult);
    }

    public void notifyMessage(String from, String text) {

        messagingStyle
                .addMessage(text, Calendar.getInstance().getTime().getTime(), from);// Pass in null for user.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,channelId,NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(NOTIFICATION_ID,notificationBuilder.setStyle(messagingStyle).build());
    }

    public void cancel(int notificationID) {
        notificationManager.cancel(notificationID);
    }

}
