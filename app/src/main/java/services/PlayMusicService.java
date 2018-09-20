package services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.ninja.playtogether.ChatActivity;

import java.io.IOException;

import extras.MusicNotification;
import model.Song;

public class PlayMusicService
        extends IntentService
        implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener{

    public static final String BROADCAST_EXTRA_STATE = "service_state";
    public static final String BROADCAST_EXTRA_SONG_ID = "song_id";
    public static final String BROADCAST_EXTRA_PLAYING = "playing";
    public static final String BROADCAST_EXTRA_PAUSED = "paused";
    public static final String BROADCAST_EXTRA_UNPAUSED = "unpaused";
    public static final String BROADCAST_EXTRA_COMPLETED = "completed";


    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_TOGGLE_PLAYBACK = "action_toggle_playback";

    private MediaPlayer player;
    public Song currentSong = null;
    final static String TAG = "MusicService";
    ServiceState serviceState = ServiceState.Preparing;
    AudioManager audioManager;

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;

    private MusicNotification notification;

    private boolean isReady = false;

    enum ServiceState {
        Stopped,
        Preparing,
        Playing,
        Paused
    }

    public PlayMusicService() {
        super("PlayMusicService");
    }

    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        initMusicPlayer();
        Context context = getApplicationContext();

        IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetBroadcastReceiver, headsetFilter);

        mMediaSession = new MediaSessionCompat(this, "MusicService");

        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE);

        mMediaSession.setPlaybackState(mStateBuilder.build());

        Intent intent = new Intent(context, ChatActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setSessionActivity(pi);

        Bundle mSessionExtras = new Bundle();
        mMediaSession.setExtras(mSessionExtras);

        mMediaSession.setCallback(
                new MediaSessionCompat.Callback() {
                    @Override
                    public void onPlay() {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        Bitmap bitmap = BitmapFactory.decodeFile(currentSong.cover, options);

                        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,currentSong.title)
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                        currentSong.artist)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                                        currentSong.album)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentSong.duration)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
                                .build();
                        mMediaSession.setMetadata(metadata);
                        mMediaSession.setActive(true);
                        playSong();
                    }
                    @Override
                    public void onSeekTo(long position) {
                        player.seekTo((int)position);
                    }

                    @Override
                    public void onStop() {
                        mMediaSession.setActive(false);
                        stopMusicPlayer();
                        stopForeground(false);
                    }

                    @Override
                    public void onPause() {
                        pausePlayer();
                        stopForeground(false);
                    }
                });

        Log.w(TAG, "onCreate");
    }

    public void initMusicPlayer() {
        if (player == null)
            player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

        Log.w(TAG, "initMusicPlayer");
    }

    public void stopMusicPlayer() {
        if (player == null)
            return;

        player.stop();
        player.release();
        player = null;

        Log.w(TAG, "stopMusicPlayer");
    }

    public void setSong(Song song){
        this.currentSong =song;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(song.cover, options);

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                        song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                .build();
        mMediaSession.setMetadata(metadata);
        mMediaSession.setActive(true);

        player.reset();

        Song songToPlay = currentSong;

        Uri songToPlayURI = ContentUris.withAppendedId
                (android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        songToPlay.id);

        try {
            player.setDataSource(getApplicationContext(), songToPlayURI);
        }
        catch(IOException io) {
            Log.e(TAG, "IOException: couldn't change the song", io);
            destroySelf();
        }
        catch(Exception e) {
            Log.e(TAG, "Error when changing the song", e);
            destroySelf();
        }

        player.prepareAsync();
        serviceState = ServiceState.Preparing;
    }

    private boolean requestAudioFocus() {
        //Request audio focus for playback
        int result = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        //Check if audio focus was granted. If not, stop the service.
        return (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    }
    BroadcastReceiver headsetBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // Headphones just connected (or not)
            if (action.equals(Intent.ACTION_HEADSET_PLUG)) {

                Log.w(TAG, "headset plug");
                boolean connectedHeadphones = (intent.getIntExtra("state", 0) == 1);
                boolean connectedMicrophone = (intent.getIntExtra("microphone", 0) == 1) && connectedHeadphones;

                // User just connected headphone and the player was paused,
                // so we shoud restart the music.
                if (connectedMicrophone && (serviceState == ServiceState.Paused)) {

                    // Will only do it if it's Setting is enabled, of course
                        LocalBroadcastManager local = LocalBroadcastManager.getInstance(context);

                        Intent broadcastIntent = new Intent(context,PlayMusicService.class);
                        broadcastIntent.setAction(PlayMusicService.ACTION_PLAY);
                        local.sendBroadcast(broadcastIntent);

                }

                // I wonder what's this for
                String headsetName = intent.getStringExtra("name");

                if (connectedHeadphones) {
                    String text = "headset plugged In";

                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    public static class ExternalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.w(TAG, "external broadcast");

            // Broadcasting orders to our MusicService
            // locally (inside the application)
            LocalBroadcastManager local = LocalBroadcastManager.getInstance(context);

            String action = intent.getAction();

            // Headphones disconnected
            if (action.equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {

                // ADD SETTINGS HERE
                String text = "Headphone disconnected";
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

                // send an intent to our MusicService to telling it to pause the audio
                Intent broadcastIntent = new Intent(context, PlayMusicService.class);
                intent.setAction(ACTION_PAUSE);

                local.sendBroadcast(broadcastIntent);
                Log.w(TAG, "becoming noisy");
                return;
            }

            if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {

                // Which media key was pressed
                KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);

                // Not interested on anything other than pressed keys.
                if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                    return;

                String intentValue = null;

                switch (keyEvent.getKeyCode()) {

                    case KeyEvent.KEYCODE_HEADSETHOOK:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        intentValue = ACTION_TOGGLE_PLAYBACK;
                        Log.w(TAG, "media play pause");
                        break;

                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        intentValue = ACTION_PLAY;
                        Log.w(TAG, "media play");
                        break;

                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        intentValue = ACTION_PAUSE;
                        Log.w(TAG, "media pause");
                        break;

                    case KeyEvent.KEYCODE_MEDIA_NEXT:
//                        intentValue = ServicePlayMusic.BROADCAST_ORDER_SKIP;
                        Log.w(TAG, "media next");
                        break;

                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        // TODO: ensure that doing this in rapid succession actually plays the
                        // previous song
//                        intentValue = ServicePlayMusic.BROADCAST_ORDER_REWIND;
                        Log.w(TAG, "media previous");
                        break;
                }

                // Actually sending the Intent
                if (intentValue != null) {
                    Intent broadcastIntent = new Intent(context, PlayMusicService.class);
                    intent.setAction(intentValue);

                    local.sendBroadcast(broadcastIntent);
                }
            }
        }
    }
    BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String Action = intent.getAction();

            if (Action == null) {
                return;
            }
            switch (Action) {
                case ACTION_PLAY:
                    unpausePlayer();
                    break;
                case ACTION_PAUSE:
                    pausePlayer();
                    break;
                case ACTION_TOGGLE_PLAYBACK:
                    togglePlayback();
                    break;
            }

            Log.w(TAG, "local broadcast received");
        }
    };


    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.w(TAG, "audiofocus gain");

                if (player == null)
                    initMusicPlayer();

                if (pausedTemporarilyDueToAudioFocus) {
                    pausedTemporarilyDueToAudioFocus = false;
                    unpausePlayer();
                }

                if (loweredVolumeDueToAudioFocus) {
                    loweredVolumeDueToAudioFocus = false;
                    player.setVolume(1.0f, 1.0f);
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                Log.w(TAG, "audiofocus loss");

                // Giving up everything
                //audioManager.unregisterMediaButtonEventReceiver(mediaButtonEventReceiver);
                //audioManager.abandonAudioFocus(this);

                //pausePlayer();
                stopMusicPlayer();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.w(TAG, "audiofocus loss transient");

                if (! isPaused()) {
                    pausePlayer();
                    pausedTemporarilyDueToAudioFocus = true;
                }
                break;

            // Temporarily lost audio focus but I can keep it playing
            // at a low volume instead of stopping completely
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.w(TAG, "audiofocus loss transient can duck");

                player.setVolume(0.1f, 0.1f);
                loweredVolumeDueToAudioFocus = true;
                break;
        }
    }
    // Internal flags for the function above {{
    private boolean pausedTemporarilyDueToAudioFocus = false;
    private boolean loweredVolumeDueToAudioFocus     = false;
    // }}

    @Override
    public void onPrepared(MediaPlayer mp) {
        mStateBuilder
                .setState(PlaybackStateCompat.STATE_PLAYING, player.getCurrentPosition(), 1)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        mMediaSession.setPlaybackState(mStateBuilder.build());

        isReady =true;

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        mStateBuilder.setState(PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSession.setPlaybackState(mStateBuilder.build());
        serviceState = ServiceState.Stopped;

        isReady = false;
        if(notification!=null)
            notification.stopNotification();
        destroySelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        Log.w(TAG, "onError");
        return false;
    }

    @Override
    public void onDestroy() {
        Context context = getApplicationContext();

        cancelNotification();

        currentSong = null;

        if (audioManager != null)
            audioManager.abandonAudioFocus(this);

        stopMusicPlayer();
        mMediaSession.release();

        Log.w(TAG, "onDestroy");
        super.onDestroy();
    }
    private void destroySelf() {
        stopSelf();
        currentSong = null;
    }
    public int getPosition() {
        return player.getCurrentPosition();
    }

    public int getDuration() {
        return player.getDuration();
    }

    public boolean isPlaying() {
        boolean returnValue = false;

        try {
            returnValue = player.isPlaying();
        }
        catch (IllegalStateException e) {
            player.reset();
            player.prepareAsync();
        }

        return returnValue;
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        String Action = intent.getAction();

        if (Action == null) {
            return;
        }
        switch (Action) {
            case ACTION_PLAY:
                unpausePlayer();
                break;
            case ACTION_PAUSE:
                pausePlayer();
                break;
            case ACTION_TOGGLE_PLAYBACK:
                togglePlayback();
                break;
        }

        Log.w(TAG, "Intent received");
    }
    public boolean isPaused() {
        return serviceState == ServiceState.Paused;
    }
    public void playSong() {
        if(isReady) {
            serviceState = ServiceState.Playing;
            player.start();
            notifyCurrentSong();

            broadcastState(PlayMusicService.BROADCAST_EXTRA_PLAYING);
            Log.w(TAG, "play song");
        }
    }

    public void pausePlayer() {
        if (serviceState != ServiceState.Paused && serviceState != ServiceState.Playing)
            return;

        player.pause();
        mStateBuilder
                .setState(PlaybackStateCompat.STATE_PAUSED, player.getCurrentPosition(), 0)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        notification.notifyPaused(true);

        mMediaSession.setActive(false);
        serviceState = ServiceState.Paused;

        broadcastState(PlayMusicService.BROADCAST_EXTRA_PAUSED);
    }

    public void unpausePlayer() {
        if (serviceState != ServiceState.Paused && serviceState != ServiceState.Playing)
            return;

        player.start();
        serviceState = ServiceState.Playing;
        mStateBuilder
                .setState(PlaybackStateCompat.STATE_PLAYING, player.getCurrentPosition(), 1)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        mMediaSession.setPlaybackState(mStateBuilder.build());

        notification.notifyPaused(false);

        mMediaSession.setActive(true);

        broadcastState(PlayMusicService.BROADCAST_EXTRA_UNPAUSED);
    }
    public void togglePlayback() {
        if (serviceState == ServiceState.Paused)
            unpausePlayer();
        else
            pausePlayer();
    }

    public void seekTo(int position) {
        player.seekTo(position);
    }


    public boolean musicBound = false;

    public class MusicBinder extends Binder {
        public PlayMusicService getService() {
            return PlayMusicService.this;
        }
    }

    private final IBinder musicBind = new MusicBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return false;
    }

    public void notifyCurrentSong() {
        if (currentSong == null)
            return;

        if (notification == null)
            notification = new MusicNotification(getApplicationContext());

        notification.notifySong(this, this, currentSong);
    }
    public void cancelNotification() {
        if (notification == null)
            return;

        notification.cancel();
        notification = null;
    }

    public MediaSessionCompat getmMediaSession() {
        return mMediaSession;
    }
    private void broadcastState(String state) {
        if (currentSong == null)
            return;
        Context context = getApplicationContext();
        Intent broadcastIntent = new Intent(context,PlayMusicService.class);

        broadcastIntent.putExtra(PlayMusicService.BROADCAST_EXTRA_STATE,   state);
        broadcastIntent.putExtra(PlayMusicService.BROADCAST_EXTRA_SONG_ID, currentSong.id);

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(broadcastIntent);

        Log.w(TAG, "sentBroadcast");
    }
}
