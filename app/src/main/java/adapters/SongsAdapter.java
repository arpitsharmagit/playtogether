package adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import com.ninja.playtogether.R;

import java.util.ArrayList;
import java.util.List;

import model.Song;
import views.SongUploaderIconView;


public class SongsAdapter extends  RecyclerView.Adapter<SongsAdapter.SongsViewHolder>
        implements Filterable {
    private Context context;
    private List<Song> songs;
    private List<Song> filterSongs;
    private OnItemClickListener listener,onLongPressListener;

    public interface OnItemClickListener {
        void onItemClick(Song item);
    }

    public SongsAdapter(Context context,
                           ArrayList<Song> songs,
                           OnItemClickListener listener,OnItemClickListener onLongPressListener) {
        this.context = context;
        this.listener = listener;
        this.onLongPressListener =onLongPressListener;
        this.songs = songs;
        filterSongs = songs;
    }

    public void setSongs(ArrayList<Song> songs){
        this.songs = songs;
        filterSongs = songs;
        notifyDataSetChanged();
    }

    public class SongsViewHolder extends RecyclerView.ViewHolder
    {
        public TextView title,artist,duration;
        private SongUploaderIconView uploaderIconView;
        public ImageView background;
        public SongsViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.song_name);
            artist = view.findViewById(R.id.artist_name);
            background = view.findViewById(R.id.song_background);
            duration = view.findViewById(R.id.song_duration);
            uploaderIconView = itemView.findViewById(R.id.icon_song_upload);
            uploaderIconView.init();
            uploaderIconView.setOnClickListener(null);
            uploaderIconView.setVisibility(View.GONE);
        }
        public void bind(final Song item, final OnItemClickListener listener,
                         final  OnItemClickListener onItemClickListener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClick(item);
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    onItemClickListener.onItemClick(item);
                    return false;
                }
            });
        }
    }

    @Override
    public SongsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_item_layout, parent, false);
        return new SongsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SongsViewHolder holder, final int position) {
        final Song item = filterSongs.get(position);
        holder.title.setText(item.title);
        holder.title.setSelected(true);
        holder.artist.setText(item.artist);
        holder.duration.setText(convertDuration(item.duration));
        if(item.cover != null && !item.cover.isEmpty()){
            holder.background.setImageDrawable(Drawable.createFromPath(item.cover));
        }

        holder.bind(filterSongs.get(position), listener,onLongPressListener);
    }
    @Override
    public int getItemCount() {
        return filterSongs.size();
    }
    private String convertDuration(long duration) {
        String out = null;
        long hours=0;
        try {
            hours = (duration / 3600000);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return out;
        }
        long remaining_minutes = (duration - (hours * 3600000)) / 60000;
        String minutes = String.valueOf(remaining_minutes);
        if (minutes.equals(0)) {
            minutes = "00";
        }
        long remaining_seconds = (duration - (hours * 3600000) - (remaining_minutes * 60000));
        String seconds = String.valueOf(remaining_seconds);
        if (seconds.length() < 2) {
            seconds = "00";
        } else {
            seconds = seconds.substring(0, 2);
        }

        if (hours > 0) {
            out = hours + ":" + minutes + ":" + seconds;
        } else {
            out = minutes + ":" + seconds;
        }

        return out;

    }
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    filterSongs = songs;
                } else {
                    ArrayList<Song> filteredList = new ArrayList<>();
                    CharSequence searchString = charString.toLowerCase();
                    for (Song song : songs) {
                        if(song.name!=null && song.name.toLowerCase().contains(searchString)){
                            filteredList.add(song);
                            continue;
                        }
                        if(song.artist !=null && song.artist.toLowerCase().contains(searchString)){
                            filteredList.add(song);
                            continue;
                        }
                        if(song.album!=null && song.album.toLowerCase().contains(searchString)){
                            filteredList.add(song);
                            continue;
                        }
                        if(song.title !=null && song.title.toLowerCase().contains(searchString)){
                            filteredList.add(song);
                            continue;
                        }
                    }
                    filterSongs = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filterSongs;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filterSongs = (ArrayList<Song>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}
