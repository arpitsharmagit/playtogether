package com.ninja.playtogether;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.ObservableSnapshotArray;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jakewharton.rxbinding.widget.RxTextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import agency.tango.android.avatarview.IImageLoader;
import agency.tango.android.avatarview.views.AvatarView;
import agency.tango.android.avatarviewglide.GlideLoader;
import datastore.Store;
import extras.ChatNotification;
import extras.DownloadNotification;
import extras.PlayApplication;
import extras.UtilityFunctions;
import model.RemoteAction;
import model.Message;
import model.Song;
import model.User;
import rx.functions.Action1;
import rx.functions.Func1;
import services.PlayMusicController;
import services.PlayMusicService;
import views.SongUploaderIconView;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static android.text.format.DateUtils.WEEK_IN_MILLIS;

public class ChatActivity extends AppCompatActivity
    implements MediaController.MediaPlayerControl {

    private final static String Tag = "ChatActivity";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 100;

    private final String DOWNLOAD="download";
    private final String ERROR="error";
    private final String DOWNLOADING="downloading";
    private final String SETUP = "setup";
    private final String PLAY="play";
    private final String PAUSE="pause";
    private final String SEEK="seek";

    private Toolbar mTopToolbar;
    private TextView mTitle,mStatus;

    private LinearLayout downloadView;
    private TextView downloadName,downloadPercentage,viewTyping;
    private ProgressBar downloadProgress;
    private Store store;
    private ImageButton mSendButton;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private RecyclerView messageList;

    private StorageReference mStorage;
    private FirebaseFirestore firestore;
    private FirestoreRecyclerAdapter adapter;
    ListenerRegistration registration;

    private boolean paused = false;
    private DownloadNotification downloadNotification;
    private ArrayList<Integer> downloadNotificationIds;
    private IImageLoader imageLoader;

    private LinearLayout currentSongView;
    private ImageView albumArt;
    private TextView songName,artistName,songDuration;
    private SeekBar seekBar;
    private SongUploaderIconView uploaderIconView;
    private AppCompatImageButton btnPlayPause;

    Handler handler = new Handler();
    private Song currentSong;
    private boolean mDragging,typingStarted;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private long newposition;
    private ListenerRegistration listenerRegistration,chatListener;
    private ChatNotification chatNotification;

    private SimpleDateFormat formater;

    @Override
    protected void onStart() {
        super.onStart();
        if(PlayApplication.other!=null){
            imageLoader =new GlideLoader();
            AvatarView profileOther = findViewById(R.id.profile_pic);
            profileOther.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ChatActivity.this, ShowPictureActivity.class);
                    startActivity(intent);
                }
            });
            imageLoader.loadImage(profileOther, PlayApplication.other.profilePictureUrl, PlayApplication.other.username);
        }
        setupChatView();
        attachListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mStorage =  FirebaseStorage.getInstance().getReference();
        formater = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        downloadNotification = new DownloadNotification(this);
        downloadNotificationIds = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();
        store =PlayApplication.store;
        chatNotification = new ChatNotification(this);

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        downloadView = findViewById(R.id.downloadView);
        downloadName = findViewById(R.id.download_name);
        downloadPercentage = findViewById(R.id.download_percent);
        downloadProgress = findViewById(R.id.download_progressBar);

        currentSongView = findViewById(R.id.current_song_view);
        albumArt = findViewById(R.id.album_art);
        songName = findViewById(R.id.song_name);
        artistName = findViewById(R.id.artist_name);
        songDuration = findViewById(R.id.song_duration);
        seekBar = findViewById(R.id.seek_bar);
        uploaderIconView=findViewById(R.id.icon_song_upload);
        uploaderIconView.init();
        uploaderIconView.setOnClickListener(null);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        viewTyping = findViewById(R.id.view_typing);

        seekBar.setOnSeekBarChangeListener(mSeekListener);
        seekBar.setMax(1000);

        if(currentSong ==null){
            currentSongView.setVisibility(View.GONE);
        }

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(PlayApplication.musicService.isPlaying()){
                    store.setAction(new RemoteAction(PAUSE,currentSong.title));
                    handler.removeCallbacks(mShowProgress);
                }
                else
                {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    store.setAction(new RemoteAction(PLAY,currentSong.title));
                    handler.post(mShowProgress);
                }
            }
        });

        mTitle = findViewById(R.id.title);
        mStatus =findViewById(R.id.title_sub);
        mTitle.setText(PlayApplication.other.username);
        mTopToolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(mTopToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mProgressBar =findViewById(R.id.progressBar);
        messageList = findViewById(R.id.message_list);

    }


    private void setupChatView(){
        if(PlayApplication.other.state.equals("Online")) {
            mStatus.setText(PlayApplication.other.state);
        }
        else{
            mStatus.setText(PlayApplication.other.last_changed);
        }

        firestore.collection("users").document(PlayApplication.other.id)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot snapshot,
                                        @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(Tag, "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            User other = snapshot.toObject(User.class);
                            if(other.isTyping){
                                viewTyping.setVisibility(View.VISIBLE);
                            }
                            else{
                                viewTyping.setVisibility(View.GONE);
                            }
                            if(other.state.equals("Online")) {
                                mStatus.setText(other.state);
                            }
                            else{
                                mStatus.setText(other.last_changed);
                            }
                            Log.d(Tag, "Current data: " + snapshot.getData());
                        } else {
                            Log.d(Tag, "Current data: null");
                        }
                    }
                });

        LinearLayoutManager linearLayoutManager =new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, final int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ( bottom < oldBottom) {
                    messageList.post(new Runnable() {
                        @Override
                        public void run() {
                            messageList.smoothScrollToPosition(bottom);
                        }
                    });
                }

            }
        });
        messageList.setLayoutManager(linearLayoutManager);

        CollectionReference from =firestore.collection("users")
                .document(PlayApplication.self.id)
                .collection("conversations")
                .document(PlayApplication.other.id)
                .collection("messages");

        Query query = firestore.collection("users")
                .document(PlayApplication.self.id)
                .collection("conversations")
                .document(PlayApplication.other.id)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        chatListener = query.addSnapshotListener(MetadataChanges.EXCLUDE, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }

                List<DocumentChange> changes = queryDocumentSnapshots.getDocumentChanges((MetadataChanges.EXCLUDE));
                for (DocumentChange change : changes) {
                    if(change.getType() == DocumentChange.Type.ADDED && PlayApplication.isActivityVisible() ==false){
                        Message msg = change.getDocument().toObject(Message.class);
                        Log.w(Tag,"ShowNotification");
                        chatNotification.notifyMessage(PlayApplication.other.username,msg.getText());
                    }
                }
            }
        });


        FirestoreRecyclerOptions<Message> options = new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Message, RecyclerView.ViewHolder>(options) {
            private static final int VIEW_TYPE_MESSAGE_SENT = 1;
            private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
            @Override
            public void onDataChanged() {
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                }
            }
            @Override
            public int getItemViewType(int position) {
                Message message = this.getItem(position);
                if(message.getName().equals(PlayApplication.self.username)){
                    return VIEW_TYPE_MESSAGE_SENT;
                } else {
                    return VIEW_TYPE_MESSAGE_RECEIVED;
                }
            }
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view;
                if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_message_sent, parent, false);
                    return new SentMessageHolder(view);
                } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_message_received, parent, false);
                    return new ReceivedMessageHolder(view);
                }

                return null;
            }

            @Override
            protected void onBindViewHolder(RecyclerView.ViewHolder holder,
                                            int position,
                                            Message message) {
                switch (holder.getItemViewType()) {
                    case VIEW_TYPE_MESSAGE_SENT:
                        ((SentMessageHolder) holder).bind(message);
                        break;
                    case VIEW_TYPE_MESSAGE_RECEIVED:
                        ((ReceivedMessageHolder) holder).bind(message);
                }

                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            }

            class SentMessageHolder extends RecyclerView.ViewHolder {
                TextView messageText, timeText;

                SentMessageHolder(View itemView) {
                    super(itemView);

                    messageText = itemView.findViewById(R.id.text_message_body);
                    timeText = itemView.findViewById(R.id.text_message_time);
                }

                void bind(Message message) {
                    messageText.setText(message.getText());
                    Date timeStamp = message.getTimestamp();
                    CharSequence formatedTime = DateUtils.formatSameDayTime(timeStamp.getTime(),
                            Calendar.getInstance().getTimeInMillis(),
                            DateFormat.FULL,DateFormat.SHORT);
                    if(!DateUtils.isToday(timeStamp.getTime())){
                        formatedTime = DateUtils.getRelativeDateTimeString(
                                PlayApplication.context,
                                message.getTimestamp().getTime(),
                                SECOND_IN_MILLIS,
                                WEEK_IN_MILLIS, 0);
                    }
                    timeText.setText(formatedTime);
                }
            }

            class ReceivedMessageHolder extends RecyclerView.ViewHolder {
                TextView messageText, timeText, nameText;
                AvatarView profileImage;

                public ReceivedMessageHolder(View v) {
                    super(v);
                    messageText = itemView.findViewById(R.id.text_message_body);
                    timeText = itemView.findViewById(R.id.text_message_time);
                }
                void bind(Message message) {
                    messageText.setText(message.getText());
                    Date timeStamp = message.getTimestamp();
                    CharSequence formatedTime = DateUtils.formatSameDayTime(timeStamp.getTime(),
                            Calendar.getInstance().getTimeInMillis(),
                            DateFormat.FULL,DateFormat.SHORT);
                    if(!DateUtils.isToday(timeStamp.getTime())){
                        formatedTime = DateUtils.getRelativeDateTimeString(
                                PlayApplication.context,
                                message.getTimestamp().getTime(),
                                SECOND_IN_MILLIS,
                                WEEK_IN_MILLIS, 0);
                    }
                    timeText.setText(formatedTime);
                }
            }
        };

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                messageList.smoothScrollToPosition(adapter.getItemCount());
            }
        });
        messageList.setAdapter(adapter);

        mMessageEditText = findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }
            private Timer timer = new Timer();
            private final long DELAY = 2000;
            @Override
            public void afterTextChanged(Editable s) {
                if(!typingStarted) {
                    typingStarted = true;
                    if(PlayApplication.self !=null) {
                        PlayApplication.self.isTyping= typingStarted;
                        store.saveUser(PlayApplication.self);
                    }
                }
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                typingStarted = false;
                                if(PlayApplication.self !=null) {
                                    PlayApplication.self.isTyping= typingStarted;
                                    store.saveUser(PlayApplication.self);
                                }
                            }
                        },
                        DELAY
                );
            }
        });
        mMessageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event.getKeyCode()==KeyEvent.KEYCODE_ENTER) {
                    String txtMsg = mMessageEditText.getText().toString();
                    Message newMsg = new Message(txtMsg,
                            PlayApplication.self.username, "");
                    store.pushMessage(newMsg);
                    mMessageEditText.setText("");
                    Log.d(Tag, "Sent Message: " + newMsg.getText());
                    return false;
                }
                return false;
            }
        });

        mSendButton = findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txtMsg = mMessageEditText.getText().toString();
                Message newMsg = new Message(txtMsg,
                        PlayApplication.self.username, "");
                store.pushMessage(newMsg);
                mMessageEditText.setText("");
                Log.d(Tag, "Sent Message: " + newMsg.getText());
            }
        });
    }

    private int setProgress() {
        if (PlayApplication.musicService == null || mDragging) {
            return 0;
        }
        int position = PlayApplication.musicService.getPosition();
        int duration = PlayApplication.musicService.getDuration();
        if (seekBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                seekBar.setProgress( (int) pos);
            }
        }

        return position;
    }

    private void attachListener(){
        final DocumentReference docRef = firestore.collection("activities")
                .document(PlayApplication.self.id);
        listenerRegistration = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(Tag, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    RemoteAction doc =  snapshot.toObject(RemoteAction.class);
                    switch (doc.action) {
                        case "error":
                            Snackbar.make(messageList,doc.data,Snackbar.LENGTH_LONG).show();
                            store.setActionOwn(new RemoteAction("",""));
                            break;
                        case "downloading":
                            downloadProgress.setProgress(Integer.parseInt(doc.data));
                            downloadPercentage.setText(doc.data+"%");
                            store.setActionOwn(new RemoteAction("",""));
                            break;
                        case "download":
                            Log.w(Tag,"Download "+doc.data);
                            downloadName.setText(doc.data);
                            downloadPercentage.setText("0%");
                            downloadProgress.setProgress(0);

                            downloadView.setVisibility(View.VISIBLE);

                            processDownload(doc.data);
                            store.setActionOwn(new RemoteAction("",""));
                            break;
                        case "setup":
                            Log.w(Tag,"Setup "+doc.data);
                            downloadView.setVisibility(View.GONE);
                            currentSong = findSongInLibrary(doc.data);
                            if (currentSong != null && PlayApplication.musicService!=null) {
                                currentSongView.setVisibility(View.VISIBLE);
                                seekBar.setVisibility(View.VISIBLE);
                                if (currentSong.cover != null) {
                                    albumArt.setImageDrawable(Drawable.createFromPath(currentSong.cover));
                                }
                                artistName.setText(currentSong.artist);
                                songName.setText(currentSong.title);
                                songDuration.setText(stringForTime((int) currentSong.duration));

                                btnPlayPause.setVisibility(View.VISIBLE);
                                btnPlayPause.setImageResource(R.drawable.ic_play);
                                PlayApplication.musicService.setSong(currentSong);
                            }
                            store.setActionOwn(new RemoteAction("",""));
                            break;
                        case "play":
                            Log.w(Tag,"Play "+doc.data);
                            currentSong = findSongInLibrary(doc.data);
                            if (currentSong != null && PlayApplication.musicService!=null) {
                                if(PlayApplication.musicService.currentSong == null){
                                    PlayApplication.musicService.setSong(currentSong);
                                }
                                if (currentSong.cover != null) {
                                    albumArt.setImageDrawable(Drawable.createFromPath(currentSong.cover));
                                }
                                artistName.setText(currentSong.artist);
                                songName.setText(currentSong.title);
                                songDuration.setText(stringForTime((int) currentSong.duration));

                                currentSongView.setVisibility(View.VISIBLE);
                                btnPlayPause.setVisibility(View.VISIBLE);
                                btnPlayPause.setImageResource(R.drawable.ic_pause);
                                PlayApplication.musicService.playSong();
                                handler.post(mShowProgress);
                            }
                            store.setActionOwn(new RemoteAction("",""));
                            break;
                        case "pause":
                            Log.w(Tag,"Pause "+doc.data);
                            if (currentSong != null && PlayApplication.musicService!=null) {
                                    btnPlayPause.setImageResource(R.drawable.ic_play);
                                    PlayApplication.musicService.pausePlayer();
                                    handler.removeCallbacks(mShowProgress);
                            }
                            store.setActionOwn(new RemoteAction("",""));
                            break;
                        case "seek":
                            Log.w(Tag,"Seek "+doc.data);
                            if (currentSong != null && PlayApplication.musicService!=null) {
                                    PlayApplication.musicService.seekTo(Integer.parseInt(doc.data));
                            }
                            store.setActionOwn(new RemoteAction("",""));
                            break;
                        default:
                            break;
                    }
                    Log.d(Tag, "Current data: " + snapshot.getData());
                } else {
                    Log.d(Tag, "Current data: null");
                }
            }
        });
    }

    private final Runnable mShowProgress = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            if (!mDragging && PlayApplication.musicService.isPlaying()) {
                handler.postDelayed(mShowProgress, 1000 - (pos % 1000));
            }
        }
    };
    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            mDragging = true;
            handler.removeCallbacks(mShowProgress);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }

            long duration = PlayApplication.musicService.getDuration();
            newposition = (duration * progress) / 1000L;

            if (songDuration != null)
                songDuration.setText(stringForTime( (int) newposition));
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            store.setAction(new RemoteAction(SEEK,String.valueOf(newposition)));
            handler.post(mShowProgress);
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private void processDownload(final String filename){
        StorageReference musicRef = mStorage.child("music/"+filename);
        try {
            final File localFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), filename+".mp3");
            if(localFile.exists()){
                MediaScannerConnection.scanFile(
                        getApplicationContext(),
                        new String[]{localFile.getAbsolutePath()},
                        null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                PlayApplication.songs = UtilityFunctions.getSongs();
                                store.setAction(new RemoteAction(SETUP,filename));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        currentSongView.setVisibility(View.VISIBLE);
                                        seekBar.setVisibility(View.VISIBLE);
                                        uploaderIconView.setUploadingStatus(SongUploaderIconView.UploadingStatus.UPLOADED);
                                    }
                                });
                                Log.v(Tag,
                                        "file " + path + " was scanned seccessfully: " + uri);
                            }
                        });

                return;
            }
            final int nId = downloadNotification.notifyDownload("PlayTogether",filename);
            //startingDownload
            if(localFile.createNewFile()==true) {
                musicRef.getFile(localFile).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        store.setAction(new RemoteAction(DOWNLOADING,String.valueOf(100)));
                        Log.i(Tag, "Download: Success: " + localFile.getPath());

                        MediaScannerConnection.scanFile(
                                getApplicationContext(),
                                new String[]{localFile.getAbsolutePath()},
                                null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                        PlayApplication.songs = UtilityFunctions.getSongs();
                                        store.setAction(new RemoteAction(SETUP,filename));
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                // Stuff that updates the UI
                                                currentSongView.setVisibility(View.VISIBLE);
                                                seekBar.setVisibility(View.VISIBLE);
                                                uploaderIconView.setUploadingStatus(SongUploaderIconView.UploadingStatus.UPLOADED);
                                            }
                                        });
                                        downloadNotification.cancel(nId);
                                        Log.v(Tag,
                                                "file " + path + " was scanned seccessfully: " + uri);
                                    }
                                });



                    }
                }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                Log.i(Tag, String.format("Download is %f done", progress));
                                downloadNotification.notifyProgress(nId,(int) progress);
                                RemoteAction doc = new RemoteAction(DOWNLOADING,String.valueOf((int)progress));
                                store.setAction(doc);
//                                uploaderIconView.updateProgress(ChatActivity.this,(int)progress);
//                                uploaderIconView.setUploadingStatus(SongUploaderIconView.UploadingStatus.IN_PROGRESS);
                                //show download %
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Log.e("download", "Download:"+exception.toString());
                                downloadNotification.cancel(nId);
                                Snackbar.make(currentSongView,"Unable to download Song."+exception.toString(),
                                        Snackbar.LENGTH_LONG).show();
                                RemoteAction doc = new RemoteAction(ERROR,"Download Error: "+exception.toString());
                                store.setAction(doc);
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadSong(final Song song){
        btnPlayPause.setVisibility(View.GONE);
        seekBar.setVisibility(View.GONE);
        currentSongView.setVisibility(View.VISIBLE);
        final StorageReference musicRef = mStorage.child("music/"+song.title);
        musicRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //set local view
                downloadName.setText(song.title);
                downloadPercentage.setText("0%");
                downloadProgress.setProgress(0);
                downloadView.setVisibility(View.VISIBLE);
                //enable remote
                RemoteAction doc = new RemoteAction(DOWNLOAD,song.title);
                store.setAction(doc);
                uploaderIconView.setUploadingStatus(SongUploaderIconView.UploadingStatus.UPLOADED);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // File not found
                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType("audio/mpeg")
                        .build();

                Log.d(Tag, "uploading file: "+song.title);

                Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id);
                UploadTask uploadTask = musicRef.putFile(trackUri,metadata);
                uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress =  (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.i(Tag,String.format("Upload is %f done",progress));
                        uploaderIconView.updateProgress(ChatActivity.this,(int)progress);
                        uploaderIconView.setUploadingStatus(SongUploaderIconView.UploadingStatus.IN_PROGRESS);
                    }
                }).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return musicRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            //set local view
                            downloadName.setText(song.title);
                            downloadPercentage.setText("0%");
                            downloadProgress.setProgress(0);
                            downloadView.setVisibility(View.VISIBLE);
                            //enable remote
                            RemoteAction doc = new RemoteAction(DOWNLOAD,song.title);
                            store.setAction(doc);
                            uploaderIconView.setUploadingStatus(SongUploaderIconView.UploadingStatus.UPLOADED);
                        } else {
                            Log.i(Tag,"failed upload file: "+song.title+task.getException().getMessage());
                            uploaderIconView.setUploadingStatus(SongUploaderIconView.UploadingStatus.NOT_UPLOADED);
                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        if(registration!=null) {
            registration.remove();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                clearChat();
                return true;
            case R.id.action_music:
                Intent musicIntent = new Intent(this,PlaySongActivity.class);
                startActivityForResult(musicIntent,101);
                return true;
            case R.id.action_connect:
                if(listenerRegistration==null){
                    attachListener();
                    item.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_connected));
                }
                else{
                    listenerRegistration.remove();
                    listenerRegistration=null;
                    item.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_disconnected));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearChat(){
        store.clearChat();
    }
    @Override
    public void onPause() {
        if(PlayApplication.self!=null) {
            PlayApplication.self.state = "Offline";
            PlayApplication.self.last_changed = formater.format(Calendar.getInstance().getTime());
            store.saveUser(PlayApplication.self);
        }
        adapter.stopListening();
        PlayApplication.activityPaused();
        super.onPause();
    }
    @Override
    public void onResume() {
        if(PlayApplication.self!=null) {
            PlayApplication.self.state = "Online";
            PlayApplication.self.last_changed = formater.format(Calendar.getInstance().getTime());
            store.saveUser(PlayApplication.self);
        }
        PlayApplication.activityResumed();
        super.onResume();
        adapter.startListening();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {

            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                if (bundle.containsKey("song")) {
                    currentSong = bundle.getParcelable("song");
                    processSelectedSong();
                    Toast.makeText(getApplicationContext(), "Current Song "+currentSong.title,
                            Toast.LENGTH_SHORT).show();

                }
            }
        }
    }

    private Song findSongInLibrary(String title){
        Song newSong = new Song();
        newSong.title = title;
        int songIndex =PlayApplication.songs.indexOf(newSong);
        if(songIndex>-1){
            return PlayApplication.songs.get(songIndex);
        }
        return null;
    }

    private void processSelectedSong(){
        if(currentSong.cover!=null) {
            albumArt.setImageDrawable(Drawable.createFromPath(currentSong.cover));
        }
        artistName.setText(currentSong.artist);
        songName.setText(currentSong.title);
        songDuration.setText(stringForTime((int)currentSong.duration));

        uploadSong(currentSong);
    }



    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void start() {
        PlayApplication.musicService.unpausePlayer();
    }
    @Override
    public void pause() {
        PlayApplication.musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (PlayApplication.musicService != null && PlayApplication.musicService.musicBound
                && PlayApplication.musicService.isPlaying())
            return PlayApplication.musicService.getDuration();
        else
            return 0;
    }
    @Override
    public int getCurrentPosition() {
        if (PlayApplication.musicService != null && PlayApplication.musicService.musicBound
                && PlayApplication.musicService.isPlaying())
            return PlayApplication.musicService.getPosition();
        else
            return 0;
    }

    @Override
    public void seekTo(int position) {
        PlayApplication.musicService.seekTo(position);
    }

    @Override
    public boolean isPlaying() {
        if (PlayApplication.musicService != null && PlayApplication.musicService.musicBound)
            return PlayApplication.musicService.isPlaying();

        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
