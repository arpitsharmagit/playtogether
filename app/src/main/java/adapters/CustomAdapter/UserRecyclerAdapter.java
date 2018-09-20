package adapters.CustomAdapter;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.common.ChangeEventType;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.ObservableSnapshotArray;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.ninja.playtogether.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import agency.tango.android.avatarview.IImageLoader;
import agency.tango.android.avatarview.views.AvatarView;
import agency.tango.android.avatarviewglide.GlideLoader;
import extras.PlayApplication;
import model.User;

public class UserRecyclerAdapter
        extends RecyclerView.Adapter<UserRecyclerAdapter.UserViewHolder> implements FirebaseAdapter<User>,
        Filterable {
    private static final String TAG = "UserRecyclerAdapter";

    private RecycleItemClick recycleItemClick;
    private final ObservableSnapshotArray<User> mSnapshots;
    private final List<User> list, backupList;
    private CustomFilter mCustomFilter;
    private boolean isFiltarable;
    private IImageLoader imageLoader;
    FirebaseAuth mAuth;

    private int currentUserIndex = -1;

    public UserRecyclerAdapter(FirestoreRecyclerOptions<User> options,RecycleItemClick recycleItemClick, boolean isFiltarable) {
        mSnapshots = options.getSnapshots();
        this.recycleItemClick = recycleItemClick;
        list = new ArrayList<>();
        backupList = new ArrayList<>();
        if (options.getOwner() != null) {
            options.getOwner().getLifecycle().addObserver(this);
        }
        this.isFiltarable = isFiltarable;
        imageLoader =new GlideLoader();
        mAuth = FirebaseAuth.getInstance();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        if (!mSnapshots.isListening(this)) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mSnapshots.removeChangeEventListener(this);
        notifyDataSetChanged();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void cleanup(LifecycleOwner source) {
        source.getLifecycle().removeObserver(this);
    }

    @Override
    public void onChildChanged(ChangeEventType type,
                               DocumentSnapshot snapshot,
                               int newIndex,
                               int oldIndex) {
        User model = mSnapshots.get(newIndex);
        if(!list.contains(model) && !mAuth.getCurrentUser().getPhoneNumber().equals(model.phone)){
            if(newIndex >currentUserIndex){
                newIndex--;
                oldIndex--;
            }
            onChildUpdate(model, type, snapshot, newIndex, oldIndex);
        }
        else{
            currentUserIndex = newIndex;
        }
    }

    protected void onChildUpdate(User model, ChangeEventType type,
                                 DocumentSnapshot snapshot,
                                 int newIndex,
                                 int oldIndex) {
        switch (type) {
            case ADDED:
                    addItem(snapshot.getId(), model);
                    notifyItemInserted(newIndex);
                break;
            case CHANGED:
                    addItem(snapshot.getId(), model, newIndex);
                    notifyItemChanged(newIndex);
                break;
            case REMOVED:
                removeItem(newIndex);
                notifyItemRemoved(newIndex);
                break;
            case MOVED:
                moveItem(snapshot.getId(), model, newIndex, oldIndex);
                notifyItemMoved(oldIndex, newIndex);
                break;
            default:
                throw new IllegalStateException("Incomplete case statement");
        }
    }

    private void moveItem(String key, User t, int newIndex, int oldIndex) {
        list.remove(oldIndex);
        list.add(newIndex, t);
        if (isFiltarable) {
            backupList.remove(oldIndex);
            backupList.add(newIndex, t);
        }
    }

    private void removeItem(int newIndex) {
        list.remove(newIndex);
        if (isFiltarable)
            backupList.remove(newIndex);
    }

    private void addItem(String key, User t, int newIndex) {
        list.remove(newIndex);
        list.add(newIndex, t);
        if (isFiltarable) {
            backupList.remove(newIndex);
            backupList.add(newIndex, t);
        }
    }

    private void addItem(String id, User t) {
        list.add(t);
        if (isFiltarable)
            backupList.add(t);
    }

    @Override
    public void onDataChanged() {
    }

    @Override
    public void onError(@NonNull FirebaseFirestoreException e) {
        Log.w(TAG, e.toString());
    }


    @Override
    public ObservableSnapshotArray<User> getSnapshots() {
        return mSnapshots;
    }

    @Override
    public User getItem(int position) {
        return list.get(position);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item_layout, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onBindViewHolder(UserRecyclerAdapter.UserViewHolder holder, int position) {
        onBindViewHolder(holder, position, getItem(position));
    }

    protected void onBindViewHolder(UserRecyclerAdapter.UserViewHolder holder, int position, User model){
        holder.bind(model);
    }

    protected boolean filterCondition(User model, String filterPattern) {
        return true;
    }

    @Override
    public Filter getFilter() {
        if (mCustomFilter == null) {
            mCustomFilter = new CustomFilter();
        }
        return mCustomFilter;
    }

    public class CustomFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults results = new FilterResults();
            if (constraint.length() == 0) {
                results.values = backupList;
                results.count = backupList.size();
            } else {
                List<User> filteredList = new ArrayList<>();
                final String filterPattern = constraint.toString().toLowerCase().trim();
                for (User t : backupList) {
                    if (filterCondition(t, filterPattern)) {
                        filteredList.add(t);
                    }
                }
                results.values = filteredList;
                results.count = filteredList.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            list.clear();
            list.addAll((Collection<? extends User>) results.values);
            notifyDataSetChanged();
        }
    }
    public class UserViewHolder extends RecyclerView.ViewHolder {

        AvatarView avatarView;
        TextView mUsernameView,mUserStatus;
        LinearLayout userStatusView;

        UserViewHolder(View view) {
            super(view);
            userStatusView = view.findViewById(R.id.user_status_view);
            mUsernameView =  view.findViewById(R.id.usernameView);
            avatarView =  view.findViewById(R.id.profilepic);
//            mUserStatus = view.findViewById(R.id.user_status);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mUsernameView.getText() + "'";
        }

        public void bind(model.User user) {
            mUsernameView.setText(user.username);

//            if(user.state.equals("Online")){
//                userStatusView.setBackgroundResource(R.drawable.border_green);
//                mUserStatus.setText(user.state);
//                Log.w("Bind",user.username+" Online");
//            }
//            else{
//                userStatusView.setBackgroundResource(R.drawable.border_red);
//                mUserStatus.setText(user.last_changed);
//                Log.w("Bind",user.username+" last online "+ user.last_changed);
//            }
            if(user.profilePictureUrl!=null && !user.profilePictureUrl.isEmpty()) {
                imageLoader.loadImage(avatarView, user.profilePictureUrl, user.username);
            }
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    model.User user = getItem(pos);
                    recycleItemClick.onItemClick(user.id, user, pos);
                }
            });
        }
    }
    public interface RecycleItemClick {
        void onItemClick(String userId, model.User user, int position);
    }
}
