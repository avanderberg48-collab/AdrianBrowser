package com.mandy.adrianbrowser.db;
import androidx.room.*;
import com.mandy.adrianbrowser.model.HistoryItem;
import java.util.List;

@Dao
public interface HistoryDao {
    @Insert public void insert(HistoryItem h);
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 500") List<HistoryItem> getAll();
    @Query("SELECT * FROM history WHERE title LIKE :q OR url LIKE :q ORDER BY timestamp DESC") List<HistoryItem> search(String q);
    @Query("DELETE FROM history") void clearAll();
    @Query("DELETE FROM history WHERE id = :id") void deleteById(int id);
}
