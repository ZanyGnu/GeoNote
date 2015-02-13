package geonote.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {

    private final Context mContext;
    SharedPreferences mPreferences = null;

    public Settings(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean shouldShowDetailViewWhenZoomedIn() {
        return mPreferences.getBoolean(
                mContext.getString(R.string.map_show_detail_view_when_zoomed_in),
                true);
    }

    public boolean isNotificationsEnabled() {
        return mPreferences.getBoolean(
                mContext.getString(R.string.enable_notifications),
                true);
    }

    public int getGeoFenceRadius() {
        return mPreferences.getInt(
                mContext.getString(R.string.geo_fence_radius),
                100);
    }
}
