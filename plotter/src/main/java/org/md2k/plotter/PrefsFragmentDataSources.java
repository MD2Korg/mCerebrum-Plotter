package org.md2k.plotter;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.utilities.Report.Log;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class PrefsFragmentDataSources extends PreferenceFragment {
    private static final String TAG = PrefsFragmentDataSources.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_datasource);
        setBackButton();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        assert v != null;
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);
        return v;
    }

    void getDataSources(final String type, final String id, final String platformType, final String platformId) {
        Log.d(TAG,"getDataSources()...");
        final DataKitApi dataKitApi = new DataKitApi(getActivity());
        final Platform platform = new PlatformBuilder().setType(platformType).setId(platformId).build();
        dataKitApi.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                Log.d(TAG,"connected...");
                ArrayList<DataSourceClient> dataSourceClients = dataKitApi.find(new DataSourceBuilder().setPlatform(platform).setType(type).setId(id)).await();
                Log.d(TAG,"Datasourceclient="+dataSourceClients.size());
                updateDataSource(dataSourceClients);
                dataKitApi.disconnect();
            }
        });
    }

    public void readDataSources() {
        if (DefaultConfiguration.isExist()) {

            readDefaultSettings();
        }
        else readAllDataSources();
    }

    public void readAllDataSources() {
        getDataSources(null, null, null, null);
    }

    void readDefaultSettings() {
        ArrayList<DataSource> defaultArrayList = DefaultConfiguration.read();
        assert defaultArrayList != null;
        for (int i = 0; i < defaultArrayList.size(); i++) {
            getDataSources(defaultArrayList.get(i).getType(), defaultArrayList.get(i).getId(), defaultArrayList.get(i).getPlatform().getType(), defaultArrayList.get(i).getPlatform().getId());
        }
    }

    @Override
    public void onResume() {
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("datasource");
        preferenceCategory.removeAll();
        readDataSources();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private String prepareName(String type, String id, String platformType, String platformId) {
        String str="";
        boolean flag=false;
        if(platformType!=null){
            str+=platformType;
            flag=true;
        }
        if(platformId!=null){
            if(flag) str+=":";
            str+=platformId;
            flag=true;
        }
        if(type!=null){
            if(flag) str+=":";
            str+=type;
            flag=true;
        }
        if(id!=null){
            if(flag) str+=":";
            str+=id;
        }
        return str;

    }

    void updateDataSource(ArrayList<DataSourceClient> dataSourceClients) {
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("datasource");
        Log.d(TAG,"dataSourceClient="+dataSourceClients.size());
        for (int i = 0; i < dataSourceClients.size(); i++) {
            final DataSourceClient dataSourceClient = dataSourceClients.get(i);
            String type = dataSourceClients.get(i).getDataSource().getType();
            String id = dataSourceClients.get(i).getDataSource().getId();
            String platformType = dataSourceClients.get(i).getDataSource().getPlatform().getType();
            String platformId = dataSourceClients.get(i).getDataSource().getPlatform().getId();

            Preference preference = new Preference(getActivity());
//            preference.setKey(dataSourceType);
            String title = prepareName(type, id, platformType, platformId);
            Log.d(TAG,"title="+title);
//            title = title.replace("_", " ");
//            title = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
            preference.setTitle(title);
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    runPlot(dataSourceClient);
                    return false;
                }
            });
            preferenceCategory.addPreference(preference);
        }
    }

    void runPlot(DataSourceClient dataSourceClient) {
        Intent intent = new Intent(getActivity(), ActivityPlot.class);
        intent.putExtra(DataSourceClient.class.getSimpleName(), dataSourceClient);
        startActivity(intent);
    }


    private void setBackButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText("Close");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }
}
