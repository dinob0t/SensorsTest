package com.sensors.test;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.content.Context;
import android.widget.EditText;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import java.text.DecimalFormat;
import com.androidplot.xy.*;
import android.graphics.Color;
import java.util.Arrays;



public class SensorsTest extends Activity implements SensorEventListener {

    private static final int INTEGRATION_SAMPLES = 256; //how many samples
    private static final int SAMPLE_INTERVAL = 5000; //sample interval in microseconds
    private static final int SIGNAL_INTEREST = 50; //50 Hz signal is of interest


	int samples = -1;
    

    private SensorManager sm;
    //private Sensor acc;
    private Sensor mag;

    private EditText maglower50hz, magupper50hz;
    private EditText magX, magY, magZ;
    private XYPlot aprTotalFieldPlot = null;
    private XYPlot aprFFTPlot = null;
    
    private SimpleXYSeries aprtotalFieldSeries = null;
    private SimpleXYSeries aprFFTSeries = null;
    //private XYPlot aprHistoryPlot = null;
    
    //private EditText locAzimuth, locPitch, locRoll;
    //private EditText locAzimuthD, locPitchD, locRollD;

    //float[] mAcc; // data for accelerometer
    float[] mMag; // data for magnetometer
    //float[] mR = new float[16];
   // float[] mI = new float[16];
//    float[] mLoc = new float[3];
    double[] sampleArray = new double[INTEGRATION_SAMPLES];
    double[] realfftArray = new double[INTEGRATION_SAMPLES];
    double[] imagfftArray = new double[INTEGRATION_SAMPLES];
    Number[] yseries1 = new Number[INTEGRATION_SAMPLES];
    //Number[] xseries1 = new Number[INTEGRATION_SAMPLES];
    Number[] yseries2 = new Number[INTEGRATION_SAMPLES];
    Number maglower;
    Number magupper;
    
    int indexlower;
    int indexupper;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // gui components
        maglower50hz = (EditText) findViewById(R.id.maglower50hz);
        magupper50hz = (EditText) findViewById(R.id.magupper50hz);
        //accZ = (EditText) findViewById(R.id.accZ);
        magX = (EditText) findViewById(R.id.magX);
        magY = (EditText) findViewById(R.id.magY);
        magZ = (EditText) findViewById(R.id.magZ);
        //locAzimuth = (EditText) findViewById(R.id.locAzimuth);
        //locPitch = (EditText) findViewById(R.id.locPitch);
        //locRoll = (EditText) findViewById(R.id.locRoll);
        //locAzimuthD = (EditText) findViewById(R.id.locAzimuthD);
        //locPitchD = (EditText) findViewById(R.id.locPitchD);
        //locRollD = (EditText) findViewById(R.id.locRollD);

        //sensors
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        aprTotalFieldPlot = (XYPlot) findViewById(R.id.aprTotalFieldPlot);
        aprtotalFieldSeries = new SimpleXYSeries("Total Field");
        aprtotalFieldSeries.useImplicitXVals();
        
        //aprTotalFieldPlot.addSeries(aprtotalFieldSeries, new LineAndPointFormatter(Color.rgb(100, 100, 200), Color.BLACK, null));
        aprTotalFieldPlot.addSeries(aprtotalFieldSeries, new LineAndPointFormatter(
                Color.rgb(0, 100, 0),                   // line color
                Color.rgb(0, 100, 0),                   // point color
                Color.rgb(100, 200, 0),null));			//fill color
        
        aprTotalFieldPlot.setDomainStepValue(3);
        aprTotalFieldPlot.setTicksPerRangeLabel(3);
        aprTotalFieldPlot.setRangeBoundaries(30, 100, BoundaryMode.FIXED);
        aprTotalFieldPlot.setDomainBoundaries(0, INTEGRATION_SAMPLES, BoundaryMode.FIXED);
        // use our custom domain value formatter:
        //aprTotalFieldPlot.setDomainValueFormat(new APRIndexFormat());

        // update our domain and range axis labels:
        aprTotalFieldPlot.setDomainLabel("Time");
        aprTotalFieldPlot.getDomainLabelWidget().pack();
        aprTotalFieldPlot.setRangeLabel("Micro Tesla");
        aprTotalFieldPlot.getRangeLabelWidget().pack();
        aprTotalFieldPlot.setGridPadding(15, 0, 15, 0);
        
        //for (int i = 0; i < INTEGRATION_SAMPLES; i++) {
        	//xseries1[i] = i*SAMPLE_INTERVAL/1000000	;
    	  //}
        aprFFTPlot = (XYPlot) findViewById(R.id.aprFFTPlot);
        aprFFTSeries = new SimpleXYSeries("Total Field");
        aprFFTSeries.useImplicitXVals();
        
        //aprTotalFieldPlot.addSeries(aprtotalFieldSeries, new LineAndPointFormatter(Color.rgb(100, 100, 200), Color.BLACK, null));
        aprFFTPlot.addSeries(aprFFTSeries, new LineAndPointFormatter(
                Color.rgb(0, 100, 0),                   // line color
                Color.rgb(0, 100, 0),                   // point color
                Color.rgb(100, 200, 0),null));			//fill color
        
        aprFFTPlot.setDomainStepValue(3);
        aprFFTPlot.setTicksPerRangeLabel(3);
        aprFFTPlot.setRangeBoundaries(-40, 0, BoundaryMode.FIXED);
        aprFFTPlot.setDomainBoundaries(0, 1+INTEGRATION_SAMPLES/2, BoundaryMode.FIXED);
        // use our custom domain value formatter:
        //aprTotalFieldPlot.setDomainValueFormat(new APRIndexFormat());

        // update our domain and range axis labels:
        aprFFTPlot.setDomainLabel("Frequency");
        aprFFTPlot.getDomainLabelWidget().pack();
        aprFFTPlot.setRangeLabel("Power");
        aprFFTPlot.getRangeLabelWidget().pack();
        aprFFTPlot.setGridPadding(15, 0, 15, 0);
    }

    @Override
    public void onStart() {
        super.onStart();
        // TODO: make delay configurable
        //sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI);
        //sm.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, mag, SAMPLE_INTERVAL);
    }

    @Override
    public void onStop() {
        super.onStop();
        sm.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }
    
    
    public void onSensorChanged(SensorEvent event) {

        //if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        //    mAcc = event.values.clone();

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mMag = event.values.clone();

        //if (mAcc != null && mMag != null) {
        if (mMag != null) {
            //boolean success = SensorManager.getRotationMatrix(mR, mI, mAcc, mMag);
            //if (success) {
                samples++;
                if ( samples < INTEGRATION_SAMPLES ) {
                	sampleArray[samples] = Math.sqrt((Double) (Math.pow(mMag[0], 2.0) + Math.pow(mMag[1], 2.0) + Math.pow(mMag[2], 2.0)));
                    return;
                }
                samples = -1;
                FFT calcfft = new FFT(INTEGRATION_SAMPLES);
                
                
                for (int i = 0; i < INTEGRATION_SAMPLES; i++) {
                	yseries1[i] = (Number) sampleArray[i];
            	  }
 

                
                realfftArray = sampleArray;                               
                imagfftArray = new double[INTEGRATION_SAMPLES];
                calcfft.fft(realfftArray,imagfftArray);
                
                for (int i = 0; i < 1+INTEGRATION_SAMPLES/2; i++) {
                	double tmp = 0.000001*SAMPLE_INTERVAL*Math.pow(Math.abs(realfftArray[i]),2.0)/INTEGRATION_SAMPLES;
                	tmp = Math.log10(2.0*tmp);
                	tmp = 10*tmp;
                	yseries2[i] = (Number) tmp;
            	  }
                
                //array element for 50Hz signal
                indexlower = (int) Math.floor(SIGNAL_INTEREST/(INTEGRATION_SAMPLES*SAMPLE_INTERVAL*0.000001));
                indexupper = (int) Math.ceil(SIGNAL_INTEREST/(INTEGRATION_SAMPLES*SAMPLE_INTERVAL*0.000001));
                
                maglower = yseries2[indexlower];
                magupper= yseries2[indexlower];
                
                //SensorManager.getOrientation(mR, mLoc);

                
                
                //set text in view
                DecimalFormat df = new DecimalFormat("###.####");
                maglower50hz.setText(df.format(maglower));
                magupper50hz.setText(df.format(magupper));
               // accZ.setText(df.format(mAcc[2]));
                magX.setText(df.format(mMag[0]));
                magY.setText(df.format(mMag[1]));
                magZ.setText(df.format(yseries2[2]));
               // locAzimuth.setText(df.format(mLoc[0]));
               // locPitch.setText(df.format(mLoc[1]));
               // locRoll.setText(df.format(mLoc[2]));
               //locAzimuthD.setText(df.format(Math.toDegrees(mLoc[0])));
               //locPitchD.setText(df.format(Math.toDegrees(mLoc[1])));
               // locRollD.setText(df.format(Math.toDegrees(mLoc[2])));
            //}
                aprtotalFieldSeries.setModel(Arrays.asList(yseries1), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
                aprTotalFieldPlot.redraw();
                
                aprFFTSeries.setModel(Arrays.asList(yseries2), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
                aprFFTPlot.redraw();
               
        }
    }
}
