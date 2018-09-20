package model;

import android.text.format.DateUtils;

import com.google.firebase.firestore.ServerTimestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import extras.PlayApplication;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static android.text.format.DateUtils.FORMAT_ABBREV_ALL;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

public class Message {

    private String id;
    private String text;
    private String name;
    private Date timestamp;

    public Message() {
    }

    public Message(String text, String name, String photoUrl) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.name = name;
        this.timestamp = Calendar.getInstance().getTime();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}

