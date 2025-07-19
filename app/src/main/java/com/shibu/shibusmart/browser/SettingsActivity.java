package com.shibu.shibusmart.browser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            
            // Handle clear browsing data preference
            Preference clearDataPref = findPreference("clear_data");
            if (clearDataPref != null) {
                clearDataPref.setOnPreferenceClickListener(preference -> {
                    showClearDataDialog();
                    return true;
                });
            }
            
            // Handle app lock preference
            SwitchPreferenceCompat appLockPref = findPreference("app_lock");
            if (appLockPref != null) {
                appLockPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        // Check if PIN is already set
                        SharedPreferences securePrefs = ShibuSmartBrowserApp.getInstance().getSecurePreferences();
                        String pin = securePrefs.getString("app_lock_pin", null);
                        
                        if (pin == null) {
                            // No PIN set, prompt to create one
                            Intent intent = new Intent(getActivity(), AppLockActivity.class);
                            startActivity(intent);
                            
                            // Return false to prevent the switch from being toggled until PIN is set
                            return false;
                        }
                    }
                    return true;
                });
            }
            
            // Handle VPN preference
            SwitchPreferenceCompat vpnPref = findPreference("vpn");
            if (vpnPref != null) {
                vpnPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        // Start VPN service
                        Intent intent = android.net.VpnService.prepare(getActivity());
                        if (intent != null) {
                            startActivityForResult(intent, 0);
                        } else {
                            onActivityResult(0, RESULT_OK, null);
                        }
                    } else {
                        // Stop VPN service
                        Intent intent = new Intent(getActivity(), com.shibu.shibusmart.browser.services.VpnService.class);
                        intent.setAction("STOP");
                        getActivity().startService(intent);
                    }
                    return true;
                });
            }
            
            // Handle download location preference
            Preference downloadLocationPref = findPreference("download_location");
            if (downloadLocationPref != null) {
                downloadLocationPref.setOnPreferenceClickListener(preference -> {
                    // Show file picker to select download location
                    // This would require a more complex implementation
                    Toast.makeText(getActivity(), "This feature is coming soon!", Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
        }
        
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 0 && resultCode == RESULT_OK) {
                // VPN permission granted, start the service
                Intent intent = new Intent(getActivity(), com.shibu.shibusmart.browser.services.VpnService.class);
                getActivity().startService(intent);
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
        
        private void showClearDataDialog() {
            String[] items = {
                    "Browsing history",
                    "Cookies and site data",
                    "Cached images and files",
                    "Passwords",
                    "Autofill form data"
            };
            
            boolean[] checkedItems = {true, true, true, false, false};
            
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Clear browsing data");
            builder.setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                checkedItems[which] = isChecked;
            });
            
            builder.setPositiveButton("Clear data", (dialog, which) -> {
                clearBrowsingData(checkedItems);
            });
            
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
        
        private void clearBrowsingData(boolean[] options) {
            // Clear selected browsing data
            if (options[0]) {
                // Clear history
                // In a real implementation, this would clear the history database
            }
            
            if (options[1]) {
                // Clear cookies and site data
                // In a real implementation, this would clear cookies and site data
            }
            
            if (options[2]) {
                // Clear cache
                // In a real implementation, this would clear the cache
            }
            
            if (options[3]) {
                // Clear passwords
                // In a real implementation, this would clear saved passwords
            }
            
            if (options[4]) {
                // Clear form data
                // In a real implementation, this would clear form data
            }
            
            Toast.makeText(getActivity(), "Browsing data cleared", Toast.LENGTH_SHORT).show();
        }
    }
}