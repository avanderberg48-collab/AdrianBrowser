package com.mandy.adrianbrowser.db;
import androidx.room.*;
import com.mandy.adrianbrowser.model.Bookmark;
import java.util.List;

@Dao
public interface BookmarkDao {
    @Insert public void insert(Bookmark b);
    @Delete public void delete(Bookmark b);
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC") List<Bookmark> getAll();
    @Query("SELECT * FROM bookmarks WHERE title LIKE :q OR url LIKE :q ORDER BY timestamp DESC") List<Bookmark> search(String q);
    @Query("DELETE FROM bookmarks WHERE id = :id") void deleteById(int id);
    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1") Bookmark findByUrl(String url);
}
