package model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RemoteAction {
    public String action; //play,download,pause,seek
    public String data;//title,seektime
    public RemoteAction(){}
    public RemoteAction(String action, String data){
        this.action=action;
        this.data=data;
    }
}
