package org.md2k.plotter;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

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
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.utilities.Report.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ActivityPlot extends Activity {

    private static final int HISTORY_SIZE = 300;            // number of points to plot in history
    private static final String TAG = ActivityPlot.class.getSimpleName();

    private XYPlot aprHistoryPlot = null;
    ArrayList<SimpleXYSeries> historySeries;
    private Redrawer redrawer;
    DataSourceClient dataSourceClient;
    DataKitAPI dataKitAPI;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_plot);
        dataSourceClient = (DataSourceClient) getIntent().getParcelableExtra(DataSourceClient.class.getSimpleName());
        preparePlot();
        dataKitAPI = DataKitAPI.getInstance(getApplicationContext());

        dataKitAPI.subscribe(dataSourceClient, new OnReceiveListener() {
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
                }else if (dataType instanceof DataTypeDoubleArray) {
                    double value[] = ((DataTypeDoubleArray) dataType).getSample();
                    v = new float[value.length];
                    for (int i = 0; i < value.length; i++)
                        v[i] = (float) value[i];
                }else if (dataType instanceof DataTypeDouble) {
                    double value =  ((DataTypeDouble) dataType).getSample();
                    v = new float[1];
                    v[0] = (float) value;
                }
                if(v!=null)
                    plotFloatArray(v);
            }
        });

        redrawer.start();

    }

    void plotFloatArray(float[] samples) {
        if (historySeries.get(0).size() > HISTORY_SIZE) {
            for (int i = 0; i < historySeries.size(); i++) {
                historySeries.get(i).removeFirst();
            }
        }
        for (int i = 0; i < historySeries.size(); i++)
            historySeries.get(i).addLast(null, samples[i]);
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        dataKitAPI.unsubscribe(dataSourceClient);
        redrawer.pause();

        redrawer.finish();
        super.onDestroy();
    }

    void preparePlotSensors(String[] name, int[] ranges) {
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

    String[] getName(ArrayList<HashMap<String, String>> dataDescriptors) {
        String name[] = new String[dataDescriptors.size()];
        for (int i = 0; i < dataDescriptors.size(); i++) {
            if (dataDescriptors.get(i).get(METADATA.NAME) == null)
                name[i] = "";
            else name[i] = dataDescriptors.get(i).get(METADATA.NAME);
        }
        return name;
    }

    int getMinValue(ArrayList<HashMap<String, String>> dataDescriptors) {
        int minValue = Integer.MAX_VALUE;
        for (int i = 0; i < dataDescriptors.size(); i++) {
            if (dataDescriptors.get(i).get(METADATA.MIN_VALUE) == null) continue;
            if (Integer.valueOf(dataDescriptors.get(i).get(METADATA.MIN_VALUE)) < minValue)
                minValue = Integer.parseInt(dataDescriptors.get(i).get(METADATA.MIN_VALUE));
        }
        return minValue;
    }

    int getMaxValue(ArrayList<HashMap<String, String>> dataDescriptors) {
        int maxValue = Integer.MIN_VALUE;
        for (int i = 0; i < dataDescriptors.size(); i++) {
            if (dataDescriptors.get(i).get(METADATA.MAX_VALUE) == null) continue;
            if (Integer.valueOf(dataDescriptors.get(i).get(METADATA.MAX_VALUE)) > maxValue)
                maxValue = Integer.parseInt(dataDescriptors.get(i).get(METADATA.MAX_VALUE));
        }
        return maxValue;
    }

    void preparePlot() {
        // setup the APR History plot:
        aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);
        aprHistoryPlot.setTitle(dataSourceClient.getDataSource().getType());
        historySeries = new ArrayList<>();
        String names[] = getName(dataSourceClient.getDataSource().getDataDescriptors());
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
