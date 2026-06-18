package com.mandy.adrianbrowser.db;
import android.content.Context;
import androidx.room.*;
import com.mandy.adrianbrowser.model.*;

@Database(entities = {Bookmark.class, HistoryItem.class, DownloadItem.class}, version = 1)
public abstract class BrowserDatabase extends RoomDatabase {
    private static BrowserDatabase INSTANCE;
    public abstract BookmarkDao bookmarkDao();
    public abstract HistoryDao historyDao();
    public abstract DownloadDao downloadDao();
    public static BrowserDatabase get(Context ctx) {
        if (INSTANCE == null)
            INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(), BrowserDatabase.class, "browser_db")
                    .allowMainThreadQueries().build();
        return INSTANCE;
    }
}
