package model;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    public long id;
    public String title;
    public String artist;
    public String cover;
    public String path;
    public String name;
    public long duration;
    public String album;
    public int dateAdded;

    public Song(){

    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Song){
            Song p = (Song) o;
            return this.title.equals(p.title);
        } else
            return false;
    }
    public Song(Parcel in){
        String[] data = new String[6];
        long[] longData =new long[2];
        int [] intData = new int [1];

        in.readStringArray(data);
        this.title = data[0];
        this.artist = data[1];
        this.cover = data[2];
        this.path = data[3];
        this.name = data[4];
        this.album = data[5];


        in.readLongArray(longData);
        this.id = longData[0];
        this.duration = longData[1];

        in.readIntArray(intData);
        this.dateAdded = intData[0];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {
                this.title,
                this.artist,
                this.cover,
                this.path,
                this.name,
                this.album});
        dest.writeLongArray(new long[]{
                this.id,
                this.duration
        });
        dest.writeIntArray(new int[]{
                this.dateAdded
        });
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
