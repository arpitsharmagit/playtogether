package adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Filter;

import com.ninja.playtogether.R;

import java.util.ArrayList;
import java.util.List;

import agency.tango.android.avatarview.IImageLoader;
import agency.tango.android.avatarview.views.AvatarView;
import agency.tango.android.avatarviewglide.GlideLoader;
import model.User;

public class UsersAdapter extends  RecyclerView.Adapter<UsersAdapter.UserViewHolder>
        implements Filterable {
    private IImageLoader imageLoader;
    private Context context;
    private List<User> users;
    private List<User> filterUsers;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User item);
    }

    public UsersAdapter(Context context,
                           ArrayList<User> contacts,
                           OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.users = contacts;
        filterUsers = contacts;
        imageLoader =new GlideLoader();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        AvatarView avatarView;
        TextView mUsernameView,mUserStatus;
        LinearLayout userStatusView;

        public UserViewHolder(View view) {
            super(view);
            userStatusView = view.findViewById(R.id.user_status_view);
            mUsernameView =  view.findViewById(R.id.usernameView);
            avatarView =  view.findViewById(R.id.profilepic);
        }
        public void bind(final User user, final OnItemClickListener listener) {
            mUsernameView.setText(user.username);
            if(user.profilePictureUrl!=null && !user.profilePictureUrl.isEmpty()) {
                imageLoader.loadImage(avatarView, user.profilePictureUrl, user.username);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClick(user);
                }
            });
        }
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item_layout, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder,final int position) {
//        final User item = filterUsers.get(position);
//        holder.mUsernameView.setText(item.name);
//        holder.phone.setText(item.phoneNumber);

        holder.bind(filterUsers.get(position), listener);
    }
    @Override
    public int getItemCount() {
        return filterUsers.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    filterUsers = users;
                } else {
                    ArrayList<User> filteredList = new ArrayList<>();
                    for (User user : users) {
                        if (user.username.toLowerCase().contains(charString)) {
                            filteredList.add(user);
                        }
                    }
                    filterUsers = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filterUsers;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filterUsers = (ArrayList<User>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}

