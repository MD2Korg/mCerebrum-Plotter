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
import org.md2k.datakitapi.datatype.DataTypeFloatArray;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;

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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_plot);
        dataSourceClient = (DataSourceClient) getIntent().getSerializableExtra(DataSourceClient.class.getSimpleName());
        preparePlot();
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
    public void onResume() {
        dataKitApi = new DataKitApi(ActivityPlot.this);
        dataKitApi.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                boolean returned = dataKitApi.subscribe(dataSourceClient, new OnReceiveListener() {
                    @Override
                    public void onReceived(DataType dataType) {
                        plotFloatArray(((DataTypeFloatArray) dataType).getSample());
                    }
                });
            }
        });
        super.onResume();
        redrawer.start();
    }

    @Override
    public void onPause() {
        dataKitApi.unsubscribe(dataSourceClient).await();
        dataKitApi.disconnect();
        redrawer.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        redrawer.finish();
        super.onDestroy();
    }

    void preparePlotSensors(String[] name, int[] ranges) {
        int[] colors = {Color.rgb(100, 100, 200), Color.rgb(100, 200, 100), Color.rgb(200, 100, 100), Color.rgb(100, 200, 200), Color.rgb(200, 100, 200), Color.rgb(200, 200, 100)};
        aprHistoryPlot.setRangeBoundaries(-1,1, BoundaryMode.AUTO);
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
                preparePlotSensors(new String[]{"X", "Y", "Z"}, new int[]{-10, 10});
                break;
            case DataSourceType.GYROSCOPE:
                preparePlotSensors(new String[]{"X", "Y", "Z"}, new int[]{-10, 10});
                break;
            case DataSourceType.CPU:
                preparePlotSensors(new String[]{"CPU Usage"}, new int[]{0, 5});
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
