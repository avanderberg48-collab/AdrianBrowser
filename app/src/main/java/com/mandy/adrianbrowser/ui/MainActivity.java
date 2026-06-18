package com.mandy.adrianbrowser.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mandy.adrianbrowser.R;
import com.mandy.adrianbrowser.db.BrowserDatabase;
import com.mandy.adrianbrowser.model.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlBar;
    private ProgressBar progressBar;
    private ImageButton btnBack, btnForward, btnRefresh, btnBookmark, btnTabs;
    private TextView tvTabCount;
    private BottomNavigationView bottomNav;

    private BrowserDatabase db;
    private List<TabItem> tabs = new ArrayList<>();
    private int currentTabIndex = 0;
    private Map<Integer, WebView> webViews = new HashMap<>();

    private static final String HOME_URL = "https://www.google.com";
    private static final int PERM_CODE = 101;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_REQ = 102;

    private SharedPreferences prefs;
    private boolean isDarkMode = false;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = BrowserDatabase.get(this);
        prefs = getSharedPreferences("browser_prefs", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);

        bindViews();
        setupBottomNav();
        requestPermissions();

        // Open first tab
        TabItem first = new TabItem("New Tab", HOME_URL);
        first.isActive = true;
        tabs.add(first);
        setupWebView(first);
        updateTabCount();

        // Handle intent URL (opened from another app)
        if (getIntent() != null && getIntent().getData() != null) {
            loadUrl(getIntent().getData().toString());
        }
    }

    private void bindViews() {
        urlBar      = findViewById(R.id.urlBar);
        progressBar = findViewById(R.id.progressBar);
        btnBack     = findViewById(R.id.btnBack);
        btnForward  = findViewById(R.id.btnForward);
        btnRefresh  = findViewById(R.id.btnRefresh);
        btnBookmark = findViewById(R.id.btnBookmark);
        btnTabs     = findViewById(R.id.btnTabs);
        tvTabCount  = findViewById(R.id.tvTabCount);
        bottomNav   = findViewById(R.id.bottomNav);
        webView     = new WebView(this); // placeholder, replaced per tab

        btnBack.setOnClickListener(v -> { if (getActive().canGoBack()) getActive().goBack(); });
        btnForward.setOnClickListener(v -> { if (getActive().canGoForward()) getActive().goForward(); });
        btnRefresh.setOnClickListener(v -> getActive().reload());
        btnBookmark.setOnClickListener(v -> toggleBookmark());
        btnTabs.setOnClickListener(v -> showTabsDialog());

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String input = urlBar.getText().toString().trim();
                loadUrl(input);
                return true;
            }
            return false;
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(TabItem tab) {
        WebView wv = new WebView(this);
        wv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setTextZoom(prefs.getInt("text_zoom", 100));
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 12; AdrianBrowser) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        // Dark mode
        if (isDarkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wv.getSettings().setForceDark(WebSettings.FORCE_DARK_ON);
        }

        wv.setWebViewClient(new BrowserClient(tab));
        wv.setWebChromeClient(new BrowserChromeClient(tab));

        wv.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            triggerDownload(url, contentDisposition, mimeType, contentLength);
        });

        webViews.put(tab.id, wv);

        FrameLayout container = findViewById(R.id.webContainer);
        container.removeAllViews();
        container.addView(wv);
        webView = wv;

        wv.loadUrl(tab.url);
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        tabs.get(currentTabIndex).isActive = false;
        currentTabIndex = index;
        TabItem tab = tabs.get(currentTabIndex);
        tab.isActive = true;

        FrameLayout container = findViewById(R.id.webContainer);
        container.removeAllViews();

        WebView wv = webViews.get(tab.id);
        if (wv == null) {
            setupWebView(tab);
        } else {
            container.addView(wv);
            webView = wv;
            urlBar.setText(tab.url);
            updateNavButtons();
        }
        updateTabCount();
    }

    private void newTab(String url) {
        TabItem tab = new TabItem("New Tab", url != null ? url : HOME_URL);
        tabs.add(tab);
        currentTabIndex = tabs.size() - 1;
        setupWebView(tab);
        updateTabCount();
    }

    private void closeTab(int index) {
        if (tabs.size() <= 1) { getActive().loadUrl(HOME_URL); return; }
        WebView wv = webViews.remove(tabs.get(index).id);
        if (wv != null) { wv.stopLoading(); wv.destroy(); }
        tabs.remove(index);
        if (currentTabIndex >= tabs.size()) currentTabIndex = tabs.size() - 1;
        switchToTab(currentTabIndex);
    }

    private WebView getActive() { return webViews.getOrDefault(tabs.get(currentTabIndex).id, webView); }

    private void loadUrl(String input) {
        String url;
        if (input.startsWith("http://") || input.startsWith("https://") || input.startsWith("file://")) {
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            url = "https://www.google.com/search?q=" + Uri.encode(input);
        }
        getActive().loadUrl(url);
        tabs.get(currentTabIndex).url = url;
    }

    private void toggleBookmark() {
        String url = getActive().getUrl();
        String title = getActive().getTitle();
        if (url == null) return;
        new Thread(() -> {
            Bookmark existing = db.bookmarkDao().findByUrl(url);
            runOnUiThread(() -> {
                if (existing != null) {
                    new Thread(() -> db.bookmarkDao().delete(existing)).start();
                    btnBookmark.setImageResource(android.R.drawable.btn_star_big_off);
                    announce("Bookmark removed");
                } else {
                    new Thread(() -> db.bookmarkDao().insert(new Bookmark(title != null ? title : url, url))).start();
                    btnBookmark.setImageResource(android.R.drawable.btn_star_big_on);
                    announce("Bookmarked: " + (title != null ? title : url));
                }
            });
        }).start();
    }

    private void showTabsDialog() {
        String[] tabTitles = new String[tabs.size() + 2];
        for (int i = 0; i < tabs.size(); i++) {
            tabTitles[i] = (i == currentTabIndex ? "▶ " : "   ") + tabs.get(i).title;
        }
        tabTitles[tabs.size()] = "+ New Tab";
        tabTitles[tabs.size() + 1] = "✕ Close Current Tab";

        new AlertDialog.Builder(this)
            .setTitle("Tabs (" + tabs.size() + " open)")
            .setItems(tabTitles, (d, which) -> {
                if (which < tabs.size()) {
                    switchToTab(which);
                } else if (which == tabs.size()) {
                    newTab(null);
                } else {
                    closeTab(currentTabIndex);
                }
            })
            .show();
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadUrl(HOME_URL); return true;
            } else if (id == R.id.nav_bookmarks) {
                startActivity(new Intent(this, BookmarksActivity.class)); return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class)); return true;
            } else if (id == R.id.nav_downloads) {
                startActivity(new Intent(this, DownloadsActivity.class)); return true;
            } else if (id == R.id.nav_more) {
                showMoreMenu(); return true;
            }
            return false;
        });
    }

    private void showMoreMenu() {
        String[] options = {"🧩 Extensions", "⚙️ Settings", "🔒 Incognito Tab", "📤 Share Page",
                            "🔍 Find on Page", "🖨️ Print Page", "🗑️ Clear Data"};
        new AlertDialog.Builder(this)
            .setTitle("More Options")
            .setItems(options, (d, which) -> {
                switch (which) {
                    case 0: startActivity(new Intent(this, ExtensionsActivity.class)); break;
                    case 1: startActivity(new Intent(this, SettingsActivity.class)); break;
                    case 2: openIncognito(); break;
                    case 3: sharePage(); break;
                    case 4: showFindOnPage(); break;
                    case 5: printPage(); break;
                    case 6: clearData(); break;
                }
            }).show();
    }

    private void openIncognito() {
        TabItem tab = new TabItem("Incognito", HOME_URL);
        tabs.add(tab);
        currentTabIndex = tabs.size() - 1;
        WebView wv = new WebView(this);
        wv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 12; AdrianBrowser) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        wv.setWebViewClient(new BrowserClient(tab));
        wv.setWebChromeClient(new BrowserChromeClient(tab));
        webViews.put(tab.id, wv);
        FrameLayout container = findViewById(R.id.webContainer);
        container.removeAllViews();
        container.addView(wv);
        webView = wv;
        wv.loadUrl(HOME_URL);
        updateTabCount();
        Toast.makeText(this, "Incognito tab opened — browsing not saved", Toast.LENGTH_SHORT).show();
        announce("Incognito tab opened. Your browsing will not be saved.");
    }

    private void sharePage() {
        String url = getActive().getUrl();
        if (url == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, url);
        share.putExtra(Intent.EXTRA_SUBJECT, getActive().getTitle());
        startActivity(Intent.createChooser(share, "Share page"));
    }

    private void showFindOnPage() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Find on Page");
        EditText et = new EditText(this);
        et.setHint("Type to search...");
        b.setView(et);
        b.setPositiveButton("Find", (d, w) -> {
            getActive().findAllAsync(et.getText().toString());
        });
        b.setNegativeButton("Close", (d, w) -> getActive().clearMatches());
        b.show();
    }

    private void printPage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            android.print.PrintManager pm = (android.print.PrintManager) getSystemService(PRINT_SERVICE);
            if (pm != null) {
                String jobName = getActive().getTitle() != null ? getActive().getTitle() : "Web Page";
                pm.print(jobName, getActive().createPrintDocumentAdapter(jobName), null);
            }
        }
    }

    private void clearData() {
        new AlertDialog.Builder(this)
            .setTitle("Clear Browsing Data")
            .setMessage("This will clear cache, cookies and history.")
            .setPositiveButton("Clear", (d, w) -> {
                WebStorage.getInstance().deleteAllData();
                getActive().clearCache(true);
                getActive().clearHistory();
                CookieManager.getInstance().removeAllCookies(null);
                new Thread(() -> db.historyDao().clearAll()).start();
                Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show();
                announce("All browsing data cleared");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void triggerDownload(String url, String contentDisposition, String mimeType, long length) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
        req.setMimeType(mimeType);
        req.addRequestHeader("User-Agent", getActive().getSettings().getUserAgentString());
        req.setDescription("Downloading via Adrian Browser");
        req.setTitle(fileName);
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (dm != null) dm.enqueue(req);
        new Thread(() -> db.downloadDao().insert(new DownloadItem(fileName, url))).start();
        Toast.makeText(this, "Downloading: " + fileName, Toast.LENGTH_SHORT).show();
        announce("Download started: " + fileName);
    }

    private void updateNavButtons() {
        btnBack.setEnabled(getActive().canGoBack());
        btnForward.setEnabled(getActive().canGoForward());
        btnBack.setAlpha(getActive().canGoBack() ? 1f : 0.4f);
        btnForward.setAlpha(getActive().canGoForward() ? 1f : 0.4f);
    }

    private void updateTabCount() {
        tvTabCount.setText(String.valueOf(tabs.size()));
    }

    private void announce(String msg) {
        bottomNav.announceForAccessibility(msg);
    }

    // ── WebViewClient ─────────────────────────────────────────────────────────

    private class BrowserClient extends WebViewClient {
        private final TabItem tab;
        BrowserClient(TabItem tab) { this.tab = tab; }

        @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            urlBar.setText(url);
            tab.url = url;
        }

        @Override public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            tab.title = view.getTitle() != null ? view.getTitle() : url;
            tab.url = url;
            urlBar.setText(url);
            updateNavButtons();

            // Save to history (not for incognito)
            if (!tab.title.equals("Incognito")) {
                new Thread(() -> db.historyDao().insert(new HistoryItem(tab.title, url))).start();
            }

            // Check if bookmarked
            new Thread(() -> {
                Bookmark b = db.bookmarkDao().findByUrl(url);
                runOnUiThread(() -> btnBookmark.setImageResource(
                    b != null ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off));
            }).start();

            announce("Page loaded: " + tab.title);
        }

        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.startsWith("tel:")) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url))); return true;
            }
            if (url.startsWith("mailto:")) {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url))); return true;
            }
            if (url.startsWith("intent:")) {
                try {
                    Intent i = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    startActivity(i);
                } catch (Exception ignored) {}
                return true;
            }
            view.loadUrl(url);
            return true;
        }

        @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame()) {
                String errHtml = "<html><body style='background:#1a1a2e;color:#eee;font-family:sans-serif;" +
                        "display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;" +
                        "text-align:center;padding:24px;'>" +
                        "<h2 style='color:#4fc3f7;'>⚠ Page Not Available</h2>" +
                        "<p>Could not load this page. Check your internet connection and try again.</p>" +
                        "<button onclick='history.back()' style='padding:12px 24px;background:#0288d1;" +
                        "color:#fff;border:none;border-radius:8px;font-size:16px;margin:8px;'>← Go Back</button>" +
                        "<button onclick='location.reload()' style='padding:12px 24px;background:#388e3c;" +
                        "color:#fff;border:none;border-radius:8px;font-size:16px;margin:8px;'>🔄 Retry</button>" +
                        "</body></html>";
                view.loadData(errHtml, "text/html", "UTF-8");
            }
        }
    }

    // ── WebChromeClient ───────────────────────────────────────────────────────

    private class BrowserChromeClient extends WebChromeClient {
        private final TabItem tab;
        BrowserChromeClient(TabItem tab) { this.tab = tab; }

        @Override public void onProgressChanged(WebView view, int p) {
            progressBar.setProgress(p);
            if (p == 100) progressBar.setVisibility(View.GONE);
        }

        @Override public void onReceivedTitle(WebView view, String title) {
            tab.title = title;
        }

        @Override public void onPermissionRequest(PermissionRequest request) {
            runOnUiThread(() -> request.grant(request.getResources()));
        }

        @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
            cb.invoke(origin, true, false);
        }

        @Override public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams params) {
            fileCallback = cb;
            startActivityForResult(params.createIntent(), FILE_REQ);
            return true;
        }

        @Override public boolean onJsAlert(WebView v, String url, String msg, JsResult r) {
            new AlertDialog.Builder(MainActivity.this)
                .setMessage(msg)
                .setPositiveButton("OK", (d, w) -> r.confirm())
                .setOnCancelListener(d -> r.cancel())
                .show();
            return true;
        }

        @Override public boolean onJsConfirm(WebView v, String url, String msg, JsResult r) {
            new AlertDialog.Builder(MainActivity.this)
                .setMessage(msg)
                .setPositiveButton("Yes", (d, w) -> r.confirm())
                .setNegativeButton("No", (d, w) -> r.cancel())
                .show();
            return true;
        }

        @Override public boolean onConsoleMessage(ConsoleMessage m) { return true; }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == FILE_REQ && fileCallback != null) {
            Uri[] results = null;
            if (res == RESULT_OK && data != null && data.getDataString() != null) {
                results = new Uri[]{Uri.parse(data.getDataString())};
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (getActive().canGoBack()) getActive().goBack();
        else super.onBackPressed();
    }

    @Override protected void onResume() { super.onResume(); if (webView != null) webView.onResume(); }
    @Override protected void onPause() { super.onPause(); if (webView != null) webView.onPause(); }

    private void requestPermissions() {
        String[] perms = {
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        List<String> needed = new ArrayList<>();
        for (String p : perms)
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) needed.add(p);
        if (!needed.isEmpty())
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERM_CODE);
    }
}
