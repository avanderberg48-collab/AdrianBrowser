package com.mandy.adrianbrowser.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.mandy.adrianbrowser.R;
import java.util.*;

public class ExtensionsActivity extends AppCompatActivity {

    // Popular Chrome extensions with their Chrome Web Store links
    private static final String[][] EXTENSIONS = {
        {"uBlock Origin", "Best ad blocker and privacy tool", "https://chrome.google.com/webstore/detail/ublock-origin/cjpalhdlnbpafiamejdnhcphjbkeiagm"},
        {"Dark Reader", "Dark mode for every website", "https://chrome.google.com/webstore/detail/dark-reader/eimadpbcbfnmbkopoojfekhnkhdbieeh"},
        {"LastPass", "Password manager and secure vault", "https://chrome.google.com/webstore/detail/lastpass-free-password-ma/hdokiejnpimakedhajhdlcegeplioahd"},
        {"Grammarly", "Writing assistant and spell checker", "https://chrome.google.com/webstore/detail/grammarly-grammar-checker/kbfnbcaeplbcioakkpcpgfkobkghlhen"},
        {"Google Translate", "Translate any web page instantly", "https://chrome.google.com/webstore/detail/google-translate/aapbdbdomjkkjkaonfhkkikfgjllcleb"},
        {"Honey", "Automatic coupon finder for shopping", "https://chrome.google.com/webstore/detail/honey-automatic-coupons-r/bmnlcjabgnpnenekpadlanbbkooimhnj"},
        {"Video Speed Controller", "Control video playback speed", "https://chrome.google.com/webstore/detail/video-speed-controller/nffaoalbilbmmfgbnbgppjihopabppdk"},
        {"OneTab", "Save all tabs to reduce memory", "https://chrome.google.com/webstore/detail/onetab/chphlpgkkbolifaimnlloiipkdnihall"},
        {"Browse Chrome Web Store", "See all extensions", "https://chrome.google.com/webstore/category/extensions"},
    };

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_list);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Extensions");

        TextView note = new TextView(this);
        note.setText("ℹ️ Adrian Browser can open Chrome extension pages. To install extensions that work on Android, use Kiwi Browser or Firefox for Android which support extensions natively.");
        note.setPadding(24, 16, 24, 16);
        note.setTextSize(14);

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_list, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                ((TextView)h.itemView.findViewById(R.id.tvTitle)).setText(EXTENSIONS[pos][0]);
                ((TextView)h.itemView.findViewById(R.id.tvSubtitle)).setText(EXTENSIONS[pos][1]);
                h.itemView.setOnClickListener(v -> {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(EXTENSIONS[pos][2]));
                    startActivity(i);
                });
            }
            @Override public int getItemCount() { return EXTENSIONS.length; }
        });
    }
}
