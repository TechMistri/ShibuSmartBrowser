package com.zyphora.browser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class AppLockActivity extends AppCompatActivity {

    private EditText pinInput;
    private Button btnUnlock;
    private Button btnUseFingerprint;
    private TextView lockMessage;
    
    private SharedPreferences securePreferences;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    
    private boolean isSettingPin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);
        
        pinInput = findViewById(R.id.pin_input);
        btnUnlock = findViewById(R.id.btn_unlock);
        btnUseFingerprint = findViewById(R.id.btn_use_fingerprint);
        lockMessage = findViewById(R.id.lock_message);
        
        securePreferences = ShibuSmartBrowserApp.getInstance().getSecurePreferences();
        
        // Check if PIN is set
        String savedPin = securePreferences.getString("app_lock_pin", null);
        if (savedPin == null) {
            // No PIN set, prompt to create one
            isSettingPin = true;
            lockMessage.setText(R.string.msg_enter_pin);
            btnUnlock.setText(R.string.btn_save);
        }
        
        btnUnlock.setOnClickListener(v -> {
            String pin = pinInput.getText().toString();
            if (pin.isEmpty()) {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (isSettingPin) {
                if (savedPin == null) {
                    // First time entering PIN, save temporarily and ask for confirmation
                    securePreferences.edit().putString("temp_pin", pin).apply();
                    lockMessage.setText(R.string.msg_confirm_pin);
                    pinInput.setText("");
                } else {
                    // Confirming PIN
                    String tempPin = securePreferences.getString("temp_pin", "");
                    if (pin.equals(tempPin)) {
                        // PINs match, save permanently
                        securePreferences.edit()
                                .putString("app_lock_pin", pin)
                                .remove("temp_pin")
                                .apply();
                        
                        Toast.makeText(this, R.string.msg_pin_set, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // PINs don't match
                        Toast.makeText(this, R.string.msg_pin_mismatch, Toast.LENGTH_SHORT).show();
                        lockMessage.setText(R.string.msg_enter_pin);
                        pinInput.setText("");
                        securePreferences.edit().remove("temp_pin").apply();
                    }
                }
            } else {
                // Verifying PIN
                if (pin.equals(savedPin)) {
                    // PIN correct
                    Toast.makeText(this, R.string.msg_authentication_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    // PIN incorrect
                    Toast.makeText(this, R.string.msg_authentication_failed, Toast.LENGTH_SHORT).show();
                    pinInput.setText("");
                }
            }
        });
        
        // Set up biometric authentication
        setupBiometricAuthentication();
    }
    
    private void setupBiometricAuthentication() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Biometric features are available
                btnUseFingerprint.setVisibility(View.VISIBLE);
                break;
            default:
                // Biometric features are unavailable
                btnUseFingerprint.setVisibility(View.GONE);
                return;
        }
        
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(AppLockActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(AppLockActivity.this, R.string.msg_authentication_success, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(AppLockActivity.this, R.string.msg_authentication_failed, Toast.LENGTH_SHORT).show();
            }
        });
        
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel")
                .build();
        
        btnUseFingerprint.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from bypassing lock
        if (!isSettingPin) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        } else {
            super.onBackPressed();
        }
    }
}