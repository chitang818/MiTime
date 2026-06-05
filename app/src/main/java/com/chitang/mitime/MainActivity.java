package com.chitang.mitime;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int COLOR_BG = Color.rgb(10, 13, 20);
    private static final int COLOR_SURFACE = Color.rgb(22, 28, 39);
    private static final int COLOR_TEXT = Color.rgb(242, 246, 255);
    private static final int COLOR_MUTED = Color.rgb(143, 154, 172);
    private static final int COLOR_BLUE = Color.rgb(34, 132, 255);
    private static final int COLOR_BLUE_DARK = Color.rgb(13, 71, 150);
    private static final int COLOR_OFF = Color.rgb(71, 82, 99);

    private View root;
    private View appIconWrap;
    private View statusPanel;
    private View statusDot;
    private ImageView floatingIcon;
    private TextView statusTitle;
    private TextView statusText;
    private TextView verifyText;
    private View permissionPanel;
    private View permissionDot;
    private TextView permissionStatus;
    private TextView permissionButton;
    private TextView toggleButton;
    private TextView statusHint;
    private TextView footerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        root = findViewById(R.id.root);
        appIconWrap = findViewById(R.id.appIconWrap);
        statusPanel = findViewById(R.id.statusPanel);
        statusDot = findViewById(R.id.statusDot);
        floatingIcon = findViewById(R.id.floatingIcon);
        statusTitle = findViewById(R.id.statusTitle);
        statusText = findViewById(R.id.statusText);
        verifyText = findViewById(R.id.verifyText);
        permissionPanel = findViewById(R.id.permissionPanel);
        permissionDot = findViewById(R.id.permissionDot);
        permissionStatus = findViewById(R.id.permissionStatus);
        permissionButton = findViewById(R.id.permissionButton);
        toggleButton = findViewById(R.id.toggleButton);
        statusHint = findViewById(R.id.statusHint);
        footerText = findViewById(R.id.footerText);

        root.setBackgroundColor(COLOR_BG);
        appIconWrap.setBackground(makeRoundRect(COLOR_BLUE_DARK, dp(18)));
        statusPanel.setBackground(makeRoundRect(COLOR_SURFACE, dp(28)));
        permissionPanel.setBackground(makeRoundRect(COLOR_SURFACE, dp(24)));

        permissionButton.setOnClickListener(v -> openWriteSettingsPage());
        toggleButton.setOnClickListener(v -> onToggleClicked());

        footerText.setText(getString(R.string.app_footer, getVersionName()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState(false);
    }

    private void onToggleClicked() {
        animatePress(toggleButton);
        if (!PermissionHelper.canWriteSystemSettings(this)) {
            openWriteSettingsPage();
            return;
        }

        FloatingWindowHelper.ToggleResult result = FloatingWindowHelper.toggle(this);
        refreshState(true);
        MiTimeWidget.updateAllWidgets(this);

        if (!result.success) {
            Toast.makeText(
                    this,
                    result.permissionMissing
                            ? R.string.toggle_need_system_settings
                            : result.privateSettingBlocked
                            ? R.string.toggle_blocked_private_setting
                            : R.string.toggle_failed,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void refreshState(boolean animated) {
        FloatingWindowHelper.UiState state = FloatingWindowHelper.readUiState(this);
        boolean canWrite = state != FloatingWindowHelper.UiState.PERMISSION_MISSING;
        boolean enabled = state == FloatingWindowHelper.UiState.ENABLED;
        int accent = enabled ? COLOR_BLUE : COLOR_OFF;

        toggleButton.setEnabled(true);

        statusTitle.setText(R.string.time_floating_window);
        statusText.setText(canWrite
                ? (enabled ? R.string.status_enabled : R.string.status_disabled)
                : R.string.status_write_settings_off);
        verifyText.setText(canWrite ? R.string.verify_ok : R.string.verify_write_settings_off);
        permissionStatus.setText(canWrite
                ? R.string.write_settings_granted
                : R.string.write_settings_missing);
        permissionButton.setText(canWrite
                ? R.string.write_settings_open
                : R.string.write_settings_get);
        toggleButton.setText(canWrite
                ? (enabled ? R.string.action_turn_off : R.string.action_turn_on)
                : R.string.write_settings_get);
        statusHint.setText(canWrite ? R.string.status_hint : R.string.status_hint_write_settings_off);

        statusDot.setBackground(makeOval(accent));
        permissionDot.setBackground(makeOval(canWrite ? COLOR_BLUE : COLOR_OFF));
        floatingIcon.setColorFilter(accent);
        toggleButton.setTextColor(COLOR_TEXT);
        toggleButton.setBackground(makeOval(accent));
        permissionButton.setTextColor(COLOR_TEXT);
        permissionButton.setBackground(makeRoundRect(canWrite ? COLOR_OFF : COLOR_BLUE, dp(18)));
        statusPanel.setBackground(makeRoundRect(COLOR_SURFACE, dp(28)));
        permissionPanel.setBackground(makeRoundRect(COLOR_SURFACE, dp(24)));

        if (animated) {
            animateStateChange(statusPanel, floatingIcon);
        }
    }

    private void openWriteSettingsPage() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = new Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName())
            );
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallback.setData(Uri.parse("package:" + getPackageName()));
                startActivity(fallback);
            } catch (ActivityNotFoundException fallbackException) {
                Toast.makeText(this, R.string.write_settings_page_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static GradientDrawable makeOval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private static GradientDrawable makeRoundRect(int color, int radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        drawable.setStroke(1, Color.argb(22, 255, 255, 255));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception exception) {
            return "1.0";
        }
    }

    private void animatePress(View view) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.96f, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.96f, 1f)
        );
        set.setDuration(180);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    private void animateStateChange(View panel, View icon) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(panel, View.ALPHA, 0.72f, 1f),
                ObjectAnimator.ofFloat(icon, View.SCALE_X, 0.92f, 1f),
                ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0.92f, 1f)
        );
        set.setDuration(260);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
}
