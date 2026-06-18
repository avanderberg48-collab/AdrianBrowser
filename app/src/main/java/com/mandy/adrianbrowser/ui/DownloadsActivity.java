package com.mandy.adrianbrowser.ui;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.mandy.adrianbrowser.R;
import com.mandy.adrianbrowser.db.BrowserDatabase;
import com.mandy.adrianbrowser.model.DownloadItem;
import java.text.SimpleDateFormat;
import java.util.*;

public class DownloadsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_list);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Downloads");
        BrowserDatabase db = BrowserDatabase.get(this);
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        new Thread(() -> {
            List<DownloadItem> items = db.downloadDao().getAll();
            runOnUiThread(() -> {
                rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
                    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_list, p, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }
                    @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                        DownloadItem di = items.get(pos);
                        ((TextView)h.itemView.findViewById(R.id.tvTitle)).setText(di.fileName);
                        ((TextView)h.itemView.findViewById(R.id.tvSubtitle)).setText(
                                di.status + " — " + sdf.format(new Date(di.timestamp)));
                    }
                    @Override public int getItemCount() { return items.size(); }
                });
                if (items.isEmpty()) rv.announceForAccessibility("No downloads yet");
            });
        }).start();
    }
}
