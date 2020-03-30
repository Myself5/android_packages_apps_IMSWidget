package org.carbonrom.IMSWidget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.view.View;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "IMSWidget.MainActivity";
    public static final String[] requiredPermissions =
            new String[] {Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PRIVILEGED_PHONE_STATE};
    private static final int SIM1_SUBID = 2;
    private static final int SIM2_SUBID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.ims_preferences_settings, new IMSSettingsFragment())
            .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tv = (TextView) findViewById(R.id.ims_status);

        if(checkPermission(requiredPermissions)) {
            Log.e(TAG, "Setting IMS Status");
            tv.setText(getIMSStatusString(this));
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tv.setText(getIMSStatusString(MainActivity.this));
                }
            });
        }

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(
            new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                tv.setText(getIMSStatusString(MainActivity.this));
                Intent intent = new Intent(MainActivity.this, IMSWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                 int[] appWidgetIds = AppWidgetManager.getInstance(MainActivity.this)
                        .getAppWidgetIds(new ComponentName(MainActivity.this,
                        IMSWidgetProvider.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                MainActivity.this.sendBroadcast(intent);
            }
        });
    }

    public static class IMSSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.ims_preference_screen, rootKey);

            SwitchPreferenceCompat sim1 = getPreferenceManager()
                    .findPreference("sim1_status");
            SwitchPreferenceCompat sim2 = getPreferenceManager()
                    .findPreference("sim2_status");

            TelephonyManager tm = (TelephonyManager)
                    getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            tm = tm.createForSubscriptionId(SIM1_SUBID);
            String sim1_operator = tm.getSimOperatorName();
            tm = tm.createForSubscriptionId(SIM2_SUBID);
            String sim2_operator = tm.getSimOperatorName();

            sim1.setTitle(getActivity().getResources()
                    .getString(R.string.sim_1, sim1_operator));
            sim2.setTitle(getActivity().getResources()
                    .getString(R.string.sim_2, sim2_operator));
        }
    }

    protected boolean checkPermission(String[] permissions) {
        boolean hasPermissions = true;
        for (String perm : permissions) {
            if (!(checkSelfPermission(perm)
                    == PackageManager.PERMISSION_GRANTED)) {
                hasPermissions = false;
                break;
            }
        }

        if (!hasPermissions) {
            Log.e(TAG, "requesting Permissions");
            requestPermissions(permissions, 0);
        } else {
            Log.e(TAG, "not requesting Permissions; everything granted");
        }
        return hasPermissions;
    }

    static boolean[] getIMSStatusForSubId(Context context, int subId) {
        boolean[] simStatus = new boolean[4];
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        tm = tm.createForSubscriptionId(subId);

        simStatus[0] = tm.isImsRegistered(subId);
        simStatus[1] = tm.isVolteAvailable();
        simStatus[2] = tm.isWifiCallingAvailable();
        simStatus[3] = tm.isVideoTelephonyAvailable();

        return simStatus;
    }

    static String getIMSStatusString(Context context) {
        final String registered = context.getResources()
                .getString(R.string.radio_info_ims_reg_status_registered);
        final String unregistered = context.getResources()
                .getString(R.string.radio_info_ims_reg_status_not_registered);

        final String available = context.getResources()
                .getString(R.string.radio_info_ims_feature_status_available);
        final String unavailable = context.getResources().getString(
                R.string.radio_info_ims_feature_status_unavailable);

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(context);
        boolean showSIM1 = prefs.getBoolean("sim1_status",true);
        boolean showSIM2 = prefs.getBoolean("sim2_status",true);

        String imsStatus = "";

        if (showSIM1 == showSIM2) {
            boolean[] simStatus_1 = getIMSStatusForSubId(context, SIM1_SUBID);
            boolean[] simStatus_2 = getIMSStatusForSubId(context, SIM2_SUBID);

            imsStatus = context.getResources().getString(R.string.radio_info_ims_reg_status_dual,
                    simStatus_1[0] ? registered : unregistered, simStatus_2[0] ? registered : unregistered,
                    simStatus_1[1] ? available : unavailable, simStatus_2[1] ? available : unavailable,
                    simStatus_1[2] ? available : unavailable, simStatus_2[2] ? available : unavailable,
                    simStatus_1[3] ? available : unavailable, simStatus_2[3] ? available : unavailable);
        } else {
            boolean[] activeSimStatus = getIMSStatusForSubId(context, showSIM1 ? SIM1_SUBID : SIM2_SUBID);
            imsStatus = context.getResources().getString(R.string.radio_info_ims_reg_status_single,
                    activeSimStatus[0] ? registered : unregistered,
                    activeSimStatus[1] ? available : unavailable,
                    activeSimStatus[2] ? available : unavailable,
                    activeSimStatus[3] ? available : unavailable);
        }
        return imsStatus;
    }
}
