package com.example.tomassarker.simplefileexplorer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends PreferenceFragment {

    private String currentPath;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsFragment.
     */
    public static SettingsFragment newInstance(String currentPath) {
        SettingsFragment fragment = new SettingsFragment();

        Bundle args = new Bundle();
        args.putString(SettingsActivity.CURRENT_PATH, currentPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        EditTextPreference edit_Pref = (EditTextPreference)
                getPreferenceScreen().findPreference("edit_text_preference_1");
        edit_Pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                File file = new File(newValue.toString());
                if (file.exists() && file.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        if (getArguments() != null) {
            currentPath = getArguments().getString(SettingsActivity.CURRENT_PATH);
        }
    }






}
