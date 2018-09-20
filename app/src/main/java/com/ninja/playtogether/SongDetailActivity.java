package com.ninja.playtogether;

import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Formatter;
import java.util.Locale;

import extras.PlayApplication;
import model.Song;

public class SongDetailActivity extends AppCompatActivity {

    private Song currentSong;

    private Toolbar toolbar;
    private ImageView albumArt;
    private TextView songTitle,songArtist,songAlbum,songDuration,songPath,title;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_detail);

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        title = findViewById(R.id.title);
        toolbar = findViewById(R.id.toolbar);
        albumArt = findViewById(R.id.album_art);
        songAlbum = findViewById(R.id.song_album);
        songArtist = findViewById(R.id.song_artist);
        songTitle = findViewById(R.id.song_title);
        songDuration = findViewById(R.id.song_duration);
        songPath = findViewById(R.id.song_path);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        Bundle bundle = getIntent().getExtras();

        if (bundle != null && bundle.containsKey("song")) {
            currentSong = bundle.getParcelable("song");
            if (currentSong != null) {
                bindControls();
            }
        }
    }
    private void bindControls() {
        title.setText("Song Details - " + currentSong.name);
        songPath.setText(currentSong.path);
        songDuration.setText(stringForTime(currentSong.duration));
        songTitle.setText(currentSong.title);
        songArtist.setText(currentSong.artist);
        songAlbum.setText(currentSong.album);
        if(currentSong.cover != null && !currentSong.cover.isEmpty()){
            albumArt.setImageDrawable(Drawable.createFromPath(currentSong.cover));
        }
    }
    private String stringForTime(long timeMs) {
        long totalSeconds = timeMs / 1000;

        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
