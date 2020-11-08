package extras;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.ninja.playtogether.ChatActivity;
import com.ninja.playtogether.R;

import model.Song;
import services.PlayMusicService;

public class MusicNotification {
    Context context;
    String channelId;
    int NOTIFICATION_ID =1001;

    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;
    PlayMusicService mService;

    PlaybackStateCompat mPlaybackState;
    MediaMetadataCompat mMetadata;
    boolean mStarted;

    public MusicNotification(Context context){
        this.context = context;
        channelId = "fcm_default_channel";
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    }

    public void notifySong(Context context, PlayMusicService service, Song song) {
        if (this.context == null)
            this.context = context;
        if (this.mService == null)
            this.mService = service;
        Bitmap bitmap;
        if (song.cover != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeFile(song.cover, options);
        }
        else{
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_music_default);
        }

        mPlaybackState = service.getmMediaSession().getController().getPlaybackState();

        mMetadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                        song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                .build();
        Notification notification = createNotification();
        if (notification != null) {
            notificationManager.notify(NOTIFICATION_ID,notification);
        }
    }

    private Notification createNotification() {
        if (mMetadata == null || mPlaybackState == null) {
            return null;
        }

        notificationBuilder = new NotificationCompat.Builder(context, channelId);
        MediaDescriptionCompat description = mMetadata.getDescription();

        addPlayPauseAction(notificationBuilder);

        Intent notifyIntent = new Intent(context, ChatActivity.class);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Letting the Intent be executed later by other application.
        PendingIntent pendingIntent = PendingIntent.getActivity
                (context,0,notifyIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder
                .setContentIntent(pendingIntent)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                //.setSubText(description.getDescription())
                .setOngoing(true)
                .setTicker("Playing '" + description.getTitle() + "' from '" + description.getSubtitle() + "'")
                .setShowWhen(false)
                .setLargeIcon(description.getIconBitmap())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_play)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        //setShowActionsInCompactView(0 /* #1: pause button */)
                        .setMediaSession(mService.getmMediaSession().getSessionToken()));
        setNotificationPlaybackState(notificationBuilder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,channelId,NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        return notificationBuilder.build();
    }
    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        String label;
        int icon;
        PendingIntent intent;
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            label = "Pause";
            icon = R.drawable.ic_pause;
            intent = getPendingIntent(PlayMusicService.ACTION_PAUSE);
            builder.setOngoing(false);
        } else {
            label = "Play";
            icon = R.drawable.ic_play;
            intent = getPendingIntent(PlayMusicService.ACTION_PLAY);
            builder.setOngoing(true);
        };

//        builder.addAction(icon, label, intent);
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        if (mPlaybackState == null || !mStarted) {
            mService.stopForeground(true);
            return;
        }
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING
                && mPlaybackState.getPosition() >= 0) {
            builder.setWhen(System.currentTimeMillis() - mPlaybackState.getPosition()).setShowWhen(true).setUsesChronometer(true);
        } else {
            builder.setWhen(0).setShowWhen(false).setUsesChronometer(false);
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mPlaybackState.getState() == PlaybackState.STATE_PLAYING);
    }

    private PendingIntent getPendingIntent(String intentType){
        Intent intent = new Intent(context, PlayMusicService.class);
        intent.setAction(intentType);
        return PendingIntent.getService(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void notifyPaused(boolean isPaused) {
        if (notificationBuilder == null)
            return;

        String label;
        int icon;
        PendingIntent intent;
        if (!isPaused) {
            label = "Pause";
            icon = R.drawable.ic_pause;
            intent = getPendingIntent(PlayMusicService.ACTION_PAUSE);
            notificationBuilder.setOngoing(false);
        } else {
            label = "Play";
            icon = R.drawable.ic_play;
            intent = getPendingIntent(PlayMusicService.ACTION_PLAY);
            notificationBuilder.setOngoing(true);
        };
        //notificationBuilder.mActions.clear();
        //notificationBuilder.addAction(icon, label, intent);

		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        // Sets the notification to run on the foreground.
        // (why not the former commented line?)
        mService.startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Cancels this notification.
     */
    public void cancel() {
        mService.stopForeground(true);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public void stopNotification(){
        notificationManager.cancel(NOTIFICATION_ID);
    }
    /**
     * Cancels all sent notifications.
     */
    public static void cancelAll(Context c) {
        NotificationManager manager = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
    }

    private final MediaControllerCompat.Callback mCb = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            Notification notification = createNotification();
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID,notification);
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            mPlaybackState = state;
            if (state.getState() == PlaybackState.STATE_STOPPED ||
                    state.getState() == PlaybackState.STATE_NONE) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID,notification);
                }
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }
    };
}
