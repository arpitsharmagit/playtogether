package model;

import android.util.Log;

public class Contact {
    public String name;
    public String phoneNumber;

    @Override
    public boolean equals(Object o){
        if(o instanceof Contact){
            Contact p = (Contact) o;
            return this.phoneNumber.contains(p.phoneNumber);
        }
        else
            return false;
    }
}
