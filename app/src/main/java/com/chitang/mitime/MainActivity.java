package com.chitang.mitime;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS_THEME = "theme";
    private static final String KEY_IS_LIGHT_THEME = "is_light_theme";

    private static final ThemePalette DARK_THEME = new ThemePalette(
            Color.rgb(10, 13, 20),
            Color.rgb(22, 28, 39),
            Color.rgb(29, 37, 50),
            Color.rgb(242, 246, 255),
            Color.rgb(143, 154, 172),
            Color.rgb(34, 132, 255),
            Color.rgb(13, 71, 150),
            Color.rgb(71, 82, 99),
            Color.rgb(255, 180, 84),
            Color.rgb(22, 28, 39),
            Color.rgb(242, 246, 255),
            Color.rgb(10, 13, 20),
            Color.argb(22, 255, 255, 255)
    );
    private static final ThemePalette LIGHT_THEME = new ThemePalette(
            Color.rgb(246, 248, 251),
            Color.WHITE,
            Color.rgb(255, 255, 255),
            Color.rgb(18, 24, 38),
            Color.rgb(95, 107, 122),
            Color.rgb(34, 132, 255),
            Color.rgb(216, 233, 255),
            Color.rgb(138, 150, 168),
            Color.rgb(232, 139, 42),
            Color.rgb(233, 238, 247),
            Color.rgb(18, 24, 38),
            Color.rgb(246, 248, 251),
            Color.rgb(221, 228, 238)
    );

    private View root;
    private View appIconWrap;
    private TextView appTitle;
    private ImageButton themeToggleButton;
    private TextView homeSubtitle;
    private View statusPanel;
    private View statusDot;
    private ImageView floatingIcon;
    private TextView statusTitle;
    private TextView statusText;
    private TextView verifyText;
    private View permissionPanel;
    private View permissionDot;
    private TextView permissionTitle;
    private TextView permissionStatus;
    private TextView permissionButton;
    private TextView toggleButton;
    private TextView statusHint;
    private TextView footerText;
    private ThemePalette currentPalette;
    private boolean isLightTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentPalette = resolveThemePalette();
        setTheme(isLightTheme ? R.style.AppTheme_Light : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        root = findViewById(R.id.root);
        appIconWrap = findViewById(R.id.appIconWrap);
        appTitle = findViewById(R.id.appTitle);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        homeSubtitle = findViewById(R.id.homeSubtitle);
        statusPanel = findViewById(R.id.statusPanel);
        statusDot = findViewById(R.id.statusDot);
        floatingIcon = findViewById(R.id.floatingIcon);
        statusTitle = findViewById(R.id.statusTitle);
        statusText = findViewById(R.id.statusText);
        verifyText = findViewById(R.id.verifyText);
        permissionPanel = findViewById(R.id.permissionPanel);
        permissionDot = findViewById(R.id.permissionDot);
        permissionTitle = findViewById(R.id.permissionTitle);
        permissionStatus = findViewById(R.id.permissionStatus);
        permissionButton = findViewById(R.id.permissionButton);
        toggleButton = findViewById(R.id.toggleButton);
        statusHint = findViewById(R.id.statusHint);
        footerText = findViewById(R.id.footerText);

        applyTheme();
        themeToggleButton.setOnClickListener(v -> onThemeToggleClicked());
        permissionButton.setOnClickListener(v -> openWriteSettingsPage());
        toggleButton.setOnClickListener(v -> onToggleClicked());

        footerText.setText(getString(R.string.app_footer, getVersionName()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentPalette = resolveThemePalette();
        applyTheme();
        refreshState(false);
    }

    private void onThemeToggleClicked() {
        animatePress(themeToggleButton);
        isLightTheme = !isLightTheme;
        getSharedPreferences(PREFS_THEME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LIGHT_THEME, isLightTheme)
                .apply();
        currentPalette = isLightTheme ? LIGHT_THEME : DARK_THEME;
        applyTheme();
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
        ThemePalette palette = currentPalette != null ? currentPalette : resolveThemePalette();
        FloatingWindowHelper.UiState state = FloatingWindowHelper.readUiState(this);
        boolean canWrite = state != FloatingWindowHelper.UiState.PERMISSION_MISSING;
        boolean enabled = state == FloatingWindowHelper.UiState.ENABLED;
        int accent = canWrite ? (enabled ? palette.accent : palette.off) : palette.warning;

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
        permissionDot.setBackground(makeOval(canWrite ? palette.accent : palette.warning));
        floatingIcon.setColorFilter(accent);
        toggleButton.setTextColor(Color.WHITE);
        toggleButton.setBackground(makeOval(accent));
        permissionButton.setTextColor(canWrite ? palette.textPrimary : Color.WHITE);
        permissionButton.setBackground(makeRoundRect(
                canWrite ? palette.controlBackground : palette.warning,
                dp(19),
                canWrite ? palette.border : palette.warning
        ));
        statusPanel.setBackground(makeRoundRect(palette.surfaceElevated, dp(26), palette.border));
        permissionPanel.setBackground(makeRoundRect(palette.surface, dp(22), palette.border));

        if (animated) {
            animateStateChange(statusPanel, floatingIcon);
        }
    }

    private ThemePalette resolveThemePalette() {
        SharedPreferences prefs = getSharedPreferences(PREFS_THEME, MODE_PRIVATE);
        if (prefs.contains(KEY_IS_LIGHT_THEME)) {
            isLightTheme = prefs.getBoolean(KEY_IS_LIGHT_THEME, false);
        } else {
            int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isLightTheme = nightMode != Configuration.UI_MODE_NIGHT_YES;
        }
        return isLightTheme ? LIGHT_THEME : DARK_THEME;
    }

    private void applyTheme() {
        ThemePalette palette = currentPalette != null ? currentPalette : resolveThemePalette();
        root.setBackgroundColor(palette.background);
        getWindow().getDecorView().setBackgroundColor(palette.background);
        appIconWrap.setBackground(makeRoundRect(palette.appIconBackground, dp(16), palette.border));
        statusPanel.setBackground(makeRoundRect(palette.surfaceElevated, dp(26), palette.border));
        permissionPanel.setBackground(makeRoundRect(palette.surface, dp(22), palette.border));

        appTitle.setTextColor(palette.textPrimary);
        homeSubtitle.setTextColor(palette.textSecondary);
        statusTitle.setTextColor(palette.textPrimary);
        statusText.setTextColor(palette.textPrimary);
        verifyText.setTextColor(palette.textSecondary);
        permissionTitle.setTextColor(palette.textPrimary);
        permissionStatus.setTextColor(palette.textSecondary);
        statusHint.setTextColor(palette.textSecondary);
        footerText.setTextColor(palette.textSecondary);

        themeToggleButton.setBackground(makeRoundRect(palette.themeButtonBackground, dp(15), palette.border));
        themeToggleButton.setImageResource(isLightTheme ? R.drawable.ic_theme_moon : R.drawable.ic_theme_sun);
        themeToggleButton.setColorFilter(palette.themeIcon);
        themeToggleButton.setContentDescription(getString(
                isLightTheme ? R.string.theme_switch_to_dark : R.string.theme_switch_to_light
        ));

        applySystemBars(palette);
    }

    private void applySystemBars(ThemePalette palette) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().setStatusBarColor(palette.systemBar);
        getWindow().setNavigationBarColor(palette.systemBar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(palette.systemBar);
        }
        if (isLightTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (isLightTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
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

    private static GradientDrawable makeRoundRect(int color, int radiusPx, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        drawable.setStroke(1, strokeColor);
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

    private static final class ThemePalette {
        final int background;
        final int surface;
        final int surfaceElevated;
        final int textPrimary;
        final int textSecondary;
        final int accent;
        final int appIconBackground;
        final int off;
        final int warning;
        final int themeButtonBackground;
        final int themeIcon;
        final int systemBar;
        final int border;
        final int controlBackground;

        ThemePalette(
                int background,
                int surface,
                int surfaceElevated,
                int textPrimary,
                int textSecondary,
                int accent,
                int appIconBackground,
                int off,
                int warning,
                int themeButtonBackground,
                int themeIcon,
                int systemBar,
                int border
        ) {
            this.background = background;
            this.surface = surface;
            this.surfaceElevated = surfaceElevated;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.accent = accent;
            this.appIconBackground = appIconBackground;
            this.off = off;
            this.warning = warning;
            this.themeButtonBackground = themeButtonBackground;
            this.themeIcon = themeIcon;
            this.systemBar = systemBar;
            this.border = border;
            this.controlBackground = themeButtonBackground;
        }
    }
}
