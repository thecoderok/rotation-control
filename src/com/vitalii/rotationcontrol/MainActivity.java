package com.vitalii.rotationcontrol;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Single-screen UI: grant the overlay permission, then pick an orientation.
 * All the real work happens in RotationService.
 */
public class MainActivity extends Activity {

    public static final String PREFS = "rotation";
    public static final String KEY_MODE = "mode"; // ActivityInfo screenOrientation constant

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);

        findViewById(R.id.grant).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });

        ((Button) findViewById(R.id.btn_landscape)).setOnClickListener(v ->
                startMode(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, "landscape"));
        ((Button) findViewById(R.id.btn_sensor_landscape)).setOnClickListener(v ->
                startMode(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, "sensor landscape"));
        ((Button) findViewById(R.id.btn_portrait)).setOnClickListener(v ->
                startMode(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, "portrait"));
        ((Button) findViewById(R.id.btn_auto)).setOnClickListener(v ->
                startMode(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR, "auto"));

        findViewById(R.id.btn_off).setOnClickListener(v -> {
            stopService(new Intent(this, RotationService.class));
            Toast.makeText(this, "Rotation control off", Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void startMode(int mode, String label) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant overlay permission first", Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        p.edit().putInt(KEY_MODE, mode).apply();

        Intent i = new Intent(this, RotationService.class);
        startForegroundService(i);
        Toast.makeText(this, "Forcing " + label, Toast.LENGTH_SHORT).show();
        refreshStatus();
    }

    private void refreshStatus() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean running = p.getBoolean(RotationService.KEY_RUNNING, false);
        int mode = p.getInt(KEY_MODE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        String m;
        switch (mode) {
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE: m = "landscape"; break;
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE: m = "sensor landscape"; break;
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT: m = "portrait"; break;
            case ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR: m = "auto"; break;
            default: m = "—"; break;
        }
        boolean overlay = Settings.canDrawOverlays(this);
        status.setText("Status: " + (running ? ("ON — forcing " + m) : "off")
                + "\nOverlay permission: " + (overlay ? "granted" : "not granted"));
    }
}
