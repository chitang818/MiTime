package com.chitang.mitime;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String ACTION_QUICK_SETTINGS_SETTINGS =
            "android.settings.QUICK_SETTINGS_SETTINGS";
    private static final String PREFS_THEME = "theme";
    private static final String KEY_IS_LIGHT_THEME = "is_light_theme_stage";
    private static final long CLOCK_TICK_OFFSET_MS = 40L;

    private static final ThemePalette DARK_THEME = new ThemePalette(
            Color.rgb(4, 12, 28),
            Color.argb(42, 255, 255, 255),
            Color.argb(54, 126, 165, 255),
            Color.rgb(242, 246, 255),
            Color.rgb(164, 178, 205),
            Color.rgb(40, 125, 255),
            Color.argb(60, 125, 166, 255),
            Color.rgb(118, 128, 148),
            Color.rgb(255, 180, 84),
            Color.argb(48, 255, 255, 255),
            Color.rgb(242, 246, 255),
            Color.rgb(4, 12, 28),
            Color.argb(54, 255, 255, 255)
    );
    private static final ThemePalette LIGHT_THEME = new ThemePalette(
            Color.rgb(246, 250, 255),
            Color.argb(178, 255, 255, 255),
            Color.argb(168, 255, 255, 255),
            Color.rgb(16, 24, 40),
            Color.rgb(105, 117, 139),
            Color.rgb(47, 124, 246),
            Color.argb(190, 234, 243, 255),
            Color.rgb(113, 113, 122),
            Color.rgb(245, 158, 11),
            Color.argb(170, 244, 248, 253),
            Color.rgb(16, 24, 40),
            Color.rgb(246, 250, 255),
            Color.argb(150, 193, 213, 239)
    );

    private View root;
    private View pageContent;
    private TextView appTitle;
    private ImageButton themeToggleButton;
    private TextView homeSubtitle;
    private View statusPanel;
    private View statusIconWrap;
    private View statusChip;
    private View timePillWrap;
    private View connectorLine;
    private ImageView statusDot;
    private ImageView floatingIcon;
    private TextView statusTitle;
    private TextView timePreviewText;
    private TextView statusText;
    private TextView verifyText;
    private View permissionPanel;
    private TextView readinessTitle;
    private View readinessDivider;
    private ImageView permissionDot;
    private TextView permissionTitle;
    private TextView permissionButton;
    private View tileIconWrap;
    private ImageView tileIcon;
    private TextView tileTitle;
    private TextView tileButton;
    private TextView footerText;
    private ThemePalette currentPalette;
    private boolean isLightTheme;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final Runnable clockTicker = new Runnable() {
        @Override
        public void run() {
            updateClockPreview();
            postNextClockTick();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentPalette = resolveThemePalette();
        setTheme(isLightTheme ? R.style.AppTheme_Light : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        root = findViewById(R.id.root);
        pageContent = findViewById(R.id.pageContent);
        appTitle = findViewById(R.id.appTitle);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        homeSubtitle = findViewById(R.id.homeSubtitle);
        statusPanel = findViewById(R.id.statusPanel);
        statusIconWrap = findViewById(R.id.statusIconWrap);
        statusChip = findViewById(R.id.statusChip);
        timePillWrap = findViewById(R.id.timePillWrap);
        connectorLine = findViewById(R.id.connectorLine);
        statusDot = findViewById(R.id.statusDot);
        floatingIcon = findViewById(R.id.floatingIcon);
        statusTitle = findViewById(R.id.statusTitle);
        timePreviewText = findViewById(R.id.timePreviewText);
        statusText = findViewById(R.id.statusText);
        verifyText = findViewById(R.id.verifyText);
        permissionPanel = findViewById(R.id.permissionPanel);
        readinessTitle = findViewById(R.id.readinessTitle);
        readinessDivider = findViewById(R.id.readinessDivider);
        permissionDot = findViewById(R.id.permissionDot);
        permissionTitle = findViewById(R.id.permissionTitle);
        permissionButton = findViewById(R.id.permissionButton);
        tileIconWrap = findViewById(R.id.tileIconWrap);
        tileIcon = findViewById(R.id.tileIcon);
        tileTitle = findViewById(R.id.tileTitle);
        tileButton = findViewById(R.id.tileButton);
        footerText = findViewById(R.id.footerText);

        bindSystemInsets();
        applyTheme();
        themeToggleButton.setOnClickListener(v -> onThemeToggleClicked());
        permissionButton.setOnClickListener(v -> openWriteSettingsPage());
        tileButton.setOnClickListener(v -> onAddTileClicked());
        statusChip.setOnClickListener(v -> onToggleClicked());

        footerText.setText(getString(R.string.app_footer, getVersionName()));
        updateClockPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentPalette = resolveThemePalette();
        applyTheme();
        refreshState(false);
        clockHandler.removeCallbacks(clockTicker);
        clockTicker.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTicker);
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
        animatePress(statusChip);
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

        statusChip.setEnabled(true);

        statusTitle.setText(R.string.time_floating_window);
        statusText.setText(canWrite
                ? (enabled ? R.string.status_enabled_short : R.string.status_disabled_short)
                : R.string.status_write_settings_off_short);
        verifyText.setText(canWrite
                ? (enabled ? R.string.toggle_hint_turn_off : R.string.toggle_hint_turn_on)
                : R.string.verify_write_settings_off);
        statusChip.setContentDescription(getString(canWrite
                ? (enabled ? R.string.toggle_hint_turn_off : R.string.toggle_hint_turn_on)
                : R.string.write_settings_get));
        permissionButton.setText(canWrite
                ? R.string.write_settings_open
                : R.string.write_settings_get);
        refreshReadinessState(canWrite, palette);

        statusDot.setImageResource(canWrite
                ? (enabled ? R.drawable.ic_switch_on : R.drawable.ic_switch_off)
                : R.drawable.ic_switch_unavailable);
        statusDot.setColorFilter(accent);
        connectorLine.setBackgroundColor(blendAlpha(accent, isLightTheme ? 80 : 150));
        floatingIcon.setColorFilter(accent);
        floatingIcon.setAlpha(canWrite && !enabled ? 0.62f : 1f);
        timePreviewText.setTextColor(resolveTimeTextColor(palette, enabled, canWrite));
        timePillWrap.setAlpha(canWrite && !enabled ? 0.76f : 1f);
        statusPanel.setBackgroundColor(Color.TRANSPARENT);
        statusChip.setBackground(makeStatusChipBackground(accent));
        timePillWrap.setBackground(makeTimePillBackground(palette, enabled, canWrite));
        permissionPanel.setBackground(makeGlassRect(dp(24), palette.surface, palette.border));
        statusText.setTextColor(accent);
        verifyText.setTextColor(palette.textSecondary);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            timePillWrap.setElevation(dp(canWrite && enabled ? 12 : 4));
            statusChip.setElevation(dp(canWrite && enabled ? 8 : 4));
        }

        if (animated) {
            animateStateChange(statusPanel, floatingIcon);
        }
    }

    private ThemePalette resolveThemePalette() {
        SharedPreferences prefs = getSharedPreferences(PREFS_THEME, MODE_PRIVATE);
        if (prefs.contains(KEY_IS_LIGHT_THEME)) {
            isLightTheme = prefs.getBoolean(KEY_IS_LIGHT_THEME, false);
        } else {
            isLightTheme = false;
        }
        return isLightTheme ? LIGHT_THEME : DARK_THEME;
    }

    private void applyTheme() {
        ThemePalette palette = currentPalette != null ? currentPalette : resolveThemePalette();
        root.setBackground(makePageBackground());
        if (root instanceof AtmosphereScrollView) {
            ((AtmosphereScrollView) root).setLightTheme(isLightTheme);
        }
        getWindow().getDecorView().setBackgroundColor(palette.background);
        statusPanel.setBackgroundColor(Color.TRANSPARENT);
        statusIconWrap.setBackground(makePermissionIconBackground(palette.accent));
        statusChip.setBackground(makeStatusChipBackground(palette.accent));
        timePillWrap.setBackground(makeTimePillBackground(palette, true, true));
        timePillWrap.setAlpha(1f);
        floatingIcon.setAlpha(1f);
        permissionPanel.setBackground(makeGlassRect(dp(24), palette.surface, palette.border));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            timePillWrap.setElevation(dp(12));
            statusChip.setElevation(dp(8));
        }

        appTitle.setTextColor(palette.textPrimary);
        homeSubtitle.setTextColor(palette.textSecondary);
        statusTitle.setTextColor(palette.textPrimary);
        timePreviewText.setTextColor(resolveTimeTextColor(palette, true, true));
        connectorLine.setBackgroundColor(blendAlpha(palette.accent, isLightTheme ? 80 : 150));
        statusText.setTextColor(palette.textPrimary);
        verifyText.setTextColor(palette.textSecondary);
        readinessTitle.setTextColor(palette.textPrimary);
        readinessDivider.setBackgroundColor(blendAlpha(palette.textSecondary, isLightTheme ? 34 : 46));
        permissionDot.setColorFilter(palette.accent);
        permissionTitle.setTextColor(palette.textPrimary);
        tileIcon.setColorFilter(palette.accent);
        tileTitle.setTextColor(palette.textPrimary);
        footerText.setTextColor(palette.textSecondary);

        themeToggleButton.setBackground(makeRoundRect(palette.themeButtonBackground, dp(15), palette.border));
        themeToggleButton.setImageResource(isLightTheme ? R.drawable.ic_theme_sun : R.drawable.ic_theme_moon);
        themeToggleButton.setColorFilter(palette.themeIcon);
        themeToggleButton.setContentDescription(getString(
                isLightTheme ? R.string.theme_switch_to_dark : R.string.theme_switch_to_light
        ));

        applySystemBars(palette);
    }

    private void refreshReadinessState(boolean canWrite, ThemePalette palette) {
        boolean added = MiTimeTileService.isTileAdded(this);
        int success = Color.rgb(34, 197, 94);
        int pending = palette.warning;
        int tileAccent = added ? success : pending;
        permissionButton.setText(canWrite
                ? R.string.status_badge_done
                : R.string.write_settings_get);
        permissionButton.setTextColor(canWrite ? palette.textPrimary : Color.WHITE);
        permissionButton.setBackground(makeRoundRect(
                canWrite ? palette.controlBackground : pending,
                dp(15),
                canWrite ? palette.border : pending
        ));
        permissionDot.setColorFilter(canWrite ? success : pending);
        statusIconWrap.setBackground(makePermissionIconBackground(canWrite ? success : pending));

        tileIcon.setColorFilter(tileAccent);
        tileIconWrap.setBackground(makePermissionIconBackground(tileAccent));
        if (added) {
            tileButton.setText(R.string.status_badge_added);
            tileButton.setTextColor(palette.textPrimary);
            tileButton.setBackground(makeRoundRect(palette.controlBackground, dp(15), palette.border));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tileButton.setText(R.string.quick_settings_tile_add);
            tileButton.setTextColor(Color.WHITE);
            tileButton.setBackground(makeRoundRect(tileAccent, dp(15), tileAccent));
        } else {
            tileButton.setText(R.string.quick_settings_tile_open);
            tileButton.setTextColor(Color.WHITE);
            tileButton.setBackground(makeRoundRect(tileAccent, dp(15), tileAccent));
        }
    }

    private void onAddTileClicked() {
        animatePress(tileButton);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            TileAddRequester.request(this);
        } else {
            Toast.makeText(this, R.string.quick_settings_tile_manual_toast, Toast.LENGTH_LONG).show();
            openQuickSettingsPage();
        }
    }

    private void onTileAddRequestResult(int result) {
        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED
                || result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
            MiTimeTileService.setTileAdded(this, true);
        }
        refreshState(false);

        int message;
        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
            message = R.string.quick_settings_tile_added_toast;
        } else if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
            message = R.string.quick_settings_tile_already_added_toast;
        } else if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED) {
            message = R.string.quick_settings_tile_not_added_toast;
        } else {
            message = R.string.quick_settings_tile_request_failed;
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void openQuickSettingsPage() {
        try {
            startActivity(new Intent(ACTION_QUICK_SETTINGS_SETTINGS));
        } catch (ActivityNotFoundException exception) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException fallbackException) {
                Toast.makeText(this, R.string.quick_settings_tile_page_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateClockPreview() {
        if (timePreviewText != null) {
            timePreviewText.setText(clockFormat.format(new Date()));
        }
    }

    private void postNextClockTick() {
        long now = System.currentTimeMillis();
        long delay = 1000L - (now % 1000L) + CLOCK_TICK_OFFSET_MS;
        clockHandler.postDelayed(clockTicker, delay);
    }

    private void bindSystemInsets() {
        final int start = pageContent.getPaddingStart();
        final int top = pageContent.getPaddingTop();
        final int end = pageContent.getPaddingEnd();
        final int bottom = pageContent.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            pageContent.setPaddingRelative(
                    start,
                    top,
                    end,
                    bottom
            );
            return insets;
        });
        root.requestApplyInsets();
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

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

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

    private GradientDrawable makePageBackground() {
        int[] colors = isLightTheme
                ? new int[]{
                Color.rgb(249, 252, 255),
                Color.rgb(231, 241, 255),
                Color.rgb(255, 252, 246),
                Color.rgb(248, 251, 255)
        }
                : new int[]{
                Color.rgb(4, 12, 28),
                Color.rgb(8, 22, 54),
                Color.rgb(24, 21, 68),
                Color.rgb(3, 8, 20)
        };
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        drawable.setDither(true);
        return drawable;
    }

    private GradientDrawable makeTimePillBackground(ThemePalette palette, boolean enabled, boolean canWrite) {
        int stateColor = canWrite ? (enabled ? palette.accent : palette.off) : palette.warning;
        int[] colors;
        if (isLightTheme) {
            colors = enabled && canWrite
                    ? new int[]{
                    Color.argb(244, 255, 255, 255),
                    Color.argb(226, 216, 235, 255),
                    Color.argb(242, 239, 247, 255)
            }
                    : new int[]{
                    Color.argb(188, 255, 255, 255),
                    Color.argb(132, 229, 234, 242),
                    Color.argb(178, 248, 250, 253)
            };
        } else {
            colors = enabled && canWrite
                    ? new int[]{
                    Color.argb(118, 255, 255, 255),
                    Color.argb(92, 67, 139, 255),
                    Color.argb(116, 255, 255, 255)
            }
                    : new int[]{
                    Color.argb(42, 255, 255, 255),
                    Color.argb(34, 110, 119, 138),
                    Color.argb(44, 255, 255, 255)
            };
        }
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        drawable.setCornerRadius(dp(64));
        drawable.setStroke(
                dp(enabled && canWrite ? 2 : 1),
                blendAlpha(stateColor, enabled && canWrite ? (isLightTheme ? 190 : 230) : (isLightTheme ? 92 : 120))
        );
        return drawable;
    }

    private int resolveTimeTextColor(ThemePalette palette, boolean enabled, boolean canWrite) {
        if (!canWrite) {
            return palette.warning;
        }
        if (enabled) {
            return isLightTheme ? Color.rgb(22, 63, 135) : Color.rgb(226, 239, 255);
        }
        return isLightTheme ? Color.rgb(92, 101, 119) : Color.rgb(138, 153, 176);
    }

    private GradientDrawable makeGlassRect(int radiusPx, int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radiusPx);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable makeStatusChipBackground(int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(blendAlpha(accent, isLightTheme ? 22 : 46));
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), blendAlpha(accent, isLightTheme ? 92 : 118));
        return drawable;
    }

    private GradientDrawable makePermissionIconBackground(int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(blendAlpha(accent, isLightTheme ? 32 : 42));
        drawable.setStroke(dp(1), blendAlpha(accent, isLightTheme ? 84 : 104));
        return drawable;
    }

    private static int blendAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
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

    private static final class TileAddRequester {
        @TargetApi(Build.VERSION_CODES.TIRAMISU)
        static void request(MainActivity activity) {
            try {
                StatusBarManager statusBarManager =
                        activity.getSystemService(StatusBarManager.class);
                if (statusBarManager == null) {
                    throw new IllegalStateException("Status bar service unavailable");
                }
                statusBarManager.requestAddTileService(
                        new ComponentName(activity, MiTimeTileService.class),
                        activity.getString(R.string.app_name),
                        Icon.createWithResource(activity, R.drawable.ic_tile_off),
                        activity.getMainExecutor(),
                        activity::onTileAddRequestResult
                );
            } catch (RuntimeException exception) {
                Toast.makeText(
                        activity,
                        R.string.quick_settings_tile_request_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
