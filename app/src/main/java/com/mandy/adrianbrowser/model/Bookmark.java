package com.mandy.adrianbrowser.model;
import androidx.room.*;

@Entity(tableName = "bookmarks")
public class Bookmark {
    @PrimaryKey(autoGenerate = true) public int id;
    public String title;
    public String url;
    public long timestamp;
    public String folder;
    public Bookmark(String title, String url) {
        this.title = title; this.url = url;
        this.timestamp = System.currentTimeMillis();
        this.folder = "General";
    }
}
