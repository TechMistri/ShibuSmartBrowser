package com.shibu.shibusmart.browser;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ShibuSmartBrowserApp extends Application {

    private static ShibuSmartBrowserApp instance;
    private SharedPreferences preferences;
    private SharedPreferences securePreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Initialize secure preferences for sensitive data
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            
            securePreferences = EncryptedSharedPreferences.create(
                    this,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Fallback to regular preferences if encryption fails
            securePreferences = getSharedPreferences("secure_prefs", Context.MODE_PRIVATE);
        }
        
        // Apply theme based on preferences
        applyTheme();
        
        // Enable WebView debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }
    
    public static ShibuSmartBrowserApp getInstance() {
        return instance;
    }
    
    public SharedPreferences getPreferences() {
        return preferences;
    }
    
    public SharedPreferences getSecurePreferences() {
        return securePreferences;
    }
    
    public void applyTheme() {
        String theme = preferences.getString("theme", "system");
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}