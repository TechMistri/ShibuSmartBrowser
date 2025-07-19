package com.shibu.shibusmart.browser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int NETWORK_SPEED_THRESHOLD_KBPS = 10; // 10 kbps threshold for lite mode

    private WebView webView;
    private EditText addressBar;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView networkStatusBar;
    private ImageButton btnBack, btnForward, btnRefresh;
    private FloatingActionButton fabMenu;

    private boolean isIncognitoMode = false;
    private boolean isDesktopMode = false;
    private boolean isNightMode = false;
    private boolean isAdBlockEnabled = true;
    private boolean isLiteMode = false;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Check if app lock is enabled
        preferences = ShibuSmartBrowserApp.getInstance().getPreferences();
        if (preferences.getBoolean("app_lock", false)) {
            startActivity(new Intent(this, AppLockActivity.class));
        }
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Initialize UI components
        webView = findViewById(R.id.web_view);
        addressBar = findViewById(R.id.address_bar);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        networkStatusBar = findViewById(R.id.network_status_bar);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        fabMenu = findViewById(R.id.fab_menu);
        
        // Set up WebView
        setupWebView();
        
        // Set up address bar
        setupAddressBar();
        
        // Set up navigation buttons
        setupNavigationButtons();
        
        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());
        
        // Set up FAB menu
        fabMenu.setOnClickListener(view -> showFabMenu());
        
        // Load homepage
        String homepage = preferences.getString("homepage", "https://www.google.com");
        webView.loadUrl(homepage);
        
        // Monitor network speed
        monitorNetworkSpeed();
        
        // Handle intent (e.g., when app is opened from a link)
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                webView.loadUrl(uri.toString());
            }
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // Apply text size from preferences
        int textSize = preferences.getInt("text_size", 100);
        webSettings.setTextZoom(textSize);
        
        // Apply desktop mode if enabled
        if (isDesktopMode) {
            webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36");
        }
        
        // Enable ad blocking if enabled
        isAdBlockEnabled = preferences.getBoolean("adblock", true);
        
        // Set WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                addressBar.setText(url);
                updateNavigationButtons();
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                
                // Apply night mode if enabled
                if (isNightMode) {
                    applyNightMode(view);
                }
                
                // Apply lite mode if enabled
                if (isLiteMode) {
                    applyLiteMode(view);
                }
            }
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Ad blocking implementation
                if (isAdBlockEnabled) {
                    String url = request.getUrl().toString();
                    if (isAdUrl(url)) {
                        return new WebResourceResponse(
                                "text/plain",
                                "utf-8",
                                new ByteArrayInputStream("".getBytes())
                        );
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        
        // Set WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
            }
        });
        
        // Set download listener
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            // Check for storage permission
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE
                );
                return;
            }
            
            downloadFile(url, userAgent, contentDisposition, mimetype);
        });
    }
    
    private void setupAddressBar() {
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String url = addressBar.getText().toString();
                loadUrl(url);
                
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
                
                return true;
            }
            return false;
        });
    }
    
    private void setupNavigationButtons() {
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        
        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });
        
        btnRefresh.setOnClickListener(v -> webView.reload());
        
        updateNavigationButtons();
    }
    
    private void updateNavigationButtons() {
        btnBack.setEnabled(webView.canGoBack());
        btnBack.setAlpha(webView.canGoBack() ? 1.0f : 0.5f);
        
        btnForward.setEnabled(webView.canGoForward());
        btnForward.setAlpha(webView.canGoForward() ? 1.0f : 0.5f);
    }
    
    private void loadUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Check if it's a valid URL without scheme
            if (URLUtil.isValidUrl("http://" + url)) {
                url = "http://" + url;
            } else {
                // Treat as a search query
                String searchEngine = preferences.getString("search_engine", "google");
                String searchUrl;
                
                switch (searchEngine) {
                    case "bing":
                        searchUrl = "https://www.bing.com/search?q=";
                        break;
                    case "duckduckgo":
                        searchUrl = "https://duckduckgo.com/?q=";
                        break;
                    case "yahoo":
                        searchUrl = "https://search.yahoo.com/search?p=";
                        break;
                    case "google":
                    default:
                        searchUrl = "https://www.google.com/search?q=";
                        break;
                }
                
                url = searchUrl + Uri.encode(url);
            }
        }
        
        webView.loadUrl(url);
    }
    
    private void showFabMenu() {
        // Create a popup menu with browser actions
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] options = {
                "Bookmarks",
                "History",
                "Downloads",
                "Share",
                "Find in page",
                "Desktop mode: " + (isDesktopMode ? "On" : "Off"),
                "Night mode: " + (isNightMode ? "On" : "Off"),
                "Incognito mode: " + (isIncognitoMode ? "On" : "Off"),
                "Ad blocker: " + (isAdBlockEnabled ? "On" : "Off"),
                "Save as PDF",
                "Translate page",
                "AI Summarize",
                "Settings"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Bookmarks
                    startActivity(new Intent(this, BookmarksActivity.class));
                    break;
                case 1: // History
                    startActivity(new Intent(this, HistoryActivity.class));
                    break;
                case 2: // Downloads
                    startActivity(new Intent(this, DownloadManagerActivity.class));
                    break;
                case 3: // Share
                    shareUrl();
                    break;
                case 4: // Find in page
                    showFindInPageDialog();
                    break;
                case 5: // Desktop mode
                    toggleDesktopMode();
                    break;
                case 6: // Night mode
                    toggleNightMode();
                    break;
                case 7: // Incognito mode
                    toggleIncognitoMode();
                    break;
                case 8: // Ad blocker
                    toggleAdBlocker();
                    break;
                case 9: // Save as PDF
                    createPdf();
                    break;
                case 10: // Translate page
                    translatePage();
                    break;
                case 11: // AI Summarize
                    summarizePage();
                    break;
                case 12: // Settings
                    startActivity(new Intent(this, SettingsActivity.class));
                    break;
            }
        });
        
        builder.show();
    }
    
    private void shareUrl() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
    
    private void showFindInPageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_find_in_page, null);
        EditText findInput = dialogView.findViewById(R.id.find_input);
        ImageButton btnFindNext = dialogView.findViewById(R.id.btn_find_next);
        ImageButton btnFindPrev = dialogView.findViewById(R.id.btn_find_prev);
        ImageButton btnFindClose = dialogView.findViewById(R.id.btn_find_close);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        findInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                webView.findAllAsync(findInput.getText().toString());
                return true;
            }
            return false;
        });
        
        btnFindNext.setOnClickListener(v -> webView.findNext(true));
        btnFindPrev.setOnClickListener(v -> webView.findNext(false));
        btnFindClose.setOnClickListener(v -> {
            webView.clearMatches();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        
        WebSettings webSettings = webView.getSettings();
        if (isDesktopMode) {
            webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36");
        } else {
            webSettings.setUserAgentString(null);
        }
        
        webView.reload();
        Toast.makeText(this, "Desktop mode: " + (isDesktopMode ? "On" : "Off"), Toast.LENGTH_SHORT).show();
    }
    
    private void toggleNightMode() {
        isNightMode = !isNightMode;
        
        if (isNightMode) {
            applyNightMode(webView);
        } else {
            webView.reload();
        }
        
        Toast.makeText(this, "Night mode: " + (isNightMode ? "On" : "Off"), Toast.LENGTH_SHORT).show();
    }
    
    private void applyNightMode(WebView webView) {
        // Inject CSS to apply dark theme
        String css = "javascript:(function() {" +
                "var css = 'html, body, body * { background-color: #121212 !important; color: #FFFFFF !important; border-color: #616161 !important; }' + " +
                "'a, a * { color: #BB86FC !important; }' + " +
                "'input, textarea { background-color: #2C2C2C !important; color: #FFFFFF !important; }' + " +
                "'img, video { filter: brightness(0.8) !important; }' + " +
                "'button, input[type=\"button\"], input[type=\"submit\"] { background-color: #2C2C2C !important; color: #FFFFFF !important; border: 1px solid #616161 !important; }';" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.appendChild(document.createTextNode(css));" +
                "document.head.appendChild(style);" +
                "})()";
        webView.evaluateJavascript(css, null);
    }
    
    private void toggleIncognitoMode() {
        isIncognitoMode = !isIncognitoMode;
        
        if (isIncognitoMode) {
            // Clear cookies and cache
            CookieManager.getInstance().removeAllCookies(null);
            webView.clearCache(true);
            webView.clearHistory();
            
            // Disable saving form data
            webView.getSettings().setSaveFormData(false);
            
            // Disable saving passwords
            webView.getSettings().setSavePassword(false);
        } else {
            // Re-enable features
            webView.getSettings().setSaveFormData(true);
            webView.getSettings().setSavePassword(true);
        }
        
        Toast.makeText(this, "Incognito mode: " + (isIncognitoMode ? "On" : "Off"), Toast.LENGTH_SHORT).show();
    }
    
    private void toggleAdBlocker() {
        isAdBlockEnabled = !isAdBlockEnabled;
        
        // Save preference
        preferences.edit().putBoolean("adblock", isAdBlockEnabled).apply();
        
        // Reload page to apply changes
        webView.reload();
        
        Toast.makeText(this, "Ad blocker: " + (isAdBlockEnabled ? "On" : "Off"), Toast.LENGTH_SHORT).show();
    }
    
    private boolean isAdUrl(String url) {
        // Simple ad URL detection
        String[] adDomains = {
                "doubleclick.net",
                "googleadservices.com",
                "googlesyndication.com",
                "adservice.google.",
                "advertising.com",
                "adnxs.com",
                "ad.doubleclick.net"
        };
        
        for (String domain : adDomains) {
            if (url.contains(domain)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void createPdf() {
        // Check for storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE
            );
            return;
        }
        
        // Create a print job
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        String jobName = getString(R.string.app_name) + " Document";
        
        PrintDocumentAdapter printAdapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            printAdapter = webView.createPrintDocumentAdapter(jobName);
        } else {
            printAdapter = webView.createPrintDocumentAdapter();
        }
        
        printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
        
        Toast.makeText(this, "Creating PDF...", Toast.LENGTH_SHORT).show();
    }
    
    private void translatePage() {
        // Simple implementation using Google Translate
        String url = webView.getUrl();
        String translateUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(url);
        webView.loadUrl(translateUrl);
    }
    
    private void summarizePage() {
        // This would require a more complex implementation with AI services
        // For now, just show a toast message
        Toast.makeText(this, "AI summarization feature coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void downloadFile(String url, String userAgent, String contentDisposition, String mimeType) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        
        // Show download options dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Download File");
        builder.setMessage("Do you want to download " + fileName + "?");
        
        builder.setPositiveButton("Download", (dialog, which) -> {
            // Use Android's built-in download manager
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading file...");
            request.setTitle(fileName);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            
            Snackbar.make(webView, "Downloading " + fileName, Snackbar.LENGTH_LONG).show();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void monitorNetworkSpeed() {
        // Simplified network monitoring without ReactiveNetwork library
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onCapabilitiesChanged(android.net.Network network, NetworkCapabilities networkCapabilities) {
                        super.onCapabilitiesChanged(network, networkCapabilities);
                        
                        // Check network speed
                        int speedKbps = getNetworkSpeedKbps();
                        
                        runOnUiThread(() -> {
                            if (speedKbps > 0 && speedKbps <= NETWORK_SPEED_THRESHOLD_KBPS) {
                                // Switch to lite mode
                                if (!isLiteMode) {
                                    isLiteMode = true;
                                    networkStatusBar.setText(R.string.msg_slow_connection);
                                    networkStatusBar.setVisibility(View.VISIBLE);
                                    
                                    // Apply lite mode to current page
                                    if (webView.getUrl() != null) {
                                        applyLiteMode(webView);
                                    }
                                }
                            } else if (speedKbps > NETWORK_SPEED_THRESHOLD_KBPS) {
                                // Switch to full mode
                                if (isLiteMode) {
                                    isLiteMode = false;
                                    networkStatusBar.setText(R.string.msg_fast_connection);
                                    networkStatusBar.setVisibility(View.VISIBLE);
                                    
                                    // Hide status bar after a delay
                                    networkStatusBar.postDelayed(() -> 
                                            networkStatusBar.setVisibility(View.GONE), 3000);
                                    
                                    // Reload page to exit lite mode
                                    webView.reload();
                                }
                            }
                        });
                    }
                });
            }
        }
    }
    
    private int getNetworkSpeedKbps() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return 0;
        
        NetworkCapabilities nc = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        }
        
        if (nc == null) return 0;
        
        // Get bandwidth in Kbps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return nc.getLinkDownstreamBandwidthKbps();
        }
        
        return 0;
    }
    
    private void applyLiteMode(WebView webView) {
        // Disable images and JavaScript to save bandwidth
        WebSettings settings = webView.getSettings();
        settings.setLoadsImagesAutomatically(false);
        settings.setJavaScriptEnabled(false);
        
        // Inject CSS to simplify page
        String css = "javascript:(function() {" +
                "var css = 'video, iframe, canvas, svg, .video-container, .media-container { display: none !important; }' + " +
                "'img { display: none !important; }' + " +
                "'* { background-image: none !important; }' + " +
                "'body, article, .content, .main { font-size: 16px !important; line-height: 1.5 !important; }' + " +
                "'body * { max-width: 100% !important; }' + " +
                "'@font-face { font-display: swap !important; }';" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.appendChild(document.createTextNode(css));" +
                "document.head.appendChild(style);" +
                "})()";
        webView.evaluateJavascript(css, null);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if app lock is enabled
        if (preferences.getBoolean("app_lock", false)) {
            startActivity(new Intent(this, AppLockActivity.class));
        }
        
        // Apply text size from preferences
        int textSize = preferences.getInt("text_size", 100);
        webView.getSettings().setTextZoom(textSize);
    }
    
    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }
}