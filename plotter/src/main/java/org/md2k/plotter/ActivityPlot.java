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

import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeFloat;
import org.md2k.datakitapi.datatype.DataTypeFloatArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.utilities.Report.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class ActivityPlot extends Activity {

    private static final int HISTORY_SIZE = 300;            // number of points to plot in history
    private static final String TAG = ActivityPlot.class.getSimpleName();

    private XYPlot aprHistoryPlot = null;
    ArrayList<SimpleXYSeries> historySeries;
    private Redrawer redrawer;
    DataSourceClient dataSourceClient;
    DataKitApi dataKitApi;

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
        dataSourceClient = (DataSourceClient) getIntent().getSerializableExtra(DataSourceClient.class.getSimpleName());
        preparePlot();
        dataKitApi = new DataKitApi(ActivityPlot.this);
        dataKitApi.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                boolean returned = dataKitApi.subscribe(dataSourceClient, new OnReceiveListener() {
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
                        }
                        plotFloatArray(v);
                    }
                });
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
        if (dataKitApi != null) {
            dataKitApi.unsubscribe(dataSourceClient).await();
            dataKitApi.disconnect();
            dataKitApi = null;
        }
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

    void preparePlot() {
        // setup the APR History plot:
        aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);
        aprHistoryPlot.setTitle(dataSourceClient.getDataSource().getType());
        historySeries = new ArrayList<>();
        switch (dataSourceClient.getDataSource().getType()) {
            case DataSourceType.ACCELEROMETER:
                preparePlotSensors(new String[]{"X", "Y", "Z"}, new int[]{-20, 20});
                break;
            case DataSourceType.GYROSCOPE:
                preparePlotSensors(new String[]{"X", "Y", "Z"}, new int[]{-400, 400});
                break;
            case DataSourceType.CPU:
                preparePlotSensors(new String[]{"CPU Usage"}, new int[]{0, 5});
                break;
            case DataSourceType.RESPIRATION:
                preparePlotSensors(new String[]{"Respiration"}, new int[]{-2000, 5000});
                break;
            case DataSourceType.ECG:
                preparePlotSensors(new String[]{"ECG"}, new int[]{0, 5000});
                break;
            case DataSourceType.ACCELEROMETER_X:
                preparePlotSensors(new String[]{"Accelerometer X"}, new int[]{1000, 3000});
                break;
            case DataSourceType.ACCELEROMETER_Y:
                preparePlotSensors(new String[]{"Accelerometer Y"}, new int[]{1000, 3000});
                break;
            case DataSourceType.ACCELEROMETER_Z:
                preparePlotSensors(new String[]{"Accelerometer Z"}, new int[]{1000, 3000});
                break;

        }

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
