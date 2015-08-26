package org.md2k.plotter;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.os.Bundle;

import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.utilities.Report.Log;

import java.util.ArrayList;

public class ActivityPlotter extends PreferenceActivity {
    private static final String TAG = ActivityPlotter.class.getSimpleName();
    ArrayList<DataSourceClient> dataSourceClients;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plotter);
        addPreferencesFromResource(R.xml.pref_datasource);
    }

    void getDataSources(final String platformType) {
        final DataKitApi dataKitApi = new DataKitApi(ActivityPlotter.this);
        dataSourceClients=null;
        final Platform platform=new PlatformBuilder().setType(platformType).build();
        dataKitApi.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                dataSourceClients = dataKitApi.find(new DataSourceBuilder().setPlatform(platform).build()).await();
                updateDataSource(platformType,dataSourceClients);
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

    void updateDataSource(String platformType, ArrayList<DataSourceClient> dataSourceClients) {
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(platformType);
        Log.d(TAG, "Preference category: " + preferenceCategory);
        preferenceCategory.removeAll();
        for (int i = 0; i < dataSourceClients.size(); i++) {
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
    void runPlot(DataSourceClient dataSourceClient){
        Intent intent = new Intent(ActivityPlotter.this, ActivityPlot.class);
        intent.putExtra(DataSourceClient.class.getSimpleName(),dataSourceClient);
        startActivity(intent);
    }
}
