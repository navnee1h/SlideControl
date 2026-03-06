package com.slidecontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etIpAddress;
    private EditText etPort;
    private Switch switchService;
    private TextView tvStatus;
    private TextView tvConnection;
    private SharedPreferences prefs;
    private Handler pingHandler = new Handler();
    private Runnable pingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SlideControl", MODE_PRIVATE);

        etIpAddress  = findViewById(R.id.etIpAddress);
        etPort       = findViewById(R.id.etPort);
        switchService = findViewById(R.id.switchService);
        tvStatus     = findViewById(R.id.tvStatus);
        tvConnection = findViewById(R.id.tvConnection);

        etIpAddress.setText(prefs.getString("ip", "192.168.1.10"));
        etPort.setText(prefs.getString("port", "5000"));

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            saveSettings();
            startPinging(); // restart ping with new IP
        });
        findViewById(R.id.btnTestNext).setOnClickListener(v -> sendCommand("next"));
        findViewById(R.id.btnTestPrev).setOnClickListener(v -> sendCommand("prev"));

        switchService.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                saveSettings();
                requestBatteryOptimizationExemption();
                startVolumeService();
            } else {
                stopVolumeService();
            }
        });

        if (!prefs.getBoolean("battery_asked", false)) {
            prefs.edit().putBoolean("battery_asked", true).apply();
            showBatteryOptimizationDialog();
        }

        startPinging();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean running = VolumeService.isRunning;
        switchService.setChecked(running);
        tvStatus.setText(running ? "🟢 Service Running" : "🔴 Service Stopped");
        startPinging();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPinging();
    }

    // Ping the server every 3 seconds and update the indicator
    private void startPinging() {
        stopPinging();
        pingRunnable = new Runnable() {
            @Override
            public void run() {
                pingServer();
                pingHandler.postDelayed(this, 3000);
            }
        };
        pingHandler.post(pingRunnable);
    }

    private void stopPinging() {
        if (pingRunnable != null) {
            pingHandler.removeCallbacks(pingRunnable);
        }
    }

    private void pingServer() {
        String ip   = etIpAddress.getText().toString().trim();
        String port = etPort.getText().toString().trim();
        if (ip.isEmpty()) return;
        String url = "http://" + ip + ":" + port + "/ping";
        new Thread(() -> {
            boolean connected = false;
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                int code = conn.getResponseCode();
                connected = (code == 200);
                conn.disconnect();
            } catch (Exception ignored) {}
            final boolean isConnected = connected;
            runOnUiThread(() -> {
                if (isConnected) {
                    tvConnection.setText("🟢 Server Connected");
                    tvConnection.setTextColor(0xFF4CAF50);
                } else {
                    tvConnection.setText("🔴 Server Disconnected");
                    tvConnection.setTextColor(0xFFE53935);
                }
            });
        }).start();
    }

    private void saveSettings() {
        String ip   = etIpAddress.getText().toString().trim();
        String port = etPort.getText().toString().trim();
        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter IP address", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.edit().putString("ip", ip).putString("port", port).apply();
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
    }

    private void startVolumeService() {
        Intent intent = new Intent(this, VolumeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        tvStatus.setText("🟢 Service Running");
        Toast.makeText(this, "Volume control started!", Toast.LENGTH_SHORT).show();
    }

    private void stopVolumeService() {
        Intent intent = new Intent(this, VolumeService.class);
        stopService(intent);
        tvStatus.setText("🔴 Service Stopped");
        Toast.makeText(this, "Volume control stopped.", Toast.LENGTH_SHORT).show();
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private void showBatteryOptimizationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Keep Service Running")
            .setMessage("To keep volume control working with screen off, allow SlideControl to run without battery restrictions.\n\nTap OK to open battery settings.")
            .setPositiveButton("OK", (d, w) -> requestBatteryOptimizationExemption())
            .setNegativeButton("Skip", null)
            .show();
    }

    private void sendCommand(String cmd) {
        String ip   = etIpAddress.getText().toString().trim();
        String port = etPort.getText().toString().trim();
        String url  = "http://" + ip + ":" + port + "/" + cmd;
        new Thread(() -> {
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                conn.disconnect();
                runOnUiThread(() ->
                    Toast.makeText(this, "✅ " + cmd + " sent!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
