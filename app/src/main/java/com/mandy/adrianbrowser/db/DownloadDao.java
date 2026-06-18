package com.mandy.adrianbrowser.db;
import androidx.room.*;
import com.mandy.adrianbrowser.model.DownloadItem;
import java.util.List;

@Dao
public interface DownloadDao {
    @Insert public void insert(DownloadItem d);
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC") List<DownloadItem> getAll();
    @Query("DELETE FROM downloads WHERE id = :id") void deleteById(int id);
    @Query("DELETE FROM downloads") void clearAll();
}
