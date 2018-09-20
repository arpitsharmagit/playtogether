package datastore;

import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import durdinapps.rxfirebase2.RxFirestore;
import extras.PlayApplication;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import model.Contact;
import model.Message;
import model.RemoteAction;
import model.User;

public class Store {
    final static String Tag = "FireStore";
    FirebaseFirestore firestore;
    List<User> users;
    Disposable usersDisposable;

    public Store(){
        firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        users = new ArrayList<>();
        CollectionReference usersRef = firestore.collection("users");
        usersDisposable = RxFirestore.getCollection(usersRef,User.class)
                .subscribe(new Consumer<List<User>>() {
                    @Override
                    public void accept(List<User> usersCol) throws Exception {
                        users = usersCol;
                        PlayApplication.users = users;
                        Log.i(Tag,"users");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(Tag,throwable.getMessage());
                    }
                });
    }
    @Override
    public void finalize() {
        usersDisposable.dispose();
    }

    public List<User> getUsers(){
        return this.users;
    }
    public void saveContact(final Contact user){
        DocumentReference document = firestore.collection("users")
                .document(PlayApplication.self.id)
                .collection("contacts")
                .document(user.name+" - "+user.phoneNumber);
        RxFirestore.setDocument(document, user).subscribe(new Action() {
            @Override
            public void run() throws Exception {
                Log.i(Tag,user.phoneNumber+" Saved");
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e(Tag,throwable.getMessage());
            }
        });
    }
    public void saveUser(final User user){
        DocumentReference document = firestore.collection("users").document(user.id);
        RxFirestore.setDocument(document, user).subscribe(new Action() {
            @Override
            public void run() throws Exception {
                Log.i(Tag,user.phone+" Saved");
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e(Tag,throwable.getMessage());
            }
        });
    }
    public User getUser(final String userId){
        for(User user:users){
            if(user.id.equals(userId)){
                return user;
            }
        }
        return null;
    }

    public void setNotificationToken(String token){
        if(PlayApplication.self!=null){
            PlayApplication.self.notificationToken = token;
            saveUser(PlayApplication.self);
        }
    }

    public void setAction(final RemoteAction doc){
        if(PlayApplication.self!=null && !doc.action.equals("download")){
            DocumentReference document = firestore.collection("activities").document(PlayApplication.self.id);
            RxFirestore.setDocument(document, doc).subscribe(new Action() {
                @Override
                public void run() throws Exception {
                    Log.i(Tag,doc.action+" self Saved");
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    Log.e(Tag,throwable.getMessage());
                }
            });
        }
        if(PlayApplication.other!=null){
            DocumentReference document = firestore.collection("activities").document(PlayApplication.other.id);
            RxFirestore.setDocument(document, doc).subscribe(new Action() {
                @Override
                public void run() throws Exception {
                    Log.i(Tag,doc.action+" other Saved");
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    Log.e(Tag,throwable.getMessage());
                }
            });
        }
    }
    public void setActionOwn(final RemoteAction doc) {

        DocumentReference document = firestore.collection("activities").document(PlayApplication.self.id);
        RxFirestore.setDocument(document, doc).subscribe(new Action() {
            @Override
            public void run() throws Exception {
                Log.i(Tag, doc.action + " self Saved");
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e(Tag, throwable.getMessage());
            }
        });
    }

    public void pushMessage(Message message){
        DocumentReference ownMessage =firestore.collection("users")
                .document(PlayApplication.self.id)
                .collection("conversations")
                .document(PlayApplication.other.id)
                .collection("messages").document(message.getId());
        DocumentReference otherMessage =firestore.collection("users")
                .document(PlayApplication.other.id)
                .collection("conversations")
                .document(PlayApplication.self.id)
                .collection("messages").document(message.getId());
        RxFirestore.setDocument(ownMessage, message).subscribe();
        RxFirestore.setDocument(otherMessage, message).subscribe();
    }
    public void clearChat() {
        firestore.collection("users/" + PlayApplication.other.id + "/conversations/"
                + PlayApplication.self.id + "/messages")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        WriteBatch batch = firestore.batch();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(PlayApplication.context, "Chat cleared for " +
                                        PlayApplication.other.username, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
        firestore.collection("users/" + PlayApplication.self.id + "/conversations/"
                + PlayApplication.other.id + "/messages")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        WriteBatch batch = firestore.batch();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(PlayApplication.context, "Chat cleared for " +
                                        PlayApplication.self.username, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }
}
