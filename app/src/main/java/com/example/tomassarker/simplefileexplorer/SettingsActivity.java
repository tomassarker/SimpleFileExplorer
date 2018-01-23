package com.example.tomassarker.simplefileexplorer;

import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    public final static String CURRENT_PATH = "current path";
    private String currentPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentPath = getIntent().getExtras().getString(CURRENT_PATH);

        // Display the fragment as the main content.
        Fragment existingFragment = getFragmentManager().findFragmentById(android.R.id.content);
        //ak uz fragment existuje, nerobime nic, obnovi sa sam
        if (existingFragment == null || !existingFragment.getClass().equals(SettingsFragment.class)) {
            SettingsFragment settingsFragment = SettingsFragment.newInstance(currentPath);
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, settingsFragment)
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
