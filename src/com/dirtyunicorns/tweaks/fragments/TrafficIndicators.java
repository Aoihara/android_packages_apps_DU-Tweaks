/*
 * Copyright (C) 2017-2019 The Dirty Unicorns Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dirtyunicorns.tweaks.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import androidx.preference.*;

import com.android.internal.logging.nano.MetricsProto;

import com.dirtyunicorns.support.preferences.CustomSeekBarPreference;
import com.dirtyunicorns.support.preferences.SystemSettingSwitchPreference;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.search.SearchIndexable;

import android.util.Log;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Locale;

@SearchIndexable
public class TrafficIndicators extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Indexable {

    private static final String NETWORK_TRAFFIC_FONT_SIZE  = "network_traffic_font_size";

    private CustomSeekBarPreference mThreshold;
    private SystemSettingSwitchPreference mNetMonitor;
    private ListPreference mNetTrafficType;
    private CustomSeekBarPreference mNetTrafficSize;
    private SystemSettingSwitchPreference mShowArrows;
    private ListPreference mNetTrafficLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.traffic_indicators);

        final ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();

        mNetTrafficLocation = (ListPreference) findPreference("network_traffic_location");
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, 0, UserHandle.USER_CURRENT);
        mNetTrafficLocation.setOnPreferenceChangeListener(this);

        int value = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_TYPE, 0, UserHandle.USER_CURRENT);
        mNetTrafficType = (ListPreference) findPreference("network_traffic_type");
        mNetTrafficType.setValue(String.valueOf(value));
        mNetTrafficType.setSummary(mNetTrafficType.getEntry());
        mNetTrafficType.setOnPreferenceChangeListener(this);

        mNetTrafficSize = (CustomSeekBarPreference) findPreference(NETWORK_TRAFFIC_FONT_SIZE);
        int NetTrafficSize = Settings.System.getInt(resolver,
                Settings.System.NETWORK_TRAFFIC_FONT_SIZE, 42);
        mNetTrafficSize.setValue(NetTrafficSize / 1);
        mNetTrafficSize.setOnPreferenceChangeListener(this);

        value = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 1, UserHandle.USER_CURRENT);
        mThreshold = (CustomSeekBarPreference) findPreference("network_traffic_autohide_threshold");
        mThreshold.setValue(value);
        mThreshold.setOnPreferenceChangeListener(this);

        mShowArrows = (SystemSettingSwitchPreference) findPreference("network_traffic_arrow");

        int netMonitorEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_STATE, 0, UserHandle.USER_CURRENT);
        if (netMonitorEnabled == 1) {
            mNetTrafficLocation.setValue(String.valueOf(location+1));
            updateTrafficLocation(location+1);
        } else {
            mNetTrafficLocation.setValue("0");
            updateTrafficLocation(0); 
        }
        mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNetTrafficLocation) {
            int location = Integer.valueOf((String) newValue);
            int index = mNetTrafficLocation.findIndexOfValue((String) newValue);
            mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntries()[index]);
            if (location > 0) {
                // Convert the selected location mode from our list {0,1,2} and store it to "view location" setting: 0=sb; 1=expanded sb
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, location-1, UserHandle.USER_CURRENT);
                // And also enable the net monitor
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 1, UserHandle.USER_CURRENT);
                updateTrafficLocation(location+1);
            } else { // Disable net monitor completely
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 0, UserHandle.USER_CURRENT);
                updateTrafficLocation(location);
            }
            return true;
        } else if (preference == mThreshold) {
            int val = (Integer) newValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, val,
                    UserHandle.USER_CURRENT);
            return true;
        }  else if (preference == mNetTrafficSize) {
            int width = ((Integer)newValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_FONT_SIZE, width);
            return true;
        } else if (preference == mNetTrafficType) {
            int val = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_TYPE, val,
                    UserHandle.USER_CURRENT);
            int index = mNetTrafficType.findIndexOfValue((String) newValue);
            mNetTrafficType.setSummary(mNetTrafficType.getEntries()[index]);
            return true;
        }
        return false;
    }

    public void updateTrafficLocation(int location) {
        switch(location){ 
            case 0:
                mThreshold.setEnabled(false);
                mShowArrows.setEnabled(false);
                break;
            case 1:
            case 2:
                mThreshold.setEnabled(true);
                mShowArrows.setEnabled(true);
                break;
            default: 
                break;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIRTYTWEAKS;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.traffic_indicators;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
        }
    };
}
