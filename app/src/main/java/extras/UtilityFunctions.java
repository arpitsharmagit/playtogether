package extras;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import datastore.Store;
import model.Contact;
import model.Song;
import model.User;

public class UtilityFunctions {

    public static ArrayList<Song> getSongs(){
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

        ContentResolver resolver = PlayApplication.context.getContentResolver();
        String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3");
        String[] selectionArgsMp3 = new String[]{ mimeType };
        final String musicsOnly = MediaStore.Audio.Media.IS_MUSIC + "=1";

        ArrayList<Song> list =new ArrayList<Song>();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED
        };
        String[] albumProjection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM_ART,
                MediaStore.Audio.Albums.ALBUM
        };

        //Find ExternalContext
        Cursor musicCursor = resolver.query(musicUri, projection, selectionMimeType,selectionArgsMp3, null);
        if(musicCursor!=null && musicCursor.moveToFirst()){

            do {

                Song song =new Song();
                song.id = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                song.artist = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                song.name = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                song.title = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                song.path = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                song.duration = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                song.dateAdded = musicCursor.getInt(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED));
                song.cover = "";
                //Get AlbumCover

                Long albumId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                Cursor cursorAlbum = resolver.query(albumUri, albumProjection,MediaStore.Audio.Albums._ID + "=" + albumId, null, null);
                if(cursorAlbum != null && cursorAlbum.moveToFirst()){
                    song.cover = cursorAlbum.getString(cursorAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                    song.album = cursorAlbum.getString(cursorAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                }

                list.add(song);
            }
            while (musicCursor.moveToNext());
        }
        Collections.sort(list, new Comparator<Song>() {
            public int compare(Song a, Song b)
            {
                return a.dateAdded -b.dateAdded;
            }
        });
        Collections.reverse(list);

        return  list;
    }

    public static ArrayList<Contact> getContactList() {
        Store mystore = new Store();
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        ContentResolver cr = PlayApplication.context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));

                        //pick only 8 digit
                        phoneNo =  phoneNo.replaceAll("\\s+","");

                        int phoneLength = phoneNo.length();
                        if(phoneLength<10){
                            continue;
                        }
                        if(phoneLength>10){
//                            Log.i("Utility",phoneNo);
                            phoneNo = phoneNo.substring(phoneLength-10);
                            StringBuilder builder = new StringBuilder();
                            builder.append(name).append(" (").append(phoneNo.length())
                                    .append(") ").append(phoneNo);

//                            Log.i("Utility",builder.toString());
                        }

                        Contact newContact = new Contact();
                        newContact.name = name;
                        newContact.phoneNumber =phoneNo;
                        if(!contacts.contains(newContact))
//                            mystore.saveContact(newContact);
                            contacts.add(newContact);
                    }
                    pCur.close();
                }
            }
        }
        if(cur!=null){
            cur.close();
        }
        return contacts;
    }

    public static boolean isSystemContact(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] projection = new String[]{
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.LOOKUP_KEY,
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.NUMBER};
        Cursor   cursor = PlayApplication.context.getContentResolver().query(uri, projection, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return false;
    }
}
