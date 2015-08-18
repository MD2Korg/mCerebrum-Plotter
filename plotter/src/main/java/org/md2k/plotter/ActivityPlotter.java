package org.md2k.plotter;

import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.utilities.Report.Log;

import java.util.ArrayList;

public class ActivityPlotter extends PreferenceActivity {

    private static final String TAG = ActivityPlotter.class.getSimpleName();
    DataKitApi dataKitApi = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plotter);
        addPreferencesFromResource(R.xml.pref_datasource);
        setPlotButton();
        setQuitButton();
        connectDataKit();
    }

    void connectDataKit() {
        dataKitApi = new DataKitApi(ActivityPlotter.this);
        dataKitApi.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                ArrayList<DataSourceClient> dataSourceClients = dataKitApi.find(new DataSourceBuilder().build()).await();
                updateDataSource(dataSourceClients);
            }
        });
    }

    @Override
    protected void onDestroy() {
        dataKitApi.disconnect();
        super.onDestroy();
    }

    void updateDataSource(ArrayList<DataSourceClient> dataSourceClients) {
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("dataSource");
        Log.d(TAG, "Preference category: " + preferenceCategory);
        preferenceCategory.removeAll();
        for (int i = 0; i < dataSourceClients.size(); i++) {
            String dataSourceType = dataSourceClients.get(i).getDataSource().getType();
            String platformType = dataSourceClients.get(i).getDataSource().getPlatform().getType();
            String platformId = dataSourceClients.get(i).getDataSource().getPlatform().getId();
            CheckBoxPreference checkBoxPreference = new CheckBoxPreference(this);
            checkBoxPreference.setKey(dataSourceType);
            String title = dataSourceType;
            title = title.replace("_", " ");
            title = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
            checkBoxPreference.setTitle(title);
            checkBoxPreference.setSummary(platformType + ":" + platformId);
            preferenceCategory.addPreference(checkBoxPreference);
        }

    }

    private void setPlotButton() {
        final Button button = (Button) findViewById(R.id.button_plot);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ActivityPlotter.this, MainActivity.class);
                startActivity(intent);

            }
        });
    }

    private void setQuitButton() {
        final Button button = (Button) findViewById(R.id.button_quit);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }
}
