package com.ninja.playtogether;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import agency.tango.android.avatarview.IImageLoader;
import agency.tango.android.avatarview.views.AvatarView;
import agency.tango.android.avatarviewglide.GlideLoader;
import extras.PlayApplication;

public class ShowPictureActivity extends AppCompatActivity {

    private Toolbar mTopToolbar;
    private IImageLoader imageLoader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);

        mTopToolbar =  findViewById(R.id.toolbar);
        mTopToolbar.setTitle(PlayApplication.other.username);
        setSupportActionBar(mTopToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if(PlayApplication.other!=null){
            imageLoader =new GlideLoader();
            AvatarView profileOther = findViewById(R.id.profile_pic);
            imageLoader.loadImage(profileOther, PlayApplication.other.profilePictureUrl, PlayApplication.other.username);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
