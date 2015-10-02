package org.md2k.plotter;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.ActivityAbout;
import org.md2k.utilities.UI.ActivityCopyright;

import java.util.ArrayList;
import java.util.HashSet;

public class ActivityPlotter extends PreferenceActivity {
    private static final String TAG = ActivityPlotter.class.getSimpleName();
    HashSet<String> autoSenseList=new HashSet<>();
    HashSet<String> phoneList=new HashSet<>();
    HashSet<String> microsoftBandList=new HashSet<>();

    ArrayList<DataSourceClient> dataSourceClients;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plotter);
        addPreferencesFromResource(R.xml.pref_datasource);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
        autoSenseList.add(DataSourceType.RESPIRATION);autoSenseList.add(DataSourceType.ECG);
        autoSenseList.add(DataSourceType.ACCELEROMETER_X);autoSenseList.add(DataSourceType.ACCELEROMETER_Y);
        autoSenseList.add(DataSourceType.ACCELEROMETER_Z);
        phoneList.add(DataSourceType.ACCELEROMETER);phoneList.add(DataSourceType.GYROSCOPE);
        microsoftBandList.add(DataSourceType.ACCELEROMETER);microsoftBandList.add(DataSourceType.GYROSCOPE);

    }

    void getDataSources(final String platformType) {
        final DataKitApi dataKitApi = new DataKitApi(ActivityPlotter.this);
        dataSourceClients=null;
        final Platform platform=new PlatformBuilder().setType(platformType).build();
        dataKitApi.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                dataSourceClients = dataKitApi.find(new DataSourceBuilder().setPlatform(platform).build()).await();
                updateDataSource(platformType, dataSourceClients);
                dataKitApi.disconnect();
            }
        });
    }
    @Override
    protected void onResume(){
        getDataSources(PlatformType.AUTOSENSE_CHEST);
        getDataSources(PlatformType.MICROSOFT_BAND);
        getDataSources(PlatformType.PHONE);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    boolean validList(String platformType, String dataSourceType){
        switch (platformType) {
            case PlatformType.AUTOSENSE_CHEST:
                return autoSenseList.contains(dataSourceType);
            case PlatformType.MICROSOFT_BAND:
                return microsoftBandList.contains(dataSourceType);
            case PlatformType.PHONE:
                return phoneList.contains(dataSourceType);
        }
        return false;

    }

    void updateDataSource(String platformType, ArrayList<DataSourceClient> dataSourceClients) {
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(platformType);
        Log.d(TAG, "Preference category: " + preferenceCategory);
        preferenceCategory.removeAll();
        for (int i = 0; i < dataSourceClients.size(); i++) {
            if(!validList(platformType,dataSourceClients.get(i).getDataSource().getType())) continue;
            final DataSourceClient dataSourceClient=dataSourceClients.get(i);
            String dataSourceType = dataSourceClients.get(i).getDataSource().getType();
            String platformId = dataSourceClients.get(i).getDataSource().getPlatform().getId();
            Preference preference = new Preference(this);
            preference.setKey(dataSourceType);
            String title = dataSourceType;
            title = title.replace("_", " ");
            title = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
            preference.setTitle(title);
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    runPlot(dataSourceClient);
                    return false;
                }
            });
            if(platformType.equals(PlatformType.MICROSOFT_BAND))
                preference.setSummary(dataSourceClient.getDataSource().getPlatform().getMetadata().get("location"));
            preferenceCategory.addPreference(preference);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                break;
            case R.id.action_settings:
//                intent = new Intent(this, ActivityAutoSenseSettings.class);
//                startActivity(intent);
                break;
            case R.id.action_about:
                intent = new Intent(this, ActivityAbout.class);
                startActivity(intent);
                break;
            case R.id.action_copyright:
                intent = new Intent(this, ActivityCopyright.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void runPlot(DataSourceClient dataSourceClient){
        Intent intent = new Intent(ActivityPlotter.this, ActivityPlot.class);
        intent.putExtra(DataSourceClient.class.getSimpleName(),dataSourceClient);
        startActivity(intent);
    }
}
