package com.mandy.adrianbrowser.model;
import androidx.room.*;

@Entity(tableName = "history")
public class HistoryItem {
    @PrimaryKey(autoGenerate = true) public int id;
    public String title;
    public String url;
    public long timestamp;
    public HistoryItem(String title, String url) {
        this.title = title; this.url = url;
        this.timestamp = System.currentTimeMillis();
    }
}
