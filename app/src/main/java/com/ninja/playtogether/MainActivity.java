package com.ninja.playtogether;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import net.grandcentrix.tray.AppPreferences;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.Nullable;

import adapters.CustomAdapter.UserRecyclerAdapter;
import adapters.UsersAdapter;
import agency.tango.android.avatarview.IImageLoader;
import agency.tango.android.avatarview.views.AvatarView;
import agency.tango.android.avatarviewglide.GlideLoader;
import datastore.Store;
import extras.PlayApplication;
import extras.UtilityFunctions;
import io.grpc.Server;
import model.Contact;
import model.User;
import model.UserStatus;

public class MainActivity extends AppCompatActivity {

    private final static String Tag = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseDatabase database ;
    private DatabaseReference consRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference connectedRef;
    private Toolbar mTopToolbar;
    private TextView mTitle;
    private AvatarView profileImage;

    private IImageLoader imageLoader;
    private RecyclerView messageRecyclerView;
    private ProgressBar progressBar;
    private UsersAdapter adapter;
    private Store store;
    private AppPreferences appPreferences;

    private SimpleDateFormat formater;

    static {
        FirebaseFirestore.setLoggingEnabled(false);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appPreferences = new AppPreferences(this);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        database = FirebaseDatabase.getInstance();
        formater = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        store =new Store();
        imageLoader =new GlideLoader();


        mTitle = findViewById(R.id.title);
        profileImage = findViewById(R.id.profile_pic);
        mTopToolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(mTopToolbar);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        messageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);


        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
        messageRecyclerView.addItemDecoration(new DividerItemDecoration(
                MainActivity.this,
                DividerItemDecoration.VERTICAL));
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser==null) {
            Intent intent = new Intent(this, PhoneAuthActivity.class);
            startActivity(intent);
        }
        else{
            if(PlayApplication.self==null) {
                firestore.collection("users")
                        .document(currentUser.getUid())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                User currentUser = documentSnapshot.toObject(User.class);
                                PlayApplication.self = currentUser;
                                loadUsers();
                                if (PlayApplication.self != null && PlayApplication.self.profilePictureUrl != null) {
                                    imageLoader.loadImage(profileImage, PlayApplication.self.profilePictureUrl, PlayApplication.self.username);
                                }
                            }
                        });

            }
            else {
                loadUsers();
                if (PlayApplication.self != null && PlayApplication.self.profilePictureUrl != null) {
                    imageLoader.loadImage(profileImage, PlayApplication.self.profilePictureUrl, PlayApplication.self.username);
                }
            }
        }

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                ).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {
                if(report.areAllPermissionsGranted()){
                    getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI,
                            true,
                            new ContentObserver(null) {
                                @Override
                                public void onChange(boolean selfChange) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            appPreferences.put("SyncContact",true);
                                            PlayApplication.contacts = UtilityFunctions.getContactList();
                                            loadUsers();
                                            syncContacts();
                                        }
                                    });
                                    super.onChange(selfChange);
                                }
                            });

                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    if(!musicDir.exists()){
                        musicDir.mkdir();
                    }
                    if(PlayApplication.contacts == null){
                        PlayApplication.contacts = UtilityFunctions.getContactList();
                    }
                    if(PlayApplication.contacts != null && appPreferences.getBoolean("SyncContact",false)) {
                        loadUsers();
                        syncContacts();
                    }
                    if(PlayApplication.songs ==null)
                        PlayApplication.songs = UtilityFunctions.getSongs();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(
                    List<com.karumi.dexter.listener.PermissionRequest> permissions,
                    PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();
    }

    private void loadUsers(){
        firestore.collection("users")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(Tag, "Listen failed.", e);
                            return;
                        }

                        ArrayList<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot snapshot : value) {
                            User user = snapshot.toObject(User.class);
                            Contact contact = new Contact();
                            contact.phoneNumber = user.phone;
                            if((PlayApplication.contacts!=null &&
                                    PlayApplication.contacts.contains(contact)) &&
                                    (PlayApplication.self!=null && !user.equals(PlayApplication.self))) {
                                users.add(user);
                            }
                            if(PlayApplication.contacts==null &&
                                    (PlayApplication.self!=null && !user.equals(PlayApplication.self))){
                                users.add(user);
                            }
                        }
                        Log.d(Tag, "Current users in DB: " + users.size());
                        progressBar.setVisibility(View.GONE);
                        adapter = new UsersAdapter(MainActivity.this, users, new UsersAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(User user) {
                                PlayApplication.other = user;
                                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                                startActivity(intent);
                            }
                        });
                        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                            @Override
                            public void onItemRangeInserted(int positionStart, int itemCount) {
                                super.onItemRangeInserted(positionStart, itemCount);
                                int UserCount = adapter.getItemCount();
                                int lastVisiblePosition = mLayoutManager.findLastCompletelyVisibleItemPosition();
                                if (lastVisiblePosition == -1 ||
                                        (positionStart >= (UserCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                                    messageRecyclerView.scrollToPosition(positionStart);
                                }
                            }
                        });
                        messageRecyclerView.setLayoutManager(mLayoutManager);
                        messageRecyclerView.setItemAnimator(new DefaultItemAnimator());
                        messageRecyclerView.setAdapter(adapter);
                    }
                });

    }
    private void syncContacts() {
        if(PlayApplication.self != null && appPreferences.getBoolean("SyncContact",false)) {
            //fire sync contact job
            new AsyncJob.AsyncJobBuilder<Boolean>()
                    .doInBackground(new AsyncJob.AsyncAction<Boolean>() {
                        @Override
                        public Boolean doAsync() {
                            try {
                                for (Contact contact : PlayApplication.contacts
                                        ) {
                                    store.saveContact(contact);
                                    Thread.sleep(100);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    })
                    .doWhenFinished(new AsyncJob.AsyncResultAction<Boolean>() {
                        @Override
                        public void onResult(Boolean result) {
                            appPreferences.put("SyncContact", false);
                        }
                    }).create().start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sign_out) {
            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle("Sign Out?")
                    .setMessage("Are you sure you want to Sign Out?")
                    .setNegativeButton(getString(R.string.no), null)
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            signOut();
                        }
                    }).create().show();

            return true;
        }

        if (id == R.id.action_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onPause() {
        if(PlayApplication.self!=null) {
            PlayApplication.self.state = "Offline";
            PlayApplication.self.last_changed = formater.format(Calendar.getInstance().getTime());
            store.saveUser(PlayApplication.self);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if(PlayApplication.self!=null) {
            PlayApplication.self.state = "Online";
            PlayApplication.self.last_changed = formater.format(Calendar.getInstance().getTime());
            store.saveUser(PlayApplication.self);
        }
        super.onResume();
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        finish();
    }
}
