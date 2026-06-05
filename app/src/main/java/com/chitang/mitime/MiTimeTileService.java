package com.chitang.mitime;

import android.annotation.TargetApi;
import android.annotation.SuppressLint;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

@SuppressLint("UseRequiresApi")
@TargetApi(Build.VERSION_CODES.N)
public class MiTimeTileService extends TileService {
    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        FloatingWindowHelper.toggle(this);
        updateTile();
        MiTimeWidget.updateAllWidgets(this);
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        FloatingWindowHelper.UiState state = FloatingWindowHelper.readUiState(this);
        boolean enabled = state == FloatingWindowHelper.UiState.ENABLED;
        boolean canWrite = state != FloatingWindowHelper.UiState.PERMISSION_MISSING;

        tile.setLabel(getString(R.string.app_name));
        tile.setIcon(Icon.createWithResource(this,
                canWrite ? (enabled ? R.drawable.ic_tile_on : R.drawable.ic_tile_off) : R.drawable.ic_tile_unavailable));

        tile.setState(canWrite
                ? (enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE)
                : Tile.STATE_UNAVAILABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(getString(canWrite
                    ? (enabled ? R.string.status_enabled_short : R.string.status_disabled_short)
                    : R.string.status_write_settings_off_short));
        }
        tile.updateTile();
    }
}
