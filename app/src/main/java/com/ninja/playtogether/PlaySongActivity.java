package com.ninja.playtogether;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.common.util.ArrayUtils;

import java.util.ArrayList;

import adapters.SongsAdapter;
import extras.PlayApplication;
import extras.UtilityFunctions;
import model.Song;

public class PlaySongActivity extends AppCompatActivity {
    private SearchView searchView;
    private RecyclerView songsView;
    private SongsAdapter adapter;
    private ProgressBar progressBar;
    private static ArrayList<Song> songs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_song);
        songs = PlayApplication.songs;

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        progressBar = findViewById(R.id.progressBar);
        songsView = findViewById(R.id.songs_list);
        adapter = new SongsAdapter(this, songs, new SongsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(final Song item) {
                new AlertDialog.Builder(PlaySongActivity.this, R.style.AlertDialogTheme)
                        .setTitle("Transfer Song")
                        .setMessage(item.title)
                        .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent();
                                intent.putExtra("song", item);
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            }})
                        .setNegativeButton("Cancel", null).show();

            }
        }, new SongsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Song item) {
                Intent intent = new Intent(PlaySongActivity.this,SongDetailActivity.class);
                intent.putExtra("song", item);
                startActivity(intent);
            }
        });
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        songsView.setLayoutManager(mLayoutManager);
        songsView.setItemAnimator(new DefaultItemAnimator());
        songsView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        songsView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }
        if (id == R.id.action_refresh) {
            progressBar.setVisibility(View.VISIBLE);
            PlayApplication.songs = UtilityFunctions.getSongs();
            adapter.setSongs(PlayApplication.songs);
            progressBar.setVisibility(View.GONE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        SearchManager searchManager = (SearchManager) getApplicationContext().getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                adapter.getFilter().filter(query);
                return false;
            }
        });


        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
