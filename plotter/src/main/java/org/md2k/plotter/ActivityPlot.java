package org.md2k.plotter;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDouble;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeFloat;
import org.md2k.datakitapi.datatype.DataTypeFloatArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.messagehandler.ResultCallback;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.utilities.permission.PermissionInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
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


public class ActivityPlot extends Activity {

    private static final int HISTORY_SIZE = 300;            // number of points to plot in history
    private static final String TAG = ActivityPlot.class.getSimpleName();
    private ArrayList<SimpleXYSeries> historySeries;
    private DataSourceClient dataSourceClient;
    private XYPlot aprHistoryPlot = null;
    private Redrawer redrawer;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.getPermissions(this, new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                if (!result) {
                    Toast.makeText(getApplicationContext(), "!PERMISSION DENIED !!! Could not continue...", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    load();

                }
            }
        });
    }

    void load() {
        try {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.activity_plot);
            dataSourceClient = getIntent().getParcelableExtra(DataSourceClient.class.getSimpleName());
            preparePlot();
            DataKitAPI.getInstance(ActivityPlot.this).connect(new OnConnectionListener() {
                @Override
                public void onConnected() {
                    try {
                        start();
                    } catch (DataKitException e) {
                        finish();
                    }
                }
            });
        } catch (Exception e) {
            finish();
        }

    }

    @Override
    public void onBackPressed() {
        close();
        super.onBackPressed();
    }

    void start() throws DataKitException {
        List<DataType> dtList = DataKitAPI.getInstance(ActivityPlot.this).query(dataSourceClient, HISTORY_SIZE);
        for (DataType dataType : dtList) {
            float v[] = null;
            if (dataType instanceof DataTypeInt) {
                int value = ((DataTypeInt) dataType).getSample();
                v = new float[1];
                v[0] = value;
            } else if (dataType instanceof DataTypeFloat) {
                float value = ((DataTypeFloat) dataType).getSample();
                v = new float[1];
                v[0] = value;
            } else if (dataType instanceof DataTypeFloatArray) {
                v = ((DataTypeFloatArray) dataType).getSample();
            } else if (dataType instanceof DataTypeIntArray) {
                int value[] = ((DataTypeIntArray) dataType).getSample();
                v = new float[value.length];
                for (int i = 0; i < value.length; i++)
                    v[i] = value[i];
            } else if (dataType instanceof DataTypeDoubleArray) {
                double value[] = ((DataTypeDoubleArray) dataType).getSample();
                v = new float[value.length];
                for (int i = 0; i < value.length; i++)
                    v[i] = (float) value[i];
            } else if (dataType instanceof DataTypeDouble) {
                double value = ((DataTypeDouble) dataType).getSample();
                v = new float[1];
                v[0] = (float) value;
            }
            if (v != null)
                plotFloatArray(v);
        }
        DataKitAPI.getInstance(ActivityPlot.this).subscribe(dataSourceClient, new OnReceiveListener() {
            @Override
            public void onReceived(DataType dataType) {
                float v[] = null;
                if (dataType instanceof DataTypeInt) {
                    int value = ((DataTypeInt) dataType).getSample();
                    v = new float[1];
                    v[0] = value;
                } else if (dataType instanceof DataTypeFloat) {
                    float value = ((DataTypeFloat) dataType).getSample();
                    v = new float[1];
                    v[0] = value;
                } else if (dataType instanceof DataTypeFloatArray) {
                    v = ((DataTypeFloatArray) dataType).getSample();
                } else if (dataType instanceof DataTypeIntArray) {
                    int value[] = ((DataTypeIntArray) dataType).getSample();
                    v = new float[value.length];
                    for (int i = 0; i < value.length; i++)
                        v[i] = value[i];
                } else if (dataType instanceof DataTypeDoubleArray) {
                    double value[] = ((DataTypeDoubleArray) dataType).getSample();
                    v = new float[value.length];
                    for (int i = 0; i < value.length; i++)
                        v[i] = (float) value[i];
                } else if (dataType instanceof DataTypeDouble) {
                    double value = ((DataTypeDouble) dataType).getSample();
                    v = new float[1];
                    v[0] = (float) value;
                }
                if (v != null)
                    plotFloatArray(v);
            }
        });

        redrawer.start();


    }

    private void plotFloatArray(float[] samples) {
        if (historySeries.get(0).size() > HISTORY_SIZE) {
            for (int i = 0; i < historySeries.size(); i++) {
                historySeries.get(i).removeFirst();
            }
        }
        for (int i = 0; i < historySeries.size(); i++)
            try {
                historySeries.get(i).addLast(null, samples[i]);
            } catch (ArrayIndexOutOfBoundsException e) {
            }
    }

    private void close() {
        DataKitAPI dataKitAPI = DataKitAPI.getInstance(ActivityPlot.this);
        if (dataSourceClient != null && dataKitAPI != null)
            try {
                dataKitAPI.unsubscribe(dataSourceClient);
            } catch (DataKitException e) {
            }
        if (dataKitAPI != null)
            dataKitAPI.disconnect();
        if (redrawer != null) {
            redrawer.pause();
            redrawer.finish();
        }
    }


    @Override
    public void onDestroy() {
        close();
        super.onDestroy();
    }

    private void preparePlotSensors(String[] name, int[] ranges) {
        int[] colors = {Color.rgb(0, 255, 0), Color.rgb(255, 0, 0), Color.rgb(0, 0, 255), Color.rgb(0, 255, 255), Color.rgb(255, 0, 255), Color.rgb(255, 255, 0)};
        aprHistoryPlot.setRangeBoundaries(ranges[0], ranges[1], BoundaryMode.FIXED);
        for (int i = 0; i < name.length; i++) {
            SimpleXYSeries xySeries = new SimpleXYSeries(name[i]);
            xySeries.useImplicitXVals();
            historySeries.add(xySeries);
            aprHistoryPlot.addSeries(xySeries,
                    new LineAndPointFormatter(colors[i % colors.length], null, null, null));
        }

    }

    private String[] getName(DataSource dataSource) {
        ArrayList<HashMap<String, String>> dataDescriptors = dataSource.getDataDescriptors();
        String name[];
        if (dataDescriptors == null || dataDescriptors.size() == 0) {
            name = new String[1];
            if (dataSource.getMetadata() != null && dataSource.getMetadata().containsKey(METADATA.NAME))
                name[0] = dataSource.getMetadata().get(METADATA.NAME);
            else name[0] = dataSource.getType();
        } else {
            name = new String[dataDescriptors.size()];
            for (int i = 0; i < dataDescriptors.size(); i++) {
                if (dataDescriptors.get(i).get(METADATA.NAME) == null)
                    name[i] = "";
                else name[i] = dataDescriptors.get(i).get(METADATA.NAME);
            }
        }
        return name;
    }

    private int getMinValue(ArrayList<HashMap<String, String>> dataDescriptors) {
        int minValue = Integer.MAX_VALUE;
        try {
            for (int i = 0; dataDescriptors != null && i < dataDescriptors.size(); i++) {
                if (dataDescriptors.get(i).get(METADATA.MIN_VALUE) == null) continue;
                if (Integer.valueOf(dataDescriptors.get(i).get(METADATA.MIN_VALUE)) < minValue)
                    minValue = Integer.parseInt(dataDescriptors.get(i).get(METADATA.MIN_VALUE));
            }
        } catch (Exception e) {
            minValue = Integer.MAX_VALUE;
        }
        return minValue;
    }

    private int getMaxValue(ArrayList<HashMap<String, String>> dataDescriptors) {
        int maxValue = Integer.MIN_VALUE;
        try {
            for (int i = 0; dataDescriptors != null && i < dataDescriptors.size(); i++) {
                if (dataDescriptors.get(i).get(METADATA.MAX_VALUE) == null) continue;
                if (Integer.valueOf(dataDescriptors.get(i).get(METADATA.MAX_VALUE)) > maxValue)
                    maxValue = Integer.parseInt(dataDescriptors.get(i).get(METADATA.MAX_VALUE));
            }
        } catch (Exception e) {
            maxValue = Integer.MIN_VALUE;
        }
        return maxValue;
    }

    private void preparePlot() {
        // setup the APR History plot:
        aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);
        aprHistoryPlot.setTitle(dataSourceClient.getDataSource().getType());
        historySeries = new ArrayList<>();
        String names[] = getName(dataSourceClient.getDataSource());
        int minValue = getMinValue(dataSourceClient.getDataSource().getDataDescriptors());
        int maxValue = getMaxValue(dataSourceClient.getDataSource().getDataDescriptors());
        if (minValue == Integer.MAX_VALUE || maxValue == Integer.MIN_VALUE || minValue == maxValue)
            preparePlotSensors(new String[]{""}, new int[]{0, 1});
        preparePlotSensors(names, new int[]{minValue, maxValue});

        aprHistoryPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        aprHistoryPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        aprHistoryPlot.setDomainStepValue(HISTORY_SIZE / 10);
        aprHistoryPlot.setTicksPerRangeLabel(3);
        aprHistoryPlot.setDomainLabel("Time");
        aprHistoryPlot.getDomainLabelWidget().pack();
        aprHistoryPlot.setRangeLabel("Samples");
        aprHistoryPlot.getRangeLabelWidget().pack();

        aprHistoryPlot.setRangeValueFormat(new DecimalFormat("#"));
        aprHistoryPlot.setDomainValueFormat(new DecimalFormat("#"));


        redrawer = new Redrawer(
                Arrays.asList(new Plot[]{aprHistoryPlot}),
                100, false);
    }
}
