package com.shibu.shibusmart.browser.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.shibu.shibusmart.browser.ShibuSmartBrowserApp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VpnService extends android.net.VpnService {

    private static final String TAG = "VpnService";
    private static final String VPN_ADDRESS = "10.0.0.2";
    private static final String VPN_ROUTE = "0.0.0.0";
    private static final String VPN_DNS = "8.8.8.8";

    private ParcelFileDescriptor vpnInterface = null;
    private ExecutorService executorService;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("STOP")) {
                stopVpn();
                return START_NOT_STICKY;
            }
        }

        // Start VPN
        startVpn();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    private void startVpn() {
        if (isRunning) {
            return;
        }

        try {
            // Configure VPN
            Builder builder = new Builder()
                    .addAddress(VPN_ADDRESS, 32)
                    .addRoute(VPN_ROUTE, 0)
                    .addDnsServer(VPN_DNS)
                    .setSession("ShibuSmart VPN")
                    .setMtu(1500);

            // Create VPN interface
            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN connection");
                return;
            }

            isRunning = true;

            // Start VPN threads
            executorService.submit(new VpnRunnable(vpnInterface.getFileDescriptor()));

            // Save VPN state
            SharedPreferences preferences = ShibuSmartBrowserApp.getInstance().getPreferences();
            preferences.edit().putBoolean("vpn_active", true).apply();

            Log.i(TAG, "VPN service started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting VPN: " + e.getMessage());
            stopVpn();
        }
    }

    private void stopVpn() {
        isRunning = false;

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                vpnInterface = null;
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN interface: " + e.getMessage());
            }
        }

        // Save VPN state
        SharedPreferences preferences = ShibuSmartBrowserApp.getInstance().getPreferences();
        preferences.edit().putBoolean("vpn_active", false).apply();

        // Stop service
        stopSelf();

        Log.i(TAG, "VPN service stopped");
    }

    private static class VpnRunnable implements Runnable {
        private final java.io.FileDescriptor fileDescriptor;
        private static final int BUFFER_SIZE = 2048;

        VpnRunnable(java.io.FileDescriptor fileDescriptor) {
            this.fileDescriptor = fileDescriptor;
        }

        @Override
        public void run() {
            try {
                FileChannel inputChannel = new FileInputStream(fileDescriptor).getChannel();
                FileChannel outputChannel = new FileOutputStream(fileDescriptor).getChannel();

                ByteBuffer packet = ByteBuffer.allocate(BUFFER_SIZE);

                while (!Thread.interrupted()) {
                    // Read from VPN interface
                    int readBytes = inputChannel.read(packet);
                    if (readBytes > 0) {
                        // Process packet (in a real VPN, you would modify/route packets here)
                        packet.flip();

                        // Write back to VPN interface
                        outputChannel.write(packet);
                        packet.clear();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "VPN thread error: " + e.getMessage());
            }
        }
    }
}