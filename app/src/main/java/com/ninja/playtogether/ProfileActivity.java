package com.ninja.playtogether;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.soundcloud.android.crop.Crop;

import java.io.ByteArrayOutputStream;
import java.io.File;

import agency.tango.android.avatarview.IImageLoader;
import agency.tango.android.avatarview.views.AvatarView;
import agency.tango.android.avatarviewglide.GlideLoader;
import datastore.Store;
import extras.PlayApplication;
import model.Message;

public class ProfileActivity extends AppCompatActivity {

    final int request_code = 1;

    private StorageReference mStorage;
    private Store store;

    Dialog progressDialog;
    AvatarView profilePicView;
    EditText editUsername;
    Button btnChange;
    boolean isNewPic = false;

    private IImageLoader imageLoader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imageLoader =new GlideLoader();

        mStorage =  FirebaseStorage.getInstance().getReference();
        store = new Store();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        progressDialog =new Dialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setContentView(R.layout.progress_layout);

        profilePicView = findViewById(R.id.profile_image);
        profilePicView.setDrawingCacheEnabled(true);
        profilePicView.buildDrawingCache();
        editUsername = findViewById(R.id.editUsername);
        btnChange = findViewById(R.id.btnChange);

        editUsername.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        if(PlayApplication.self!=null && PlayApplication.self.profilePictureUrl !=null){
            imageLoader.loadImage(profilePicView, PlayApplication.self.profilePictureUrl, PlayApplication.self.username);
        }

        profilePicView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Crop.pickImage(ProfileActivity.this);
//                Intent i=new Intent(Intent.ACTION_PICK);
//                i.setType("image/*");
//                startActivityForResult(i,request_code);
            }
        });

        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Upload Image and Set Text

                if(isNewPic) {
                    progressDialog.show();
                    Drawable bitmapDrawable =profilePicView.getDrawable();
                    Bitmap bitmap = ((BitmapDrawable)profilePicView.getDrawable().getCurrent()).getBitmap();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data = baos.toByteArray();

                    final StorageReference pictureProfilePath = mStorage.child("profile_images/" + PlayApplication.self.id + ".jpg");

                    UploadTask uploadTask = pictureProfilePath.putBytes(data);
                    uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return pictureProfilePath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            progressDialog.dismiss();
                            isNewPic =false;
                            if (task.isSuccessful()) {
                                if (editUsername.getText().toString()!=null && editUsername.getText().toString().length()>0) {
                                    PlayApplication.self.username = editUsername.getText().toString();
                                }
                                Uri downloadUri = task.getResult();
                                PlayApplication.self.profilePictureUrl = downloadUri.toString();
                                store.saveUser(PlayApplication.self);
                                Toast.makeText(ProfileActivity.this, "Profile uploaded successfully.", Toast.LENGTH_SHORT)
                                        .show();
                                finish();
                            } else {
                                Toast.makeText(ProfileActivity.this,
                                        "Error in uploading this image.\nPlease try again.", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    });
                }
                else if (editUsername.getText().toString()!=null && editUsername.getText().toString().length()>0){
                    PlayApplication.self.username = editUsername.getText().toString();
                    store.saveUser(PlayApplication.self);
                    editUsername.setText("");
                    Toast.makeText(ProfileActivity.this, "Username updated successfully.", Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
        super.onActivityResult(requestCode, resultCode, result);
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
        Crop.of(source, destination).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            profilePicView.setImageURI(Crop.getOutput(result));
            isNewPic = true;
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
