package extras;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.ninja.playtogether.R;

public class DownloadNotification {

    Context context;
    String channelId;
    static int NOTIFICATION_ID =2001;

    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;

    boolean mStarted;

    public DownloadNotification(Context context){
        this.context = context;
        channelId = "fcm_default_channel";
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public int notifyDownload( String title, String text) {
        if (this.context == null)
            this.context = context;
        Notification notification = createNotification(title,text);
        if (notification != null) {
            NOTIFICATION_ID++;
            notificationManager.notify(NOTIFICATION_ID,notification);
        }
        return NOTIFICATION_ID;
    }

    private Notification createNotification(String title, String text) {

        notificationBuilder = new NotificationCompat.Builder(context, channelId);

        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_music_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setColor(ContextCompat.getColor(context, R.color.colorRed))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,channelId,NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        return notificationBuilder.build();
    }

    public void notifyProgress(int notificationId,int progress) {
        if (notificationBuilder == null)
            return;

        if(progress == 100) {
            notificationBuilder.setProgress(0, 0, false);
        }
        else {
            notificationBuilder.setProgress(100, progress, false);
        }

        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    public void cancel(int notificationID) {
        notificationManager.cancel(notificationID);
    }

    public static void cancelAll(Context c) {
        NotificationManager manager = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
    }
}
