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
import com.mandy.adrianbrowser.model.HistoryItem;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView rv;
    private BrowserDatabase db;
    private EditText etSearch;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_list);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("History");
        db = BrowserDatabase.get(this);
        rv = findViewById(R.id.recyclerView);
        etSearch = findViewById(R.id.etSearch);
        rv.setLayoutManager(new LinearLayoutManager(this));
        loadHistory(null);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int count) { loadHistory(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        Button btnClear = findViewById(R.id.btnAction);
        btnClear.setVisibility(View.VISIBLE);
        btnClear.setText("Clear All History");
        btnClear.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Clear History?")
                .setPositiveButton("Clear", (d, w) -> {
                    new Thread(() -> { db.historyDao().clearAll(); runOnUiThread(() -> loadHistory(null)); }).start();
                }).setNegativeButton("Cancel", null).show();
        });
    }

    private void loadHistory(String q) {
        new Thread(() -> {
            List<HistoryItem> items = (q == null || q.isEmpty()) ? db.historyDao().getAll()
                    : db.historyDao().search("%" + q + "%");
            runOnUiThread(() -> {
                rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
                    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_list, p, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }
                    @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                        HistoryItem hi = items.get(pos);
                        ((TextView) h.itemView.findViewById(R.id.tvTitle)).setText(hi.title);
                        ((TextView) h.itemView.findViewById(R.id.tvSubtitle)).setText(
                                sdf.format(new Date(hi.timestamp)) + " — " + hi.url);
                        h.itemView.setOnClickListener(v -> {
                            Intent i = new Intent(HistoryActivity.this, MainActivity.class);
                            i.setData(android.net.Uri.parse(hi.url));
                            startActivity(i); finish();
                        });
                    }
                    @Override public int getItemCount() { return items.size(); }
                });
            });
        }).start();
    }
}
