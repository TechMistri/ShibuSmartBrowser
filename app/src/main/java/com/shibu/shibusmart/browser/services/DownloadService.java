package com.shibu.shibusmart.browser.services;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.shibu.shibusmart.browser.DownloadManagerActivity;
import com.shibu.shibusmart.browser.R;

import java.io.File;

public class DownloadService extends Service {

    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private DownloadManager downloadManager;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize notification channel
        createNotificationChannel();
        
        // Get notification manager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Initialize download manager
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String url = intent.getStringExtra("url");
            String fileName = intent.getStringExtra("fileName");
            String mimeType = intent.getStringExtra("mimeType");
            String userAgent = intent.getStringExtra("userAgent");
            
            if (url != null && fileName != null) {
                startDownload(url, fileName, mimeType, userAgent);
            }
        }
        
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Download notifications");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void startDownload(String url, String fileName, String mimeType, String userAgent) {
        // Show initial notification
        showDownloadNotification(fileName, 0);
        
        // Create download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setDescription("Downloading file...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        
        if (mimeType != null) {
            request.setMimeType(mimeType);
        }
        
        if (userAgent != null) {
            request.addRequestHeader("User-Agent", userAgent);
        }
        
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        
        // Start download
        long downloadId = downloadManager.enqueue(request);
        
        // Stop service after starting download
        stopForeground(true);
        stopSelf();
    }
    
    private void showDownloadNotification(String fileName, int progress) {
        // Create intent for notification click
        Intent intent = new Intent(this, DownloadManagerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(fileName)
                .setContentText(getString(R.string.msg_downloading))
                .setProgress(100, progress, true)
                .setOngoing(true)
                .setContentIntent(pendingIntent);
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }
    
    private String getMimeType(String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return null;
    }
}