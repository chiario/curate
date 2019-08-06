package com.example.curate.models;

import androidx.annotation.Nullable;

import java.util.HashMap;

import static com.example.curate.models.Party.LOCATION_PERMISSION_KEY;
import static com.example.curate.models.Party.NAME_KEY;
import static com.example.curate.models.Party.SONG_LIMIT_KEY;
import static com.example.curate.models.Party.USER_LIMIT_KEY;

public class Settings {
    private String name;
    private boolean isLocationEnabled;
    private Integer userLimit;
    private Integer songLimit;


    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public boolean getLocationEnabled() {
        return isLocationEnabled;
    }

    public void setLocationEnabled(boolean locationEnabled) {
        isLocationEnabled = locationEnabled;
    }

    public Integer getUserLimit() {
        return userLimit;
    }

    public void setUserLimit(@Nullable Integer userLimit) {
        this.userLimit = userLimit;
    }

    public Integer getSongLimit() {
        return songLimit;
    }

    public void setSongLimit(@Nullable Integer songLimit) {
        this.songLimit = songLimit;
    }

    /**
     * This returns a HashMap of the parameters to relay to the ParseCloud. For each setting,
     * if the setting has not changed from the oldSettings to the newSettings, it is set to null
     * in the parameters. Otherwise, it is set to its value in newSettings.
     *
     * @param oldSettings the original Settings object to compare against
     * @param newSettings the newest Settings object
     * @return the parameters for the ParseCloud function
     */
    static HashMap<String, Object> getSettingsParams(Settings newSettings, Settings oldSettings) {
        HashMap<String, Object> params = new HashMap<>();
        // First check if the settings have changed
        boolean isLocationChanged = newSettings.getLocationEnabled() != oldSettings.getLocationEnabled();
        boolean isNameChanged = !newSettings.getName().equals(oldSettings.getName());
        boolean isUserLimitChanged = !newSettings.getUserLimit().equals(oldSettings.getUserLimit())
                && newSettings.getUserLimit() != 0;
        boolean isSongLimitChanged = !newSettings.getSongLimit().equals(oldSettings.getSongLimit());

        // Set the parameters from newSettings, or set to null if they haven't changed
        params.put(NAME_KEY, isNameChanged ? newSettings.getName() : null);
        params.put(LOCATION_PERMISSION_KEY, isLocationChanged ? newSettings.getLocationEnabled() : null);
        params.put(USER_LIMIT_KEY, isUserLimitChanged ? newSettings.getUserLimit() : null);
//        params.put(SONG_LIMIT_KEY, isSongLimitChanged ? newSettings.getSongLimit() : null);
        params.put(SONG_LIMIT_KEY, null); // TODO - undo this change once song limit is implemented fully
        return params;
    }
}
