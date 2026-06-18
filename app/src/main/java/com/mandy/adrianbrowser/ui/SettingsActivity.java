package com.mandy.adrianbrowser.ui;

import android.content.SharedPreferences;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.mandy.adrianbrowser.R;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Settings");

        prefs = getSharedPreferences("browser_prefs", MODE_PRIVATE);

        Switch swDark       = findViewById(R.id.swDarkMode);
        Switch swJavascript = findViewById(R.id.swJavascript);
        Switch swCookies    = findViewById(R.id.swCookies);
        Switch swImages     = findViewById(R.id.swImages);
        Switch swDesktop    = findViewById(R.id.swDesktopSite);
        Switch swSavePass   = findViewById(R.id.swSavePasswords);
        SeekBar sbZoom      = findViewById(R.id.sbTextZoom);
        TextView tvZoom     = findViewById(R.id.tvZoomValue);
        EditText etHomepage = findViewById(R.id.etHomepage);
        Button btnSave      = findViewById(R.id.btnSave);
        Button btnClearCache= findViewById(R.id.btnClearCache);

        // Load saved values
        swDark.setChecked(prefs.getBoolean("dark_mode", false));
        swJavascript.setChecked(prefs.getBoolean("javascript", true));
        swCookies.setChecked(prefs.getBoolean("cookies", true));
        swImages.setChecked(prefs.getBoolean("load_images", true));
        swDesktop.setChecked(prefs.getBoolean("desktop_site", false));
        swSavePass.setChecked(prefs.getBoolean("save_passwords", false));
        int zoom = prefs.getInt("text_zoom", 100);
        sbZoom.setProgress(zoom - 50);
        tvZoom.setText("Text Size: " + zoom + "%");
        etHomepage.setText(prefs.getString("homepage", "https://www.google.com"));

        sbZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean user) {
                tvZoom.setText("Text Size: " + (p + 50) + "%");
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        btnSave.setOnClickListener(v -> {
            prefs.edit()
                .putBoolean("dark_mode", swDark.isChecked())
                .putBoolean("javascript", swJavascript.isChecked())
                .putBoolean("cookies", swCookies.isChecked())
                .putBoolean("load_images", swImages.isChecked())
                .putBoolean("desktop_site", swDesktop.isChecked())
                .putBoolean("save_passwords", swSavePass.isChecked())
                .putInt("text_zoom", sbZoom.getProgress() + 50)
                .putString("homepage", etHomepage.getText().toString().trim())
                .apply();
            Toast.makeText(this, "Settings saved! Restart the browser to apply changes.", Toast.LENGTH_LONG).show();
            btnSave.announceForAccessibility("Settings saved successfully");
            finish();
        });

        btnClearCache.setOnClickListener(v -> {
            android.webkit.WebStorage.getInstance().deleteAllData();
            android.webkit.CookieManager.getInstance().removeAllCookies(null);
            Toast.makeText(this, "Cache and cookies cleared", Toast.LENGTH_SHORT).show();
        });
    }
}
