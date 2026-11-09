package org.leeko.intervaltimer;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Settings. The values live in the default shared preferences under the keys of
 * res/xml/pref_general.xml; AppController and TimerActivity read them from there.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        EdgeToEdgeHelper.applySystemBarPadding(this);
        ViewCompat.setAccessibilityHeading(findViewById(R.id.settingsTitle), true);

        findViewById(R.id.settingsBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settingsContainer, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);

            // No icons here, so don't keep room for them on the left.
            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
                getPreferenceScreen().getPreference(i).setIconSpaceReserved(false);
            }

            bindVersionSummary(findPreference("app_version"));

            // Android ignores orientation locks on large screens, so don't offer a
            // switch that would do nothing there.
            if (Utils.isLargeScreen(requireContext())) {
                Preference lockPortrait = findPreference(Utils.PREF_LOCK_PORTRAIT);
                if (lockPortrait != null) {
                    getPreferenceScreen().removePreference(lockPortrait);
                }
            }
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // The settings sit in one rounded card, like the lists on the other screens.
            RecyclerView list = getListView();
            float density = getResources().getDisplayMetrics().density;
            int side = (int) (16 * density);
            list.setBackgroundResource(R.drawable.bg_card);
            list.setClipToOutline(true);
            list.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
            list.setOverScrollMode(View.OVER_SCROLL_NEVER);
            android.view.ViewGroup.LayoutParams lp = list.getLayoutParams();
            if (lp instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams margins = (android.view.ViewGroup.MarginLayoutParams) lp;
                margins.setMargins(side, (int) (4 * density), side, (int) (16 * density));
                margins.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                list.setLayoutParams(margins);
            }
        }

        /** Sets a preference's summary to the app's version name (for example "2.4"). */
        private void bindVersionSummary(Preference preference) {
            if (preference == null) {
                return;
            }
            try {
                PackageInfo info = requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0);
                preference.setSummary(info.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                // Can't happen for our own package.
            }
        }
    }
}
