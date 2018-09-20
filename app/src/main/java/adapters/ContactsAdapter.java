package adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.ninja.playtogether.R;

import java.util.ArrayList;
import java.util.List;

import model.Contact;


public class ContactsAdapter extends  RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>
        implements Filterable {
    private Context context;
    private List<Contact> contacts;
    private List<Contact> filterContacts;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Contact item);
    }

    public ContactsAdapter(Context context,
                           ArrayList<Contact> contacts,
                           OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.contacts = contacts;
        filterContacts = contacts;
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {
        public TextView name,phone;

        public ContactViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.contact_name);
            phone = view.findViewById(R.id.contact_phone);
        }
        public void bind(final Contact item, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClick(item);
                }
            });
        }
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_item_layout, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder,final int position) {
        final Contact item = filterContacts.get(position);
        holder.name.setText(item.name);
        holder.phone.setText(item.phoneNumber);

        holder.bind(filterContacts.get(position), listener);
    }
    @Override
    public int getItemCount() {
        return filterContacts.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    filterContacts = contacts;
                } else {
                    ArrayList<Contact> filteredList = new ArrayList<>();
                    for (Contact user : contacts) {
                        if (user.name.toLowerCase().contains(charString)) {
                            filteredList.add(user);
                        }
                    }
                    filterContacts = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filterContacts;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filterContacts = (ArrayList<Contact>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}
