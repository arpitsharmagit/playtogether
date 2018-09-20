package adapters.CustomAdapter;

import android.arch.lifecycle.LifecycleObserver;
import android.support.annotation.RestrictTo;

import com.firebase.ui.firestore.ChangeEventListener;
import com.firebase.ui.firestore.FirestoreArray;
import com.firebase.ui.firestore.ObservableSnapshotArray;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FirebaseAdapter<T> extends ChangeEventListener, LifecycleObserver {
    /**
     * If you need to do some setup before the adapter starts listening for change events in the
     * database, do so it here and then call {@code super.startListening()}.
     */
    void startListening();

    /**
     * Removes listeners and clears all items in the backing {@link FirestoreArray}.
     */
    void stopListening();

    ObservableSnapshotArray<T> getSnapshots();

    T getItem(int position);

}
