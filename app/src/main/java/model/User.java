package model;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

@IgnoreExtraProperties
public class User {

    public String id;
    public String username;
    public String phone;
    public String notificationToken;
    public String last_changed;
    public String state;
    public String profilePictureUrl;
    public boolean isTyping;

    public User() {
    }

    public User(String id, String username, String phone) {
        this.id = id;
        this.username = username;
        this.phone = phone;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Contact){
            Contact p = (Contact) o;
            return this.phone.contains(p.phoneNumber);
        } else if(o instanceof User){
            User p = (User) o;
            return this.phone.contains(p.phone);
        }
        else
            return false;
    }
}
