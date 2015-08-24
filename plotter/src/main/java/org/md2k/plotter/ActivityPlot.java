package org.md2k.plotter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.androidplot.Plot;
import com.androidplot.util.PlotStatistics;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import org.md2k.datakitapi.DataKitApi;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeFloatArray;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.messagehandler.OnReceiveListener;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.utilities.Report.Log;

import java.text.DecimalFormat;
import java.util.Arrays;

public class ActivityPlot extends Activity
{

    private static final int HISTORY_SIZE = 300;            // number of points to plot in history
    private static final String TAG = ActivityPlot.class.getSimpleName();

    private XYPlot aprHistoryPlot = null;

    //private SimpleXYSeries aprLevelsSeries = null;
    private SimpleXYSeries aLvlSeries;
    private SimpleXYSeries pLvlSeries;
    private SimpleXYSeries rLvlSeries;
    SimpleXYSeries rollHistorySeries;
    SimpleXYSeries azimuthHistorySeries;
    private Redrawer redrawer;
    DataSourceClient dataSourceClient;
    DataKitApi dataKitApi;
    SimpleXYSeries pitchHistorySeries;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataSourceClient= (DataSourceClient) getIntent().getSerializableExtra(DataSourceClient.class.getSimpleName());
        preparePlot();
    }
    void plotAccelerometer(float [] samples){
        if (rollHistorySeries.size() > HISTORY_SIZE) {
            rollHistorySeries.removeFirst();
            pitchHistorySeries.removeFirst();
            azimuthHistorySeries.removeFirst();
        }

        // add the latest history sample:
        azimuthHistorySeries.addLast(null, samples[0]);
        pitchHistorySeries.addLast(null, samples[1]);
        rollHistorySeries.addLast(null, samples[2]);

    }

    @Override
    public void onResume() {
        dataKitApi=new DataKitApi(ActivityPlot.this);
        dataKitApi.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                boolean returned=dataKitApi.subscribe(dataSourceClient, new OnReceiveListener() {
                    @Override
                    public void onReceived(DataType dataType) {
                        plotAccelerometer(((DataTypeFloatArray)dataType).getSample());
                        Log.d(TAG,"ActivityPlot-> onReceived...");
                    }
                });
                Log.d(TAG,"subscribe="+returned);
            }
        });
        super.onResume();
        redrawer.start();
    }

    @Override
    public void onPause() {
        redrawer.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        redrawer.finish();
        super.onDestroy();
    }
    void preparePlot(){
        aLvlSeries = new SimpleXYSeries("A");
        pLvlSeries = new SimpleXYSeries("P");
        rLvlSeries = new SimpleXYSeries("R");


        // setup the APR History plot:
        aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);
        aprHistoryPlot.setTitle(dataSourceClient.getDataSource().getType());

        azimuthHistorySeries = new SimpleXYSeries("Az.");
        azimuthHistorySeries.useImplicitXVals();
        pitchHistorySeries = new SimpleXYSeries("Pitch");
        pitchHistorySeries.useImplicitXVals();
        rollHistorySeries = new SimpleXYSeries("Roll");
        rollHistorySeries.useImplicitXVals();

        aprHistoryPlot.setRangeBoundaries(-20, 20, BoundaryMode.FIXED);
        aprHistoryPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        aprHistoryPlot.addSeries(azimuthHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(100, 100, 200), null, null, null));
        aprHistoryPlot.addSeries(pitchHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(100, 200, 100), null, null, null));
        aprHistoryPlot.addSeries(rollHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(200, 100, 100), null, null, null));
        aprHistoryPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        aprHistoryPlot.setDomainStepValue(HISTORY_SIZE/10);
        aprHistoryPlot.setTicksPerRangeLabel(3);
        aprHistoryPlot.setDomainLabel("Sample Index");
        aprHistoryPlot.getDomainLabelWidget().pack();
        aprHistoryPlot.setRangeLabel("Angle (Degs)");
        aprHistoryPlot.getRangeLabelWidget().pack();

        aprHistoryPlot.setRangeValueFormat(new DecimalFormat("#"));
        aprHistoryPlot.setDomainValueFormat(new DecimalFormat("#"));

        // setup checkboxes:
        CheckBox hwAcceleratedCb = (CheckBox) findViewById(R.id.hwAccelerationCb);
        final PlotStatistics levelStats = new PlotStatistics(1000, false);
        final PlotStatistics histStats = new PlotStatistics(1000, false);

        aprHistoryPlot.addListener(histStats);
        hwAcceleratedCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                } else {
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
            }
        });

        CheckBox showFpsCb = (CheckBox) findViewById(R.id.showFpsCb);
        showFpsCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                levelStats.setAnnotatePlotEnabled(b);
                histStats.setAnnotatePlotEnabled(b);
            }
        });


        redrawer = new Redrawer(
                Arrays.asList(new Plot[]{aprHistoryPlot}),
                100, false);

    }


/*    // Called whenever a new orSensor reading is taken.
    @Override
    public synchronized void onSensorChanged(SensorEvent sensorEvent) {
        // get rid the oldest sample in history:
        if (rollHistorySeries.size() > HISTORY_SIZE) {
            rollHistorySeries.removeFirst();
            pitchHistorySeries.removeFirst();
            azimuthHistorySeries.removeFirst();
        }

        // add the latest history sample:
        azimuthHistorySeries.addLast(null, sensorEvent.values[0]);
        pitchHistorySeries.addLast(null, sensorEvent.values[1]);
        rollHistorySeries.addLast(null, sensorEvent.values[2]);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Not interested in this event
    }
    */
}