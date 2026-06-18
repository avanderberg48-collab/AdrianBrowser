package com.mandy.adrianbrowser.model;
import androidx.room.*;

@Entity(tableName = "downloads")
public class DownloadItem {
    @PrimaryKey(autoGenerate = true) public int id;
    public String fileName;
    public String url;
    public String localPath;
    public long fileSize;
    public long timestamp;
    public String status; // "completed", "failed", "downloading"
    public DownloadItem(String fileName, String url) {
        this.fileName = fileName; this.url = url;
        this.timestamp = System.currentTimeMillis();
        this.status = "downloading";
    }
}
