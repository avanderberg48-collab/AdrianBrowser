package com.mandy.adrianbrowser.ui;

import android.content.Intent;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.mandy.adrianbrowser.R;
import com.mandy.adrianbrowser.db.BrowserDatabase;
import com.mandy.adrianbrowser.model.Bookmark;
import java.util.*;

public class BookmarksActivity extends AppCompatActivity {
    private RecyclerView rv;
    private List<Bookmark> list = new ArrayList<>();
    private BrowserDatabase db;
    private EditText etSearch;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_list);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Bookmarks");

        db = BrowserDatabase.get(this);
        rv = findViewById(R.id.recyclerView);
        etSearch = findViewById(R.id.etSearch);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadBookmarks();

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int count) { searchBookmarks(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadBookmarks() {
        new Thread(() -> {
            list = db.bookmarkDao().getAll();
            runOnUiThread(() -> setAdapter(list));
        }).start();
    }

    private void searchBookmarks(String q) {
        new Thread(() -> {
            List<Bookmark> results = q.isEmpty() ? db.bookmarkDao().getAll()
                    : db.bookmarkDao().search("%" + q + "%");
            runOnUiThread(() -> setAdapter(results));
        }).start();
    }

    private void setAdapter(List<Bookmark> items) {
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_list, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                Bookmark bm = items.get(pos);
                TextView title = h.itemView.findViewById(R.id.tvTitle);
                TextView sub   = h.itemView.findViewById(R.id.tvSubtitle);
                title.setText(bm.title);
                sub.setText(bm.url);
                h.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(BookmarksActivity.this, MainActivity.class);
                    i.setData(android.net.Uri.parse(bm.url));
                    startActivity(i); finish();
                });
                h.itemView.setOnLongClickListener(v -> {
                    new android.app.AlertDialog.Builder(BookmarksActivity.this)
                        .setTitle("Delete Bookmark?")
                        .setMessage(bm.title)
                        .setPositiveButton("Delete", (d, w) -> {
                            new Thread(() -> {
                                db.bookmarkDao().delete(bm);
                                runOnUiThread(() -> loadBookmarks());
                            }).start();
                        }).setNegativeButton("Cancel", null).show();
                    return true;
                });
            }
            @Override public int getItemCount() { return items.size(); }
        });
        rv.announceForAccessibility(items.size() + " bookmarks");
    }
}
