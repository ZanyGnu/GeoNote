package geonote.app.Activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import geonote.app.Fragments.SettingsFragment;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }
}
