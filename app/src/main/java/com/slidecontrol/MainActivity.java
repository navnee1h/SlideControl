package com.slidecontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etIpAddress;
    private EditText etPort;
    private Switch switchService;
    private TextView tvStatus;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SlideControl", MODE_PRIVATE);

        etIpAddress   = findViewById(R.id.etIpAddress);
        etPort        = findViewById(R.id.etPort);
        switchService = findViewById(R.id.switchService);
        tvStatus      = findViewById(R.id.tvStatus);

        // Load saved values
        etIpAddress.setText(prefs.getString("ip", "192.168.1.10"));
        etPort.setText(prefs.getString("port", "5000"));

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveSettings());

        Button btnTestNext = findViewById(R.id.btnTestNext);
        btnTestNext.setOnClickListener(v -> sendCommand("next"));

        Button btnTestPrev = findViewById(R.id.btnTestPrev);
        btnTestPrev.setOnClickListener(v -> sendCommand("prev"));

        switchService.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                saveSettings();
                startVolumeService();
            } else {
                stopVolumeService();
            }
        });

        boolean running = VolumeService.isRunning;
        switchService.setChecked(running);
        tvStatus.setText(running ? "🟢 Service Running" : "🔴 Service Stopped");
    }

    private void saveSettings() {
        String ip   = etIpAddress.getText().toString().trim();
        String port = etPort.getText().toString().trim();

        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter IP address", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
            .putString("ip", ip)
            .putString("port", port)
            .apply();

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

    private void sendCommand(String cmd) {
        String ip   = etIpAddress.getText().toString().trim();
        String port = etPort.getText().toString().trim();
        String url  = "http://" + ip + ":" + port + "/" + cmd;
        new Thread(() -> {
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(3000);
                conn.getResponseCode();
                conn.disconnect();
                runOnUiThread(() ->
                    Toast.makeText(this, "✅ " + cmd + " sent!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
