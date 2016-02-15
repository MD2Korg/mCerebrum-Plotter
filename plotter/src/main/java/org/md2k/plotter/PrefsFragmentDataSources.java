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
import android.widget.Toast;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnExceptionListener;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.datakitapi.status.Status;
import org.md2k.utilities.Report.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;

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
    DataKitAPI dataKitAPI;
    ArrayList<DataSource> defaultDataSources;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataKitAPI.getInstance(getActivity().getApplicationContext()).close();
        dataKitAPI = DataKitAPI.getInstance(getActivity().getApplicationContext());
        try {
            defaultDataSources = Configuration.readDefault();
        } catch (FileNotFoundException ignored) {
        }
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

    void findDataSource(final String type, final String id, final String platformType, final String platformId) {
        final Platform platform = new PlatformBuilder().setType(platformType).setId(platformId).build();
        ArrayList<DataSourceClient> dataSourceClients = dataKitAPI.find(new DataSourceBuilder().setPlatform(platform).setType(type).setId(id));
        Log.d(TAG,"dataSourceClients="+dataSourceClients.size()+" type="+type+" platformType="+platformType+" platformId="+platformId);
        updateDataSource(dataSourceClients);
    }

    public void findDataSources() {
        if (defaultDataSources != null) {
            for (int i = 0; i < defaultDataSources.size(); i++)
                findDataSource(defaultDataSources.get(i).getType(), defaultDataSources.get(i).getId(), defaultDataSources.get(i).getPlatform().getType(), defaultDataSources.get(i).getPlatform().getId());
        } else
            findDataSource(null, null, null, null);

    }


    @Override
    public void onStart() {
        Log.d(TAG,"onResume()...");
        ((PreferenceCategory) findPreference("autosense")).removeAll();
        ((PreferenceCategory) findPreference("phone")).removeAll();
        ((PreferenceCategory) findPreference("microsoft_band")).removeAll();
        ((PreferenceCategory) findPreference("other")).removeAll();
        if (dataKitAPI.isConnected())
            findDataSources();
        else {
            dataKitAPI.connect(new OnConnectionListener() {
                @Override
                public void onConnected() {
                    Log.d(TAG,"connected...");
                    findDataSources();
                }
            }, new OnExceptionListener() {
                @Override
                public void onException(Status status) {
                    Log.d(TAG, "onException...");
                    Toast.makeText(getActivity(), "Plotter Stopped. DataKit Error: " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                    dataKitAPI.disconnect();
                    dataKitAPI.close();
                }
            });
        }
        super.onStart();
    }

    @Override
    public void onDestroy() {
        dataKitAPI.close();
        super.onDestroy();
    }

    private String getName(DataSource dataSource) {
        String name;
        if (dataSource.getMetadata().containsKey(METADATA.NAME))
            name = dataSource.getMetadata().get(METADATA.NAME);
        else {
            name = dataSource.getType();
            if (dataSource.getId() != null && dataSource.getId().length() != 0)
                name += "(" + dataSource.getId() + ")";
        }
        return name;
    }
    private String getPlatformName(Platform platform) {
        String name;
        if(platform==null) return "Platform - Not Defined";
        if (platform.getMetadata().containsKey(METADATA.NAME))
            name = platform.getMetadata().get(METADATA.NAME);
        else {
            name = platform.getType();
            if (platform.getId() != null && platform.getId().length() != 0)
                name += "(" + platform.getId() + ")";
        }
        return name;
    }

    void updateDataSource(ArrayList<DataSourceClient> dataSourceClients) {
        for (int i = 0; i < dataSourceClients.size(); i++) {
            final DataSourceClient dataSourceClient = dataSourceClients.get(i);

            Preference preference = new Preference(getActivity());
            preference.setTitle(getName(dataSourceClient.getDataSource()));
            preference.setSummary(getPlatformName(dataSourceClient.getDataSource().getPlatform()));
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(dataSourceClient.getDataSource().getDataDescriptors()==null || dataSourceClient.getDataSource().getDataDescriptors().size()==0 ||
                    !dataSourceClient.getDataSource().getDataDescriptors().get(0).containsKey(METADATA.MIN_VALUE))
                        Toast.makeText(getActivity(),"Error: MIN_VALUE & MAX_VALUE are not defined in DataDescriptor...Not possible to plot",Toast.LENGTH_LONG).show();
                    else
                        runPlot(dataSourceClient);
                    return false;
                }
            });
            PreferenceCategory preferenceCategory;
            preferenceCategory = (PreferenceCategory) findPreference("other");
            if(dataSourceClient.getDataSource().getPlatform()==null)
                preferenceCategory = (PreferenceCategory) findPreference("other");
            else if(dataSourceClient.getDataSource().getPlatform().getType()==null)
                preferenceCategory = (PreferenceCategory) findPreference("other");
            else if(PlatformType.AUTOSENSE_CHEST.equals(dataSourceClient.getDataSource().getPlatform().getType()))
                preferenceCategory = (PreferenceCategory) findPreference("autosense");
            else if(PlatformType.AUTOSENSE_WRIST.equals(dataSourceClient.getDataSource().getPlatform().getType()))
                preferenceCategory = (PreferenceCategory) findPreference("autosense");
            else if(PlatformType.PHONE.equals(dataSourceClient.getDataSource().getPlatform().getType()))
                preferenceCategory = (PreferenceCategory) findPreference("phone");
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
    @Override
    public void onStop() {
        Log.d(TAG,"onStop()...");
        dataKitAPI.disconnect();
        super.onStop();
    }
}
