package services;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import datastore.Store;

public class PlayInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = "PlayInstanceIDService";
    Store store;

    @Override
    public void onCreate() {
        super.onCreate();
        store = new Store();
    }

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken);
    }
    private void sendRegistrationToServer(String token) {
        store.setNotificationToken(token);
    }
}
