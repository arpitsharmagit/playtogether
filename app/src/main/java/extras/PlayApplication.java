package extras;

import android.Manifest;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.widget.Toast;

import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity;
import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import net.grandcentrix.tray.AppPreferences;

import java.util.ArrayList;
import java.util.List;

import datastore.Store;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import model.Contact;
import model.Song;
import model.User;
import services.PlayMusicService;

public class PlayApplication extends Application {
        public static Context context;
        public static User self;
        public static User other;
        public static ArrayList<Contact> contacts;
        public static ArrayList<Song> songs;
        public static List<User> users;
        public static Store store;
        public static PlayMusicService musicService = null;
        private static Intent musicServiceIntent = null;
        private static boolean activityVisible =true;
        private AppPreferences appPreferences;

        public void onCreate() {
            super.onCreate();
            appPreferences = new AppPreferences(this);
            PlayApplication.context = getApplicationContext();
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {
                contacts = UtilityFunctions.getContactList();
            }
            else{
                appPreferences.put("SyncContact",true);
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                songs = UtilityFunctions.getSongs();
            }
            store = new Store();
            startMusicService(getApplicationContext());
            ReactiveNetwork
                    .observeNetworkConnectivity(context)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Connectivity>() {
                        @Override
                        public void accept(Connectivity connectivity) throws Exception {
                            if(connectivity.state() != NetworkInfo.State.CONNECTED){
                                Toast.makeText(context,"Internet is not available.",Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

    @Override
    public void onTerminate() {
            stopMusicService(getApplicationContext());
        super.onTerminate();
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    public static ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayMusicService.MusicBinder binder = (PlayMusicService.MusicBinder)service;

            // Here's where we finally create the MusicService
            musicService = binder.getService();
            musicService.musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService.musicBound = false;
        }
    };

    public static void startMusicService(Context c) {

        if (musicServiceIntent != null)
            return;

        if (PlayApplication.musicService != null)
            return;

        // Create an intent to bind our Music Connection to
        // the MusicService.
        musicServiceIntent = new Intent(c, PlayMusicService.class);
        c.bindService(musicServiceIntent, musicConnection, Context.BIND_AUTO_CREATE);
        c.startService(musicServiceIntent);
    }
    public static void stopMusicService(Context c) {

        if (musicServiceIntent == null)
            return;

        c.stopService(musicServiceIntent);
        musicServiceIntent = null;

        PlayApplication.musicService = null;
    }

}
